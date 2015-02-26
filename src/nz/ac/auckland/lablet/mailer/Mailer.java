/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.net.*;
import java.util.ArrayList;
import java.util.List;


public class Mailer extends Activity {
    private List<Uri> attachments;
    private GroupMembers groupMembers = new GroupMembers();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // this makes it easier to copy config files into the data directory
        ensureDataDir();

        getAttachments();

        final ListView listView = (ListView)findViewById(R.id.listView);
        if (attachments != null) {
            List<String> attachmentNames = new ArrayList<>();
            for (Uri uri : attachments)
                attachmentNames.add(uri.getLastPathSegment());
            listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, attachmentNames));
        }

        final Button sendButton = (Button)findViewById(R.id.sendButton);

        final EditText upiEditText = (EditText)findViewById(R.id.upiEditText);
        final EditText passwordEditText = (EditText)findViewById(R.id.passwordEditText);
        final GroupMemberView groupMemberView = (GroupMemberView)findViewById(R.id.groupMemberView);
        final Button addButton = (Button)findViewById(R.id.addButton);

        groupMemberView.setGroupMembers(groupMembers);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String upi = upiEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                showSendingDialog(upi, password);
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                groupMembers.addMember("");
            }
        });

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    private void ensureDataDir() {
        File baseDir = getExternalFilesDir(null);
        baseDir.mkdirs();
    }

    private void getAttachments() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            attachments = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (attachments == null) {
                Object object = extras.get(Intent.EXTRA_STREAM);
                if (object != null && object instanceof Uri) {
                    attachments = new ArrayList<>();
                    attachments.add((Uri)object);
                }
            }
        }
    }

    private void showSendingDialog(String upi, String password) {
        if (attachments == null)
            finish();

        SendingDialog sendingDialog = new SendingDialog(this, upi, password);
        sendingDialog.setAttachments(attachments);
        sendingDialog.setGroupMembers(groupMembers);
        sendingDialog.show();
    }

}
