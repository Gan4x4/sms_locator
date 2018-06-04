package com.gan4x4.smslocator;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsListener extends BroadcastReceiver {
    final static double minimalAccuracy = 50.0;

    Location location = null;
    int attempt = 0;
    String phone = null;
    Context context = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        /*
            New SMS
         */
        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
            SmsMessage[] messages = extractSmsFromIntent(intent);
            String passphrase = MainActivity.getCurrentPassphrase(context);
            for(SmsMessage m : messages){
                if (m.getMessageBody().toLowerCase().trim().equals(passphrase.toLowerCase())){
                    this.phone = m.getOriginatingAddress();
                    sendSMS("SMSLocator start");
                    tryGetLocation();
                }
            }
        }

        /*
            Request from activity after getting permission
        */
        if(intent.getAction().equals("com.gan4x4.SEND_LOCATION")){
            readIntentExtras(intent); // Contain only phone
            if (phone != null && ! phone.isEmpty()){
                tryGetLocation();
            }
        }

        /*
            Request from GPS when it determine current location
        */
        if (intent.getAction().equals("com.gan4x4.LOCATION_UPDATE")){
            readIntentExtras(intent);
            if (location != null && phone != null) {
                if (location.hasAccuracy() && location.getAccuracy() < minimalAccuracy) {
                    //Log.d("SMSListener", "Location updated " + location.getLatitude() + " " + location.getLongitude());
                    sendSMS(composeSmsText(location));
                }
                else{
                    Log.d("SMSListener", "Location updated but accuracy is bad:" + location.getAccuracy());
                    tryGetLocation();
                }
            }else {
                Log.d("SMSListener", "Location updated but empty");
            }

        }

    }

    private void readIntentExtras(Intent intent){
        String key = LocationManager.KEY_LOCATION_CHANGED;
        location = (Location) intent.getExtras().get(key);
        phone = intent.getExtras().getString("phone");
        attempt = intent.getExtras().getInt("attempt",0);
    }

    public SmsMessage[] extractSmsFromIntent(Intent intent){
        SmsMessage[] msgs = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        } else{
            // For ver. prior 19
            Bundle bundle = intent.getExtras();
            if (bundle != null){
                //---retrieve the SMS message received---
                Object[] pdus = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];
                for(int i=0; i<msgs.length; i++){
                    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                }
            }
        }
        return msgs;
    }


    public void tryGetLocation(){
        try {
            sendLocationRequest();
        } catch ( SecurityException e ) {
            openActivityToRequestPermission();
        }
    }

    private void sendLocationRequest() throws SecurityException {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Intent intentp = new Intent("com.gan4x4.LOCATION_UPDATE");
        intentp.putExtra("phone",phone);
        intentp.putExtra("attempt",attempt++);
        intentp.putExtra("satellites",7);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentp,PendingIntent.FLAG_UPDATE_CURRENT);
        locationManager.requestSingleUpdate (LocationManager.GPS_PROVIDER,pendingIntent);
    }

    private void openActivityToRequestPermission(){
        Intent i = new Intent();
        i.putExtra("phone",phone);
        i.setClassName(context.getPackageName(), MainActivity.class.getName());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    /**
        New 2017 api  for google maps
        https://developers.google.com/maps/documentation/urls/guide#universal-cross-platform-syntax
    */
    public String composeSmsText(Location loc){
        Float accuracy = loc.getAccuracy();
        String url = "https://www.google.com/maps/search/?api=1&zoom="+getZoom(accuracy)+"&query="+loc.getLatitude()+","+loc.getLongitude();


        //String url = "http://maps.google.com/?geo="+loc.getLatitude()+","+loc.getLongitude();
        //url += "\n "+"yandexmaps://maps.yandex.ru/?pt="+loc.getLatitude()+","+loc.getLongitude();
        //url += "\n "+"yandexmaps://maps.yandex.ru/?pt="+loc.getLatitude()+","+loc.getLongitude();
        //url += "\n "+"http://maps.google.com/?ll="+loc.getLatitude()+","+loc.getLongitude();
        //url = "intent://geo:"+loc.getLatitude()+","+loc.getLongitude()+"?q="+loc.getLatitude()+","+loc.getLongitude();
        return url+"\n accuracy:"+accuracy+"\n attempt: "+attempt;
    }

    public int getZoom(Float accuracy){
        return 20;
    }

    public void sendSMS(String text){
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phone, null, text, null, null);
    }
}
