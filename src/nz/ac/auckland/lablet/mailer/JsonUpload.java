/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class JsonUpload extends HTTPJsonRequest {
    private HTTPMultiPartTransfer multiPartTransfer;

    final private List<Uri> attachments;
    final private GroupMembers groupMembers;

    public JsonUpload(List<Uri> attachments, GroupMembers groupMembers) {
        this.attachments = attachments;
        this.groupMembers = groupMembers;
    }

    public class Progress {
        final public String info;

        public Progress(String info) {
            this.info = info;
        }
    }

    public Observable<Progress> upload(final URL server) {
        return Observable.create(new Observable.OnSubscribeFunc<Progress>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Progress> receiver) {
                try {
                    sendUploadRequest(receiver, server);
                } catch (Exception e) {
                    receiver.onError(e);
                }

                receiver.onCompleted();

                return new Subscription() {
                    @Override
                    public void unsubscribe() {
                        if (multiPartTransfer != null)
                            multiPartTransfer.disconnect();
                    }
                };
            }
        });
    }

    private void sendUploadRequest(final Observer<? super Progress> receiver, URL server) throws IOException,
            JSONException {
        receiver.onNext(new Progress("started"));

        // group members
        List<String> members = new ArrayList<>();
        if (groupMembers != null) {
            for (int i = 0; i < groupMembers.size(); i++) {
                String member = groupMembers.get(i).trim();
                if (member.equals(""))
                    continue;
                members.add(member);
            }
        }

        // attachments
        List<String> fileIds = new ArrayList<>();
        if (attachments != null) {
            for (int i = 0; i < attachments.size(); i++)
                fileIds.add("" + i);
        }

        // start request
        multiPartTransfer = sendOverHTTP(server, "upload", new Argument("files", fileIds));

        // upload attachments
        if (attachments != null) {
            for (int i = 0; i < attachments.size(); i++) {
                final File file = new File(attachments.get(i).getPath());
                receiver.onNext(new Progress(file.getName()));

                OutputStream outputStream = multiPartTransfer.addFile(fileIds.get(i), file.getName());
                InputStream attachmentStream = new FileInputStream(file);
                StreamHelper.copy(attachmentStream, outputStream, new StreamHelper.IProgressListener() {
                    @Override
                    public int getReportingStep() {
                        return 10 * 1024;
                    }

                    @Override
                    public void onNewProgress(int totalProgress) {
                        final int KBYTE = 1024;
                        String update = file.getName();
                        update += " " + totalProgress / KBYTE + "/" + file.length() / KBYTE + " kBytes";
                        receiver.onNext(new Progress(update));
                    }
                });
            }
        }

        receiver.onNext(new Progress("finished"));

        // receive response
        InputStream inputStream = multiPartTransfer.receive();

        ByteArrayOutputStream receivedData = new ByteArrayOutputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        StreamHelper.copy(bufferedInputStream, receivedData);

        // parse response
        JSONObject returnValue = getReturnValue(receivedData.toString());
        if (returnValue == null)
            throw new IOException("bad return value");

        if (returnValue.has("files")) {
            JSONArray fileArray = returnValue.getJSONArray("files");
            for (int i = 0; i < fileArray.length(); i++) {
                String file = fileArray.getString(i);
                System.out.println(file);
            }
        }

        if (returnValue.has("error")) {
            int error = returnValue.getInt("error");
            if (error != 0)
                throw new IOException("error: " + error);
        }
    }
}
