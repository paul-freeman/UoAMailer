/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;


public class HTTPMultiPartTransfer {
    final private String boundary = "===" + System.currentTimeMillis() + "===";
    final private String LINE_FEED = "\r\n";

    final private HttpURLConnection connection;
    private OutputStream outputStream = null;
    private PrintWriter writer = null;
    private IState currentState = null;

    public HTTPMultiPartTransfer(URL url) throws IOException {
        connection = (HttpURLConnection)url.openConnection();

        connection.setUseCaches(false);
        connection.setDoOutput(true); // indicates POST method
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("Accept-Charset", "utf-8");

        outputStream = connection.getOutputStream();
        try {
            writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
    }

    private interface IState {
        void enter();
        void leave();
    }

    abstract private class AbstractState implements IState {
        @Override
        public void leave() {
            writer.append(LINE_FEED);
        }
    }

    private void setState(IState state) {
        if (currentState != null)
            currentState.leave();

        currentState = state;

        if (currentState != null)
            currentState.enter();
    }

    private class SendFileState extends AbstractState {
        private String name;
        private String filename;

        public SendFileState(String name, String filename) {
            this.name = name;
            this.filename = filename;
        }

        @Override
        public void enter() {
            // header
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(filename).append(LINE_FEED);

            // body
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Type: \"text/plain\"").append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"")
                    .append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();
        }
    }

    private class SendDataState extends AbstractState {
        private String name;
        private String smallData;

        public SendDataState(String name, String smallData) {
            this.name = name;
            this.smallData = smallData;
        }

        @Override
        public void enter() {
            // header
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED);
            writer.append(LINE_FEED);
            if (smallData != null)
                writer.append(smallData);

            writer.flush();
        }
    }

    public void addSmallData(String name, String data) {
        setState(new SendDataState(name, data));
        setState(null);
    }

    public PrintWriter addData(String name) {
        setState(new SendDataState(name, null));
        return writer;
    }

    public PrintWriter addFile(String name, String filename) {
        setState(new SendFileState(name, filename));
        return writer;
    }

    public InputStream receive() throws IOException {
        setState(null);

        // finish
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.append(LINE_FEED).flush();
        writer.close();

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK)
            throw new IOException("Bad server response: " + status);

        return connection.getInputStream();
    }

    public void disconnect() {
        connection.disconnect();
    }

    @Override
    public void finalize() {
        disconnect();
    }
}
