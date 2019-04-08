/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sip;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.*;
import android.net.sip.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PresenceChannel;
import com.pusher.client.channel.PresenceChannelEventListener;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.channel.User;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.HttpAuthorizer;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles all calling, receiving calls, and UI interaction in the WalkieTalkie app.
 */
public class WalkieTalkieActivity extends Activity implements View.OnTouchListener {

    private static final int REQUEST_SIP = 10;
    private static final int USERS_ONLINE =5 ;
    public static String sipAddress = null;

    private static final String TAG = "APP_DEBUG";
    public SipManager manager = null;
    public SipProfile me = null;
    public SipAudioCall call = null;
    public IncomingCallReceiver callReceiver;

    private static final int CALL_ADDRESS = 1;
    private static final int LOGOUT = 2;
    private static final int UPDATE_SETTINGS_DIALOG = 3;
    private static final int HANG_UP = 4;


    private ArrayList<Contact> contacts = new ArrayList<>();

    private RecyclerViewAdapter recyclerViewAdapter;

    private RecyclerView recyclerView;

    private  Pusher pusher;



    private void createRecyclerView(){
        contacts.add(new Contact("Server","j.veg.lv",200));

        recyclerView = findViewById(R.id.rvView);
        recyclerViewAdapter = new RecyclerViewAdapter(this, contacts);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }


