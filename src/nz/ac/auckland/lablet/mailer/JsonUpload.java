/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;

import android.app.Activity;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class JsonUpload extends HTTPJsonRequest {
    private HTTPMultiPartTransfer multiPartTransfer;

    final private Activity activity;
    final private List<Uri> attachments;
    final private List<String> groupMembers;

    public JsonUpload(Activity activity, List<Uri> attachments, List<String> groupMembers) {
        this.activity = activity;
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
                uploadInner(server).subscribeOn(Schedulers.threadPoolForIO())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(receiver);

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

    private Observable<Progress> uploadInner(final URL server) {
        return Observable.create(new Observable.OnSubscribeFunc<Progress>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Progress> receiver) {
                try {
                    sendUploadRequest(receiver, server);
                } catch (Exception e) {
                    receiver.onError(e);
                }

                receiver.onCompleted();

                return new Subscriptions().empty();
            }
        });
    }

    private void sendUploadRequest(final Observer<? super Progress> receiver, URL server) throws IOException,
            JSONException {
        receiver.onNext(new Progress("started"));

        // attachments
        List<String> fileIds = new ArrayList<>();
        if (attachments != null) {
            for (int i = 0; i < attachments.size(); i++)
                fileIds.add("" + i);
        }

        // start request
        multiPartTransfer = new HTTPMultiPartTransfer(server);

        // upload attachments
        if (attachments != null) {
            for (int i = 0; i < attachments.size(); i++) {
                final Uri uri = attachments.get(i);
                final String name = uri.getLastPathSegment();
                ParcelFileDescriptor fd = activity.getContentResolver().openFileDescriptor(uri, "r");
                final long fileSize = fd.getStatSize();
                fd.close();

                receiver.onNext(new Progress(name));

                OutputStream outputStream = multiPartTransfer.addFile(fileIds.get(i), name);
                InputStream attachmentStream = activity.getContentResolver().openInputStream(uri);
                StreamHelper.copy(attachmentStream, outputStream, new StreamHelper.IProgressListener() {
                    @Override
                    public int getReportingStep() {
                        return 10 * 1024;
                    }

                    @Override
                    public void onNewProgress(int totalProgress) {
                        final int KBYTE = 1024;
                        String update = name;
                        update += " " + totalProgress / KBYTE + "/" + fileSize / KBYTE + " kBytes";
                        receiver.onNext(new Progress(update));
                    }
                });
                attachmentStream.close();
            }
        }

        // do rpc once all files are uploaded
        doRPC(multiPartTransfer, "upload", new Argument("files", fileIds), new Argument("groupMembers", groupMembers));

        receiver.onNext(new Progress("wait for response"));

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
        receiver.onNext(new Progress("done"));
    }
}
