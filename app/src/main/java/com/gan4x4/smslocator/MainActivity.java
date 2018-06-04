package com.gan4x4.smslocator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    final static int MY_PERMISSIONS_REQUEST = 3;
    final String[] PERMISSIONS_LIST = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS};
    final static String PASSPHRASE_KEY = "passphrase";
    final static String PREFERENCES_KEY = "SMSLocator_SP";
    final static String PHONE_KEY = "phone";

    private static final Intent[] AUTO_START_INTENTS = {
            new Intent().setComponent(new ComponentName("com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity")),
            new Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
            new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")).setData(
                    Uri.parse("mobilemanager://function/entry/AutoStart"))
    };

    String phone;
    TextView tePassphrase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for (Intent intent : AUTO_START_INTENTS){
            if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(intent);
                break;
            }
        }
        storePhoneFromIntent();
        initPassphraseEdit();
    }

    /*
        If starting intent contain phone field, it's meat that broadcast receiver fail to send
        location to this phone because location permission was disabled.
        That phone must be stored, to retry send when permission was granted
     */
    private void storePhoneFromIntent(){
        Intent intent = getIntent();
        phone = intent.getStringExtra(PHONE_KEY);
    }

    private void initPassphraseEdit(){
        tePassphrase = findViewById(R.id.te_passphrase);
        tePassphrase.setText(getCurrentPassphrase(this));
    }


    @Override
    public void onResume(){
        requestPermission();
        checkGPS();
        enableBroadcastReceiver(false);
        super.onResume();
    }

    @Override
    public void onPause(){
        savePassphrase();
        super.onPause();
    }

    private void savePassphrase(){
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(PREFERENCES_KEY, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PASSPHRASE_KEY, getPassphraseFromInput());
        editor.commit();
    }


    /*
    This method was static because it can be called from BroadcastReceiver
     */
    public static String getCurrentPassphrase(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_KEY, 0);
        return prefs.getString(PASSPHRASE_KEY,context.getString(R.string.default_passphrase));
    }

    private String getPassphraseFromInput(){
        String userInput = tePassphrase.getText().toString().toLowerCase().trim();
        if (userInput.isEmpty()){
            return getString(R.string.default_passphrase);
        }
        return userInput;
    }


    public void requestPermission() {

        String[] missingPermissions = getMissingPermission();

        if (missingPermissions.length > 0) {

            // Permission is not granted
            // Should we show an explanation?
            //if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            //        Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            //} else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        missingPermissions,
                        MY_PERMISSIONS_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            //}
        } else {
            // Permission has already been granted
        }
    }

    private String[] getMissingPermission(){
        List<String> denied = new ArrayList<String>();
        for(String permission : this.PERMISSIONS_LIST){
            if (ContextCompat.checkSelfPermission(this,permission)
                    != PackageManager.PERMISSION_GRANTED){
                denied.add(permission);
            }
        }
        String[] simpleArray = new String[ denied.size() ];
        return denied.toArray( simpleArray );
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    enableBroadcastReceiver(true);
                    if (this.phone != null){
                        Intent intentp = new Intent("com.gan4x4.LOCATION_UPDATE");
                        intentp.putExtra("phone",phone);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intentp,PendingIntent.FLAG_UPDATE_CURRENT);
                        phone = null;
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    enableBroadcastReceiver(false);
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void checkGPS(){
        final Activity activity = this;
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled ) {
            // notify user

            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setTitle(R.string.gps_network_not_enabled);
            dialog.setMessage(activity.getResources().getString(R.string.enabling_gps_explanation));
            dialog.setPositiveButton(activity.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // Open device settings
                    Intent settingsIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivity(settingsIntent);
                    paramDialogInterface.cancel();
                }
            });
            dialog.setNegativeButton(activity.getString(R.string.exit_application), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // Exit from application
                    activity.finish();
                    enableBroadcastReceiver(false);
                }
            });
            dialog.show();
        }
    }

    private void enableBroadcastReceiver(boolean enable){
        ComponentName component = new ComponentName(this, SmsListener.class);
        int status = this.getPackageManager().getComponentEnabledSetting(component);
        if(status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED && enable == false) {
            //Disable
            this.getPackageManager().
                    setComponentEnabledSetting(component,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED ,
                            PackageManager.DONT_KILL_APP);

        } else if(status == PackageManager.COMPONENT_ENABLED_STATE_DISABLED && enable == true) {
            //Enable
            this.getPackageManager().
                    setComponentEnabledSetting(component,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED ,
                            PackageManager.DONT_KILL_APP);
        }
    }

}
