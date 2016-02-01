package eu.niallhayden.callnotifierdemo;

/**
 * Created by mrnia on 31/01/2016.
 * taken from http://stackoverflow.com/questions/6517079/send-email-in-service-without-prompting-user
 */
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

public final class GMailSender {

    private static final String PREFS_NAME = "main";
    private static final String PREF_MAIL_TOKEN = "mail_token";

    public static boolean initToken(Activity ctx) {
        boolean res = false;
        final SharedPreferences settings = ctx. getSharedPreferences(PREFS_NAME, 0);

        String mailToken = settings.getString(PREF_MAIL_TOKEN, "");

        if (mailToken == "") {

            AccountManager am = AccountManager.get(ctx);

            Account[] accounts = am.getAccountsByType("com.google");
            for (Account account : accounts) {
                Log.d("getToken", "account="+account);
            }

            Account me = null;
            if (accounts != null && accounts.length > 0 ) {
                me = accounts[0]; //You need to get a google account on the device, it changes if you have more than one
            }

            if (me != null) {
                res = true;
                am.getAuthToken(me, "oauth2:https://mail.google.com/", null, ctx, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> result) {
                        try {
                            Bundle bundle = result.getResult();
                            String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                            Log.d("initToken callback", "token=" + token);

                            SharedPreferences.Editor e = settings.edit();
                            e.putString(PREF_MAIL_TOKEN, token);
                            e.apply();
                        } catch (Exception e) {
                            Log.d("test", e.getMessage());
                        }
                    }
                }, null);
            } else {
                Log.d("GMailSender.initToken", "No GMail accounts found");
                return false;
            }
        } else {
            res = true;
        }

        return res;
    }

    private static Properties mProperties = null;
    private static Properties getProperties() {
        if ( mProperties == null ) {
            mProperties = new Properties();
            mProperties.put("mail.smtp.starttls.enable", "true");
            mProperties.put("mail.smtp.starttls.required", "true");
            mProperties.put("mail.smtp.sasl.enable", "false");
        }
        return mProperties;
    }

    private static SMTPTransport connectToSmtp(SharedPreferences preferences,
                                       String host, int port, String userEmail,
                                       boolean debug) throws Exception {

        Properties props = getProperties();

        Session session = Session.getInstance(props);
        session.setDebug(debug);

        final URLName unusedUrlName = null;
        SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
        // If the password is non-null, SMTP tries to do AUTH LOGIN.
        final String emptyPassword = null;

        /* enable if you use this code on an Activity (just for test) or use the AsyncTask
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
         */

        final SharedPreferences settings = preferences;
        String mailToken = settings.getString(PREF_MAIL_TOKEN, "");

        transport.connect(host, port, userEmail, emptyPassword);

        byte[] response = String.format("user=%s\1auth=Bearer %s\1\1",
                userEmail, mailToken).getBytes();
        response = BASE64EncoderStream.encode(response);

        transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);

        return transport;
    }

    public static void sendMail(SharedPreferences preferences,
                                      String subject, String body, String user,
                                      String recipients) {
        try {

            SMTPTransport smtpTransport = connectToSmtp(
                    preferences, "smtp.gmail.com", 587, user, true
            );

            Properties props = getProperties();
            Session session = Session.getInstance(props);

            MimeMessage message = new MimeMessage(session);
            DataHandler handler = new DataHandler(new ByteArrayDataSource(
                    body.getBytes(), "text/plain"));
            message.setSender(new InternetAddress(user));
            message.setSubject(subject);
            message.setDataHandler(handler);
            if (recipients.indexOf(',') > 0)
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipients));
            else
                message.setRecipient(Message.RecipientType.TO,
                        new InternetAddress(recipients));
            smtpTransport.sendMessage(message, message.getAllRecipients());

        } catch (Exception e) {
            Log.d("test", e.getMessage(), e);
        }
    }

}
