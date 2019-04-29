package com.example.android.sip;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pusher.client.Pusher;

import java.text.ParseException;
import java.util.ArrayList;

public class BaseActivity extends AppCompatActivity {


    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPageAdapter adapter;


    private static final int REQUEST_SIP = 10;
    private static final int USERS_ONLINE = 5;
    public static String sipAddress = null;

    private static final String TAG = "APP_DEBUG";
    public SipManager manager = null;
    public SipProfile me = null;
    public SipAudioCall call = null;
    public IncomingCallReceiver callReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        tabLayout = findViewById(R.id.tb_layout);
        viewPager = findViewById(R.id.view_pager);
        adapter = new ViewPageAdapter(getSupportFragmentManager());

        adapter.addFragment(new CallFragment(), "");
        adapter.addFragment(new ContactFragment(), "");
        adapter.addFragment(new SettingsFragment(), "");

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_call);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_contact);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_settings);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setElevation(0);


        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.USE_SIP);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            Log.d("APP_DEBUG", "onCreate: no permission given");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_SIP}, REQUEST_SIP);
        } else {
            //TODO
            make();
//            connectToPusher();
        }


    }

    private void make() {
//        ToggleButton pushToTalkButton = (ToggleButton) findViewById(R.id.pushToTalk);
//        pushToTalkButton.setOnTouchListener(this);

        // Set up the intent filter.  This will be used to fire an
        // IncomingCallReceiver when someone calls the SIP address used by this
        // application.

        Log.d(TAG, "make: function is called");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);

        // "Push to talk" can be a serious pain when the screen keeps turning off.
        // Let's prevent that.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeManager();
    }

    public void initializeManager() {
        if (manager == null) {
            manager = SipManager.newInstance(this);
            Log.d(TAG, "initializeManager: manager initialiased");
        }

        initializeLocalProfile();
    }


    /**
     * Logs you into your SIP provider, registering this device as the location to
     * send SIP calls to for your SIP address.
     */
    public void initializeLocalProfile() {
        if (manager == null) {
            return;
        }

        if (me != null) {
            closeLocalProfile();
        }

//        String username = ((App)getApplication()).getPrefManager().getUSER_Phone();
        String username = "3006";
//        Log.d(TAG, "initializeLocalProfile: username "+username);
        String domain = "192.168.43.229";
//        String username="3006";
//        String password="aaaa";
//        String password = ((App)getApplication()).getPrefManager().getUSER_Password();
//        Log.d(TAG, "initializeLocalProfile: password "+password);
//        if (username.length() == 0 || domain.length() == 0 || password.length() == 0) {
//            showDialog(UPDATE_SETTINGS_DIALOG);
//            return;
//        }

        String password = "aaaa";

        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            me = builder.build();

            Log.d(TAG, "initializeLocalProfile: profile built");
            Intent i = new Intent();
            i.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            manager.open(me, pi, null);


            // This listener must be added AFTER manager.open is called,
            // Otherwise the methods aren't guaranteed to fire.

            manager.setRegistrationListener(me.getUriString(), new SipRegistrationListener() {
                public void onRegistering(String localProfileUri) {
                    Log.d("APP_DEBUG", "onRegistering: ");
                    updateStatus("Registering with SIP Server...");
                }

                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    Log.d("APP_DEBUG", "onRegistrationDone: ");
                    updateStatus("Ready");
                }

                public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                 String errorMessage) {
                    Log.d("APP_DEBUG", "onRegistrationFailed: " + errorMessage + " " + errorCode);
                    updateStatus("Registration failed.  Please check settings.");
                }
            });
        } catch (ParseException pe) {
            updateStatus("Connection Error.");
        } catch (SipException se) {
            updateStatus("Connection error.");
        }
    }


    /**
     * Closes out your local profile, freeing associated objects into memory
     * and unregistering your device from the server.
     */
    public void closeLocalProfile() {
        if (manager == null) {
            return;
        }
        try {
            if (me != null) {
                manager.close(me.getUriString());
            }
        } catch (Exception ee) {
            Log.d(TAG, "Failed to close local profile.", ee);
        }
    }


    /**
     * Updates the status box at the top of the UI with a messege of your choice.
     *
     * @param status The String to display in the status box.
     */
    public void updateStatus(final String status) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        this.runOnUiThread(new Runnable() {
            public void run() {

                Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_base), status, Snackbar.LENGTH_LONG);

                snackbar.show();
            }
        });

        Log.d(TAG, "updateStatus to: " + status);
    }


    private SipAudioCall incCall = null;

    public void incomingCall(SipAudioCall c) {
        if (c == null) {
            return;
        }
        if (c.isInCall()) {
            return;
        }
        if (incCall != null) {
            return;
        }
        incCall = c;

        SipProfile caller = incCall.getPeerProfile();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Incoming Call from")
                .setMessage(caller.getUriString())
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try {
                            incCall.answerCall(30);
                            incCall.startAudio();
                            incCall.setSpeakerMode(true);

                            if (incCall.isMuted()) {
                                Log.d(TAG, "call was muted ");
                                incCall.toggleMute();
                            }
                        } catch (SipException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            incCall.endCall();
                        } catch (SipException e) {
                            e.printStackTrace();
                        }
                        incCall.close();
                        incCall = null;
                    }
                });
        builder.show();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_SIP: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    Log.d("APP_DEBUG", "onRequestPermissionsResult: permission granted");
                    make();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    /**
     * Updates the status box with the SIP address of the current call.
     *
     * @param call The current, active call.
     */
    public void updateStatus(SipAudioCall call) {
        String useName = call.getPeerProfile().getDisplayName();
        if (useName == null) {
            useName = call.getPeerProfile().getUserName();
        }
        updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
    }


    /**
     * Make an outgoing call.
     */
    public void initiateCall() {

        updateStatus(sipAddress);

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
                @Override
                public void onCallEstablished(SipAudioCall call) {
                    call.startAudio();
                    call.setSpeakerMode(true);
                    if (call.isMuted()) {
                        Log.d(TAG, "onCallEstablished: call was muted");
                        call.toggleMute();
                    }
                    updateStatus(call);
                    Log.d("APP_DEBUG", "onCallEstablished: ");

                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    updateStatus("Ready.");
                    Log.d("APP_DEBUG", "onCallEnded: ");
                }

                @Override
                public void onError(SipAudioCall call, int errorCode, String errorMessage) {
                    super.onError(call, errorCode, errorMessage);
                    Log.d(TAG, "onError: " + errorMessage + " code " + errorCode);
                }

                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    super.onRinging(call, caller);
                    Log.d(TAG, "onRinging: ringing ");
                }

                @Override
                public void onCalling(SipAudioCall call) {
                    super.onCalling(call);
                    Log.d(TAG, "onCalling: ");

                }

                @Override
                public void onCallBusy(SipAudioCall call) {
                    super.onCallBusy(call);
                    Log.d(TAG, "onCallBusy: ");
                }

                @Override
                public void onReadyToCall(SipAudioCall call) {
                    super.onReadyToCall(call);
                    Log.d(TAG, "onReadyToCall: ");
                }

                @Override
                public void onCallHeld(SipAudioCall call) {
                    super.onCallHeld(call);
                    Log.d(TAG, "onCallHeld: ");
                }

                @Override
                public void onChanged(SipAudioCall call) {
                    super.onChanged(call);
                    Log.d(TAG, "onChanged: ");
                }

                @Override
                public void onRingingBack(SipAudioCall call) {
                    super.onRingingBack(call);
                    Log.d(TAG, "onRingingBack: ");
                }

            };

            call = manager.makeAudioCall(me.getUriString(), sipAddress, listener, 30);

        } catch (Exception e) {
            Log.i(TAG, "Error when trying to close manager.", e);
            if (me != null) {
                try {
                    manager.close(me.getUriString());
                } catch (Exception ee) {
                    Log.i(TAG,
                            "Error when trying to close manager.", ee);
                    ee.printStackTrace();
                }
            }
            if (call != null) {
                call.close();
            }
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // When we get back from the preference setting Activity, assume
        // settings have changed, and re-login with new auth info.


        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.USE_SIP);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            Log.d("APP_DEBUG", "onCreate: no permission given");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_SIP}, REQUEST_SIP);
        } else {
            //TODO
            initializeManager();
//            make();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (call != null) {
            call.close();
        }

        closeLocalProfile();

        if (callReceiver != null) {
            this.unregisterReceiver(callReceiver);
        }
    }


}
