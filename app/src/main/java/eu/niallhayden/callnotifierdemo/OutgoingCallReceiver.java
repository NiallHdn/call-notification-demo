package eu.niallhayden.callnotifierdemo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;

public class OutgoingCallReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "main";

    public OutgoingCallReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String captureNo = settings.getString("input_phone_no_to_capture", "");

        if (phoneNumber.equals(captureNo)) {
            String smsNo = settings.getString("input_sms_notification_no", "");
            String email = settings.getString("input_email_notification_address", "");

            //Send SMS
            if ( smsNo != "" ) {

                PendingIntent piSent = PendingIntent.getBroadcast(context, 0,
                        new Intent(SmsSentReceiver.SENT), 0);

                PendingIntent piDelivered = PendingIntent.getBroadcast(context, 0,
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
                new SendMailAsync().execute(context);
            }
        }


        String msg = String.format("Phone No. Called %s", phoneNumber);
        Log.d("OUTGOINGCALL", msg);
    }
}
