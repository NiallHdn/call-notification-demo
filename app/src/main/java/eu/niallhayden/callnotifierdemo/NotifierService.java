package eu.niallhayden.callnotifierdemo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class NotifierService extends Service {

    private static BroadcastReceiver mCallNotifierReceiver;
    private static IntentFilter mCallNotifierFilter;

    public NotifierService() {
        mCallNotifierReceiver = new OutgoingCallReceiver();
        mCallNotifierFilter = new IntentFilter("android.intent.action.NEW_OUTGOING_CALL");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        this.registerReceiver(mCallNotifierReceiver, mCallNotifierFilter);
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(mCallNotifierReceiver);
    }
}
