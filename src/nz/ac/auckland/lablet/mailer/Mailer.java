package nz.ac.auckland.lablet.mailer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

                mail(upi, password);
            }
        });
    }

    private void mail(String upi, String password) {
        Intent intent = getIntent();

        String senderEmail = upi + "@aucklanduni.ac.nz";
        final MailSender mailSender = new MailSender();
        mailSender.setHost("smtp.googlemail.com");
        mailSender.setUser(senderEmail);
        mailSender.setPassword(password);

        mailSender.setFrom(senderEmail);
        String toArray[] = new String[1];
        toArray[0] = senderEmail;
        mailSender.setTo(toArray);
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (subject == null)
            subject = "UoA mailer";
        String body = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (body == null)
            body = "UoA empty message";
        mailSender.setSubject(subject);
        mailSender.setBody(body);
        Uri uri = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
        try {
            if (uri != null)
                mailSender.addAttachment(uri.getPath());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "File attachment not found: " + uri.getPath(), Toast.LENGTH_LONG).show();
            return;
        }

        new SendAsyncTask().execute(mailSender);
    }

    class SendAsyncTask extends AsyncTask<MailSender, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(MailSender... mailSenders) {
            try {
                mailSenders[0].send();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

        }

    }
}
