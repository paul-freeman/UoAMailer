/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import java.util.Locale;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.concurrency.Schedulers;


import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class SendingDialog extends AlertDialog {
    private static final String TAG = "SendingDialog";
    private static final int BUFFER_SIZE = 1024;
    final private Activity activity;
    private TextView statusView;
    private Subscription currentSubscription = null;
    private String upi;
    private String password;
    private Button okButton;
    private Button cancelButton;
    private URL serverAddress;

    private List<String> groupMembers = new ArrayList<>();
    private List<Uri> attachments;

    private boolean done = false;
    private final File externalCacheDir;

    public SendingDialog(Activity activity, String upi, String password) {
        super(activity);

        setCancelable(false);

        this.activity = activity;
        this.upi = upi;
        this.password = password;
        // save any files into this directory
        externalCacheDir = activity.getExternalCacheDir();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.sending_dialog, null);
        setTitle("Sending...");
        addContentView(contentView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // scale length
        statusView = (TextView)contentView.findViewById(R.id.statusText);
        String text = "";
        statusView.setText(text);

        // button bar
        cancelButton = (Button)contentView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unsubscribe();
                dismiss();
            }
        });

        okButton = (Button)contentView.findViewById(R.id.okButton);
        okButton.setEnabled(false);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (done) {
                    Intent resultData = new Intent();
                    resultData.putExtra("status", "sent");
                    activity.setResult(Activity.RESULT_OK, resultData);
                    activity.finish();
                }
            }
        });

        try {
            serverAddress = new URL(readServerAddress());
            mail(upi, password);
        } catch (MalformedURLException e) {
            setError("Can't connect to server.");
        }

        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                unsubscribe();
            }
        });
    }

    private void unsubscribe() {
        if (currentSubscription != null) {
            currentSubscription.unsubscribe();
            currentSubscription = null;
        }
    }
    private void setError(String message) {
        unsubscribe();

        statusView.setText(message);
        cancelButton.setEnabled(false);
        okButton.setEnabled(true);
    }

    private void setDone() {
        unsubscribe();

        done = true;

        okButton.setEnabled(true);
        cancelButton.setEnabled(false);

        // remove cache files
        final File[] files = externalCacheDir.listFiles();
        Log.i(TAG, String.format(Locale.UK, "Deleting %d files from cache directory", files.length));
        int filesRemaining = 0;
        for (File file : files) {
            if (!file.delete()) {
                filesRemaining++;
            }
        }
        if (filesRemaining > 0) {
            Log.e(TAG, String
                .format(Locale.UK, "Could not delete %d files in cache directory", filesRemaining));
        }
    }

    private void setProgress(String message) {
        statusView.setText(message);
    }

    private String readServerAddress() {
        File baseDir = getContext().getExternalFilesDir(null);
        File file = new File(baseDir, "config");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            return reader.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void mail(final String upi, final String password) {
        JsonLogin jsonLogin = new JsonLogin(getContext());

        setProgress("login...");
        currentSubscription = jsonLogin.login(serverAddress, upi, password).subscribeOn(Schedulers.threadPoolForIO())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        setError("login failed");
                    }

                    @Override
                    public void onNext(Boolean results) {
                        if (results) {
                            Log.d(TAG, "login ok");
                            setProgress("login ok");
                            uploadData();
                        } else
                            Log.d(TAG, "login failed");
                            setError("login failed");
                    }
                });
    }

    private void uploadData() {
        setProgress("Upload");
        JsonUpload jsonUpload = new JsonUpload(activity, attachments, groupMembers);
        currentSubscription = jsonUpload.upload(serverAddress).subscribe(new Observer<JsonUpload.Progress>() {
                @Override
                public void onCompleted() {
                    setDone();
                }

                @Override
                public void onError(Throwable throwable) {
                    setError("Upload failed");
                }

                @Override
                public void onNext(JsonUpload.Progress progress) {
                    Log.d(TAG, "Upload: " + progress.info);
                    setProgress("Upload: " + progress.info);
                }
            });
    }

    public void setGroupMembers(GroupMembers groupMembers) {
        this.groupMembers.clear();
        if (!this.groupMembers.contains(upi))
            this.groupMembers.add(upi);
        for (int i = 0; i < groupMembers.size(); i++)
            this.groupMembers.add(groupMembers.get(i));
    }

    void setAttachments(List<Uri> uriList) {
        attachments = new ArrayList<>();

        for (Uri uri : uriList) {

            if (uri.getLastPathSegment().matches("[\\w\\-.]+")) {
                // the mailer chokes on filenames with special characters (and maybe spaces)
                // so let's just make sure all files contain only words, '-', and '.'
                Log.d(TAG, "Filename okay: " + uri.getLastPathSegment());
                attachments.add(uri);
            } else {
                // files with invalid names can probably be copied to files with new names
                Log.d(TAG, "Filename not okay: " + uri.getLastPathSegment());
                Uri cacheUri = makeCacheCopy(uri);
                if (cacheUri == null) {
                    continue;
                }
                attachments.add(cacheUri);
            }
        }
    }

  /**
   * Makes a copy of the file referenced by the given {@link Uri} in the external cache directory.
   *
   * @param uri original {@link Uri}
   * @return {@link Uri} to the new file in the cache directory
   */
    @Nullable
    private Uri makeCacheCopy(Uri uri) {
        // get an input stream for the new file
        FileInputStream inData;
        try {
            inData = new FileInputStream(uri.getPath());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not open input stream... skipping " + uri.getLastPathSegment());
            return null;
        }

        // get an output stream for the new file
        final File outFile = getCleanCacheFile(uri);
        final FileOutputStream outData;
        try {
            outData = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not open output stream... skipping " + uri.getLastPathSegment());
            return null;
        }

        // perform the copy
        try {
            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            while ((len = inData.read(buf)) > 0) {
                outData.write(buf, 0, len);
            }
            Log.i(TAG, "File copied to cache directory with valid naming");
        } catch (IOException e) {
            Log.e(TAG, "Could not copy all data... skipping " + uri.getLastPathSegment());
            return null;
        } finally {
            try {
                inData.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close input file");
            }
            try {
                outData.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close output file");
            }
        }
        return Uri.fromFile(outFile);
    }

  /**
   * Creates a *clean* {@link File} object.
   *
   * Basically, the server we are sending files to seems to not handle special
   * characters very well. So this method will create a rather normal file name,
   * which only allows '.' and '_' special characters.
   *
   * @param uri the Android {@link Uri} to clean
   * @return a clean {@link File} object in the device external cache directory
   */
    @NonNull
    private File getCleanCacheFile(Uri uri) {
        final String newName = uri.getLastPathSegment()
            // fix all the invalid characters in the new file name
            .replace(" ", "_")
            .replaceAll("[^\\w\\-.]", "");
        return new File(externalCacheDir, newName);
    }

    public List<Uri> getAttachments() {
        return attachments;
    }
}
