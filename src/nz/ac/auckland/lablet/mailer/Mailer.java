/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.net.*;


public class Mailer extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button sendButton = (Button)findViewById(R.id.sendButton);

        final EditText upiEditText = (EditText)findViewById(R.id.upiEditText);
        final EditText passwordEditText = (EditText)findViewById(R.id.passwordEditText);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String upi = upiEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                showSendingDialog(upi, password);
            }
        });

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    private void showSendingDialog(String upi, String password) {
        SendingDialog sendingDialog = new SendingDialog(this, upi, password);
        sendingDialog.show();
    }

}
