package com.example.android.sip;

import android.annotation.SuppressLint;
import android.app.Application;

import com.facebook.stetho.Stetho;

public class App extends Application {


//    public static String deviceID;
    public static boolean isLoggedIn;
    public static String accessToken;
    public PrefManager prefManager;
    public static String ip="192.168.1.4";
    public static String restApi="http://"+ip+":8000/api/";
    public static String channelAuth="http://"+ip+":8000/api/broadcast/auth";
//    public Snackbar mSnackBar;


    @SuppressLint("CheckResult")
    @Override
    public void onCreate() {


        super.onCreate();
        Stetho.initializeWithDefaults(this);
        prefManager = new PrefManager(this);
//        FirebaseApp.initializeApp(this);
//        RetrofitClient.initialize(this);

        isLoggedIn = prefManager.isLoggedIn();
        accessToken = prefManager.getUserAccessToken();


//        deviceID = Settings.Secure.getString(getContentResolver(),
//                Settings.Secure.ANDROID_ID);


    }

    public PrefManager getPrefManager() {
        return prefManager;
    }


    public static class MessageEvent { /* Additional fields if needed */
    }


}
