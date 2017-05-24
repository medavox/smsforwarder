package com.medavox.smsforwarder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.TextView;

import com.medavox.util.io.Bytes;

import static com.medavox.util.io.Bytes.bytesToHex;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "SMSForwarder";
    private static final int smsSendRequestCode = 42;
    private static final int smsReceiveRequestCode = 43;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //support runtime permission checks on android versions >= 6.0
        //if we're on android 6+ AND we haven't got location permissions yet, ask for them
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            // todo: Show an explanation to the user *asynchronously*
            // After the user sees the explanation, try again to request the permission.

            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, smsReceiveRequestCode);
        }
        else {
            Intent i = this.registerReceiver(br, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
        }

        //support runtime permission checks on android versions >= 6.0
        //if we're on android 6+ AND we haven't got location permissions yet, ask for them
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            // todo: Show an explanation to the user *asynchronously*
            // After the user sees the explanation, try again to request the permission.

            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, smsSendRequestCode);
        }
        else {
            SmsManager sm = SmsManager.getDefault();
            //by 'data message' they mean an SMS sent over 3G or higher networks
            //the method we want, sendRawPdu, has been hidden/deleted after 4.3
            /*sm.sendDataMessage(
                    "07564600990",
                    null,//service centre address. default is used if this is null.
                    (short)6565,//destination port???
                    "data message lol".getBytes(),
                    null,
                    null
            );*/
            sm.sendTextMessage("07516041435", null, "lol i did this", null, null);
        }



    }

    private BroadcastReceiver br  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                Log.i(TAG,"number of messages: "+msgs.length);
                for(SmsMessage msg : msgs) {
                    //boolean muh = msg.getProtocolIdentifier() == SmsM
                    //todo:find way to work out if this is a data or a text message
                    boolean isData = true;
                    TextView tv = (TextView)MainActivity.this.findViewById(R.id.texty);
                    if(isData) {
                        msg.getUserData();
                        tv.setText("data message from "+msg.getOriginatingAddress()+": "+
                            bytesToHex(msg.getUserData()));
                    }
                    else {
                        //is text
                        Log.i(TAG, "message: \""+msg+"\"");

                        tv.setText("message from "+msg.getOriginatingAddress()+": "+
                                msg.getMessageBody());
                    }
                }
            }
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        //todo: handle multiple permissions
        //if there are no permissions etc to process, return early.
//See https://developer.android.com/reference/android/support/v4/app/ActivityCompat.OnRequestPermissionsResultCallback.html#onRequestPermissionsResult%28int,%20java.lang.String[],%20int[]%29
        if(permissions.length != 1 || grantResults.length != 1) {
            return;
        }
        Log.i(TAG, "permissions results length:" + permissions.length);


        Log.i(TAG, "permission \"" + permissions[0] + "\" result: " + grantResults[0]);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

        }
        else if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //the user has granted permission, so start the location service
            //this happens by starting the service in onResume()
        }
    }
}
