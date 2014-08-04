/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;

import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import java.io.*;
import java.net.URL;


public class JsonLogin extends HTTPJsonRequest {
    private String message = "";
    private boolean loggedIn = false;
    private HTTPMultiPartTransfer multiPartTransfer;

    public Observable<Boolean> login(final URL server, final String username, final String password) {
        return Observable.create(new Observable.OnSubscribeFunc<Boolean>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Boolean> receiver) {
                try {
                    boolean result = sendLoginRequest(server, username, password);

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

    private boolean sendLoginRequest(URL server, String username, String password) throws IOException, JSONException {
        multiPartTransfer = sendOverHTTP(server, "login", new Argument("upi", username),
                new Argument("password", password));
        InputStream inputStream = multiPartTransfer.receive();

        ByteArrayOutputStream receivedData = new ByteArrayOutputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        StreamHelper.copy(bufferedInputStream, receivedData);

        // parse response
        JSONObject returnValue = getReturnValue(receivedData.toString());
        if (returnValue == null)
            throw new IOException("bad return value");

        if (returnValue.has("error"))
            loggedIn = returnValue.getInt("error") == 0;
        if (returnValue.has("message"))
            message = returnValue.getString("message");

        return loggedIn;
    }

    public String getMessage() {
        return message;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
}
