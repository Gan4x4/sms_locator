package com.gan4x4.smslocator;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Storage {
    final static String PASSPHRASE_KEY = "passphrase";
    final static String REQUESTS_KEY = "requests";
    final static String PREFERENCES_KEY = "SMSLocator_SP";

    final static String FIRST_START_KEY = "first_start";

    Context context;
    SharedPreferences prefs;
    private static Storage _storage;

    private Storage(Context context){
        this.context = context;
        prefs = context.getSharedPreferences(PREFERENCES_KEY, 0);
    }

    public static Storage getInstance(Context context){
        if (Storage._storage == null){
            _storage = new Storage(context.getApplicationContext());
        }
        return _storage;
    }

    protected boolean isFirstStart(){
        return prefs.getBoolean(FIRST_START_KEY,true);
    }

    public String getCurrentPassphrase(){
        return prefs.getString(PASSPHRASE_KEY,context.getString(R.string.default_passphrase));
    }

    public void savePassphrase(String phrase){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PASSPHRASE_KEY, phrase);
        editor.commit();
    }


    public boolean hasRequestFromPhone(String phone){
        return getCurrentRequests().contains(phone);
    }

    public void addRequest(String phone){
        if (phone == null){
            return;
        }
        Set<String> currentRequests = getCurrentRequests();
        if (! currentRequests.contains(phone)){
            currentRequests.add(phone);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(REQUESTS_KEY, currentRequests);
            editor.commit();
        }
    }

    public void removeRequest(String phone){
        Set<String> currentRequests = getCurrentRequests();
        if (currentRequests.contains(phone)){
            currentRequests.remove(phone);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(REQUESTS_KEY, currentRequests);
            editor.commit();
        }
    }

    public Set<String> getCurrentRequests(){
        return prefs.getStringSet(REQUESTS_KEY,new HashSet<String>());
    }

    public void clearRequests(){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(REQUESTS_KEY, null);
        editor.commit();
    }

}
