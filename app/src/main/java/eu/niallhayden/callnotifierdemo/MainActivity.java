package eu.niallhayden.callnotifierdemo;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "main";
    private static final String PREF_MAIL_TOKEN = "mail_token";
    private static final String PREF_MAIL_FROM = "mail_from";


    private EditText mCaptureNo;
    private EditText mSmsNo;
    private EditText mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String captureNo = settings.getString("input_phone_no_to_capture", "");
        String smsNo = settings.getString("input_sms_notification_no", "");
        String email = settings.getString("input_email_notification_address", "");

        mCaptureNo = (EditText)findViewById(R.id.input_phone_no_to_capture);
        mSmsNo = (EditText)findViewById(R.id.input_sms_notification_no);
        mEmail = (EditText)findViewById(R.id.input_email_notification_address);

        if (captureNo != "") mCaptureNo.setText(captureNo);
        if (smsNo != "") mSmsNo.setText(smsNo);
        if (email != "") mEmail.setText(email);

        Button save = (Button)findViewById(R.id.button_save);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cNo = mCaptureNo.getText().toString();
                String sNo = mSmsNo.getText().toString();
                String eM = mEmail.getText().toString();
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor e = settings.edit();
                e.putString("input_phone_no_to_capture", cNo);
                e.putString("input_sms_notification_no", sNo);
                e.putString("input_email_notification_address", eM);
                e.apply();

                Snackbar.make(v, "Settings Updated", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Button testSend = (Button)findViewById(R.id.button_send_test);

        testSend.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String smsNo = settings.getString("input_sms_notification_no", "");
                String email = settings.getString("input_email_notification_address", "");

                //Send SMS
                if ( smsNo != "" ) {

                    PendingIntent piSent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                            new Intent(SmsSentReceiver.SENT), 0);

                    PendingIntent piDelivered = PendingIntent.getBroadcast(getApplicationContext(), 0,
                            new Intent(SmsDeliveredReceiver.DELIVERED), 0);

                    SmsManager.getDefault().sendTextMessage(
                            smsNo,
                            null,
                            "Tazer Shot Fired!!! Oooh someone's a hurtin' ",
                            piSent,
                            piDelivered
                    );
                }

                //Send Email
                if ( email != "" ) {
                    sendMail();
                }
            }
        });

        getApplicationContext().registerReceiver(
                new SmsSentReceiver(),
                new IntentFilter(SmsSentReceiver.SENT)
        );
        getApplicationContext().registerReceiver(
                new SmsDeliveredReceiver(),
                new IntentFilter(SmsDeliveredReceiver.DELIVERED)
        );

    }

    public void sendMail() {
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        AccountManager am = AccountManager.get(this);

        Account[] accounts = am.getAccountsByType("com.google");

        String meName = "";
        Account me = null;
        if (accounts != null && accounts.length > 0 ) {
            me = accounts[0]; //You need to get a google account on the device, it changes if you have more than one
            meName = me.name;
        }

        final String from = meName;
        if (me != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                .setTitle("Configuring...")
                .setMessage("Please Wait While we Setup the App to Send Email")
                .setCancelable(false);

            final Dialog wait = builder.create();
            wait.show();
            am.getAuthToken(me, "oauth2:https://mail.google.com/", null, this, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> result) {
                    try {
                        Bundle bundle = result.getResult();
                        String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                        Log.d("initToken callback", "token=" + token);

                        SharedPreferences.Editor e = settings.edit();
                        e.putString(PREF_MAIL_TOKEN, token);
                        e.putString(PREF_MAIL_FROM, from);
                        e.apply();

                        new SendMailAsync().execute(getApplicationContext());
                    } catch (Exception e) {
                        Log.d("test", e.getMessage());
                    } finally {
                        wait.dismiss();
                    }
                }
            }, null);
        } else {
            Log.d("GMailSender.initToken", "No GMail accounts found");
            Toast.makeText(getApplicationContext(),
                    "GMail Account not found. EMail Cannot be Sent. Configure a GMail Account on the Device to Enable EMail Notifications.",
                    Toast.LENGTH_LONG).show();
        }

    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
