package eu.niallhayden.callnotifierdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

/**
 * Created by niall hayden on 31/01/2016.
 */
public class SendMailAsync extends AsyncTask<Context, Integer, Integer> {

    private static final String PREFS_NAME = "main";
    private static final String PREF_MAIL_TOKEN = "mail_token";
    private static final String PREF_MAIL_FROM = "mail_from";
    private static final String INPUT_EMAIL_NOTIFICATION_ADDRESS = "input_email_notification_address";

    private static final String HOST = "smtp.gmail.com";
    private static final Integer PORT = 587;

    private static final String SUBJECT = "Trigger Pull Alert";
    private static final String BODY = "Oooo thats gonna sting for a while....";
    private static final int SUCCESS = 0;
    private static final int FAILED_NO_VALID_TOKEN = -1;
    private static final int FAILED_ERROR_LOGGED = -2;

    private static Properties mProperties = null;

    private Context mContext;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Integer doInBackground(Context... params) {

        mContext = params[0];

        Properties props = getProperties();

        Session session = Session.getInstance(props);
        session.setDebug(true);

        final URLName unusedUrlName = null;
        try {
            SMTPTransport transport = new SMTPTransport(session, unusedUrlName);

            /* enable if you use this code on an Activity (just for test) or use the AsyncTask
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
             */

            final SharedPreferences settings = params[0].getSharedPreferences(PREFS_NAME, 0);
            String mailToken = settings.getString(PREF_MAIL_TOKEN, "");
            String from = settings.getString(PREF_MAIL_FROM, "");
            String recipients = settings.getString(INPUT_EMAIL_NOTIFICATION_ADDRESS, "");

            if ( mailToken == null || mailToken.length() <= 0 ) {
                return FAILED_NO_VALID_TOKEN;
            }
            transport.connect(HOST, PORT, from, null);

            byte[] response = String.format("user=%s\1auth=Bearer %s\1\1",
                    from, mailToken).getBytes();
            response = BASE64EncoderStream.encode(response);

            transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);

            MimeMessage message = new MimeMessage(session);
            DataHandler handler = new DataHandler(new ByteArrayDataSource(
                    BODY.getBytes(), "text/plain"));
            message.setSender(new InternetAddress(from));
            message.setSubject(SUBJECT);
            message.setDataHandler(handler);
            if (recipients.indexOf(',') > 0)
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipients));
            else
                message.setRecipient(Message.RecipientType.TO,
                        new InternetAddress(recipients));
            transport.sendMessage(message, message.getAllRecipients());

            return SUCCESS;
        } catch ( Exception ex ) {
            Log.d("test", ex.getMessage(), ex);
            return FAILED_ERROR_LOGGED;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        switch (result) {
            case SUCCESS: {
                Toast.makeText(mContext, "EMail Sent", Toast.LENGTH_LONG).show();
                break;
            }
            case FAILED_NO_VALID_TOKEN: {
                Toast.makeText(mContext, "EMail Failed - A valid auth token could no be found.", Toast.LENGTH_LONG).show();
                break;
            }
            case FAILED_ERROR_LOGGED: {
                Toast.makeText(mContext, "EMail Failed - Error has been logged", Toast.LENGTH_LONG).show();
                break;
            }
            default: {
                Toast.makeText(mContext, "EMail Failed - An Unknown Error Occurred", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    private static Properties getProperties() {
        if ( mProperties == null ) {
            mProperties = new Properties();
            mProperties.put("mail.smtp.starttls.enable", "true");
            mProperties.put("mail.smtp.starttls.required", "true");
            mProperties.put("mail.smtp.sasl.enable", "false");
            mProperties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        }
        return mProperties;
    }
}
