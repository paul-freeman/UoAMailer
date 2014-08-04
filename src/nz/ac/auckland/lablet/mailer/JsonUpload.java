/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class JsonUpload extends HTTPJsonRequest {
    private HTTPMultiPartTransfer multiPartTransfer;

    public Observable<Boolean> upload(final URL server) {
        return Observable.create(new Observable.OnSubscribeFunc<Boolean>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Boolean> receiver) {
                try {
                    boolean result = sendUploadRequest(server);

                    receiver.onNext(result);
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

    private boolean sendUploadRequest(URL server) throws IOException, JSONException {
        List<String> fileIds = new ArrayList();
        fileIds.add("test1");
        fileIds.add("test2");
        multiPartTransfer = sendOverHTTP(server, "upload", new Argument("files", fileIds));
        multiPartTransfer.addFile("test1", "test1.txt").append("test1 data");
        multiPartTransfer.addFile("test2", "test2.txt").append("test2 data");

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

        if (returnValue.has("error"))
            return returnValue.getInt("error") == 0;

        return false;
    }
}
