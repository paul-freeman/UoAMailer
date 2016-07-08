/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;

import android.content.Context;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;


public class HTTPMultiPartTransfer {
    final private String boundary = "===" + System.currentTimeMillis() + "===";
    final private String LINE_FEED = "\r\n";

    final private HttpURLConnection connection;
    private OutputStream outputStream = null;
    private PrintWriter writer = null;
    private IState currentState = null;
    private InputStream inputStream =null;

    final private Context context;

    public HTTPMultiPartTransfer(URL url, Context context) throws IOException {
        this.context = context;

        connection = openConnection(url);

        connection.setUseCaches(false);
        connection.setDoOutput(true); // indicates POST method
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("Accept-Charset", "utf-8");
        connection.setChunkedStreamingMode(8 * 1024);

        outputStream = connection.getOutputStream();
        try {
            writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
    }

    private HttpURLConnection getCertifiedConnection(URL url, File cert) throws IOException {
        // from: https://developer.android.com/training/articles/security-ssl.html

        // Load CAs from an InputStream
        // (could be from a resource or ByteArrayInputStream or ...)
        SSLContext context;
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");

            InputStream caInput = new BufferedInputStream(new FileInputStream(cert));
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
        } catch (CertificateException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } catch (KeyStoreException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } catch (KeyManagementException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }

        // Tell the URLConnection to use a SocketFactory from our SSLContext
        HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
        urlConnection.setSSLSocketFactory(context.getSocketFactory());
        return urlConnection;
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        if (url.getProtocol().equals("https")) {
            File baseDir = context.getExternalFilesDir(null);
            File file = new File(baseDir, url.getHost() + ".cert");
            if (file.exists())
                return getCertifiedConnection(url, file);
        }
        return (HttpURLConnection)url.openConnection();
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

    public OutputStream addFile(String name, String filename) {
        setState(new SendFileState(name, filename));
        return outputStream;
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

        inputStream = connection.getInputStream();
        return inputStream;
    }

    public void disconnect() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        connection.disconnect();
        outputStream = null;
    }

    @Override
    public void finalize() {
        disconnect();
    }
}