    private void updateContacts(final Contact c) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                contacts.add(c);
                recyclerViewAdapter.notifyDataSetChanged();
                recyclerViewAdapter.setContactListFull(contacts);
            }
        });
    }

    private void removeContacts(final Contact c) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                int i=0;

                for (Contact p:contacts) {
                    if(p.getEmail().equals(c.getEmail())){
                        contacts.remove(i);
                        recyclerViewAdapter.notifyDataSetChanged();
                        recyclerViewAdapter.setContactListFull(contacts);
                        Log.d(TAG, "run: "+"removed");
                        break;
                    }
                    i++;
                }

            }
        });
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.walkietalkie);
        createRecyclerView();

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.USE_SIP);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            Log.d("APP_DEBUG", "onCreate: no permission given");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_SIP}, REQUEST_SIP);
        } else {
            //TODO
            make();
            connectToPusher();
        }


    }

    private void make() {
//        ToggleButton pushToTalkButton = (ToggleButton) findViewById(R.id.pushToTalk);
//        pushToTalkButton.setOnTouchListener(this);

        // Set up the intent filter.  This will be used to fire an
        // IncomingCallReceiver when someone calls the SIP address used by this
        // application.
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);

        // "Push to talk" can be a serious pain when the screen keeps turning off.
        // Let's prevent that.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeManager();
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

    public void initializeManager() {
        if(manager == null) {
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//        String username = prefs.getString("namePref", "");
//        String domain = prefs.getString("domainPref", "");
//        String password = prefs.getString("passPref", "");

         String username = "3006";

            String domain = "j.veg.lv";

        String password = "aaaa";


        if (username.length() == 0 || domain.length() == 0 || password.length() == 0) {
            showDialog(UPDATE_SETTINGS_DIALOG);
            return;
        }

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
                        Log.d("APP_DEBUG", "onRegistrationFailed: "+errorMessage+" "+errorCode);
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
                    if(call.isMuted()){
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
                    Log.d(TAG, "onError: "+errorMessage+" code "+errorCode);
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

        }
        catch (Exception e) {
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

    /**
     * Updates the status box at the top of the UI with a messege of your choice.
     * @param status The String to display in the status box.
     */
    public void updateStatus(final String status) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView labelView = (TextView) findViewById(R.id.sipLabel);
                labelView.setText(status);
//                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),status,Snackbar.LENGTH_SHORT);
//                snackbar.show();

//                Toast.makeText(getApplicationContext(),status,Toast.LENGTH_SHORT).show();
            }
        });

        Log.d(TAG, "updateStatus to: "+status);
    }



    /**
     * Updates the status box with the SIP address of the current call.
     * @param call The current, active call.
     */
    public void updateStatus(SipAudioCall call) {
        String useName = call.getPeerProfile().getDisplayName();
        if(useName == null) {
          useName = call.getPeerProfile().getUserName();
        }
        updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
    }

    /**
     * Updates whether or not the user's voice is muted, depending on whether the button is pressed.
     * @param v The View where the touch event is being fired.
     * @param event The motion to act on.
     * @return boolean Returns false to indicate that the parent view should handle the touch event
     * as it normally would.
     */
    public boolean onTouch(View v, MotionEvent event) {
        if (call == null) {
            return false;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN && call != null && call.isMuted()) {
            call.toggleMute();
        } else if (event.getAction() == MotionEvent.ACTION_UP && !call.isMuted()) {
            call.toggleMute();
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CALL_ADDRESS, 0, "Call someone");
        menu.add(0, USERS_ONLINE, 0, "Users online.");
        menu.add(0, HANG_UP, 0, "End Current Call.");
        menu.add(0, LOGOUT, 0, "Logout");

//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.search_menu, menu);
//        MenuItem searchItem = menu.findItem(R.id.action_search);
//
//        SearchView searchView = (SearchView) searchItem.getActionView();
//        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
//
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String s) {
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String s) {
//
//                recyclerViewAdapter.getFilter().filter(s);
//                return true;
//            }
//        });
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CALL_ADDRESS:
                showDialog(CALL_ADDRESS);
                break;
            case LOGOUT:
                logout();
                break;
            case HANG_UP:
                if(call != null) {
                    try {
                      call.endCall();
                    } catch (SipException se) {
                        Log.d(TAG,
                                "Error ending call.", se);
                    }
                    call.close();
                };
                break;
            case USERS_ONLINE:
                Intent intent=new Intent(WalkieTalkieActivity.this,ContactActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void logout() {
        RestApi restApi = RetrofitClient.getClient().create(RestApi.class);

        Call<Void> call = restApi.logout();
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
//

                Log.d("APP_DEBUG", "RESPONSE IS " + response.code());
                if (response.code()!=200) {
                    Toast.makeText(getApplicationContext(),response.message(),Toast.LENGTH_SHORT).show();
                    return;
                } else {

                    ((App) getApplication()).getPrefManager().setIsLoggedIn(false);
                    ((App) getApplication()).getPrefManager().setUserAccessToken("");
                    ((App) getApplication()).getPrefManager().setUSER_Phone(200);
                    ((App) getApplication()).getPrefManager().setUserEmail("");
                    ((App) getApplication()).getPrefManager().setUserName("");
                    Intent intent = new Intent(WalkieTalkieActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

                Log.d("APP_DEBUG", "ERROR IS " + t.getMessage());
                Toast.makeText(getApplicationContext(),t.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CALL_ADDRESS:

                LayoutInflater factory = LayoutInflater.from(this);
                final View textBoxView = factory.inflate(R.layout.call_address_dialog, null);
                return new AlertDialog.Builder(this)
                        .setTitle("Call Someone.")
                        .setView(textBoxView)
                        .setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        EditText textField = (EditText)
                                                (textBoxView.findViewById(R.id.calladdress_edit));
                                        sipAddress = textField.getText().toString();
                                        initiateCall();

                                    }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();

            case UPDATE_SETTINGS_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage("Please update your SIP Account Settings.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                updatePreferences();
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();
        }
        return null;
    }

    public void updatePreferences() {
        Intent settingsActivity = new Intent(getBaseContext(),
                SipSettings.class);
        startActivity(settingsActivity);
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


    private void connectToPusher(){
        Log.d(TAG, "connectToPusher: starting");
        PusherOptions options = new PusherOptions();
        options.setHost(App.ip);
        options.setWsPort(6001);
        options.setEncrypted(false);
        options.buildUrl("ABCDEFG");


        HttpAuthorizer authorizer = new HttpAuthorizer(App.channelAuth);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", ((App) getApplication()).getPrefManager().getUserAccessToken());
        authorizer.setHeaders(headers);
        options.setAuthorizer(authorizer);

        pusher = new Pusher("ABCDEFG", options);
        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange connectionStateChange) {
                Log.d(TAG, "Changed to " + connectionStateChange.getCurrentState());
            }

            @Override
            public void onError(String s, String s1, Exception e) {

                Log.d(TAG, "Failed " + s + " " + s1);
            }
        });

        Channel channel = pusher.subscribe("ch");

        channel.bind("App\\Events\\ContactCreated", new SubscriptionEventListener() {
            @Override
            public void onEvent(String s, String s1, String s2) {
                Log.d("APP_DEBUG", "Event " + s + " " + s2);
            }
        });

        PresenceChannel presenceChannel = pusher.subscribePresence("presence-chat", new PresenceChannelEventListener() {
            @Override
            public void onUsersInformationReceived(String s, Set<com.pusher.client.channel.User> set) {
                Log.d(TAG, "onUsersInformationReceived: ");

                contacts.clear();
                contacts.add(new Contact("demo","demo@j.veg.lv",200));
                for (User u:set) {
                    Gson g = new Gson();
                    Contact p = g.fromJson(u.getInfo(), Contact.class);
                    Log.d(TAG, "userSubscribed: " + p.getPhone());
                    Log.d(TAG, "userSubscribed: " + p.getName());
                        contacts.add(p);
                }

                recyclerViewAdapter.notifyDataSetChanged();
                recyclerViewAdapter.setContactListFull(contacts);
            }

            @Override
            public void userSubscribed(String s, com.pusher.client.channel.User user) {
                Log.d("APP_DEBUG_SUBSCRIBED", s);
                Log.d(TAG, "userSubscribed: " + user.getInfo());
                Gson g = new Gson();
                Contact p = g.fromJson(user.getInfo(), Contact.class);
                Log.d(TAG, "userSubscribed: " + p.getPhone());
                Log.d(TAG, "userSubscribed: " + p.getName());
//                        contacts.add(p);
//                        recyclerViewAdapter.notifyDataSetChanged();
//                        recyclerViewAdapter.setContactListFull(contacts);
                updateContacts(p);

            }

            @Override
            public void userUnsubscribed(String s, User user) {
                Log.d("APP_DEBUG_UNSUBSCRIBER", s);
                Log.d(TAG, "userUnsubscribed: ");
                Gson g = new Gson();
                Contact p = g.fromJson(user.getInfo(), Contact.class);
                removeContacts(p);
                Log.d(TAG, "userUnsubscribed: " + p.getPhone());
                Log.d(TAG, "userUnsubscribed: " + p.getName());
            }

            @Override
            public void onAuthenticationFailure(String s, Exception e) {
                Log.d("APP_DEBUG_AUTH_FAIL", s);
                Log.d(TAG, "onAuthenticationFailure: " + e.getMessage());

            }

            @Override
            public void onSubscriptionSucceeded(String s) {
                Log.d("APP_DEBUG_SUB_SUCCESS", s);
            }

            @Override
            public void onEvent(String s, String s1, String s2) {

            }
        });
    }

    private SipAudioCall incCall=null;

    public void incomingCall(SipAudioCall c){
        if(c==null){
            return;
        }
        if(c.isInCall()){
            return;
        }
        if(incCall!=null){
            return;
        }
        incCall=c;

        SipProfile caller=incCall.getPeerProfile();

        AlertDialog.Builder builder=new AlertDialog.Builder(this);

        builder.setTitle("Incoming Call from")
                .setMessage(caller.getUriString())
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try {
                            incCall.answerCall(30);
                            incCall.startAudio();
                            incCall.setSpeakerMode(true);

                            if(incCall.isMuted()){
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
                        incCall=null;
                    }
                });
        builder.show();

    }

}
