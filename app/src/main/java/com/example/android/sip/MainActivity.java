package com.example.android.sip;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {


    private static final int MODIFY_AUDIO_SEETINGS =10 ;
    @BindView(R.id.etEmail)
    EditText etEmail;

    @BindView(R.id.etPassword)
    EditText etPassword;

    @BindView(R.id.tvError)
    TextView tvError;

    @BindView(R.id.tvRegsiter)
    TextView tvRegister;

    @BindView(R.id.btnLogin)
    Button btnLogin;


    PusherOptions options;

    //    Pusher pusher;
    private Pusher pusher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        tvError.setVisibility(View.INVISIBLE);

        if(!(SipManager.isVoipSupported(getApplicationContext()) && SipManager.isApiSupported(getApplicationContext()))){
            tvError.setText("Your phone does not support SIP");
            tvError.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            tvRegister.setEnabled(false);

        }else {

            ((App) getApplication()).getPrefManager().setIsLoggedIn(true);
            if (((App) getApplication()).getPrefManager().isLoggedIn()) {




                int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS);

                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

                    Log.d("APP_DEBUG", "onCreate: no permission given");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_SIP}, MODIFY_AUDIO_SEETINGS);
                } else {
                    //TODO
//
//                //check if session is valid and if valid
                Intent intent = new Intent(MainActivity.this, WalkieTalkieActivity.class);
                startActivity(intent);
                finish();
                }




                //else login screen

            }
        }



    }


    @OnClick(R.id.tvRegsiter)
    public void signUpClicked() {

        Intent intent = new Intent(MainActivity.this, SignupActivity.class);
        startActivity(intent);
        finish();


    }

    @OnClick(R.id.btnLogin)
    public void loginClicked(View view) {
        RestApi restApi = RetrofitClient.getClient().create(RestApi.class);
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        UserCredentials user = new UserCredentials(email, password);
        Call<ApiToken> call = restApi.login(user);
        call.enqueue(new Callback<ApiToken>() {
            @Override
            public void onResponse(Call<ApiToken> call, Response<ApiToken> response) {
//

                Log.d("APP_DEBUG", "RESPONSE IS " + response.code());
                if (response.code()!=200) {
                    tvError.setVisibility(View.VISIBLE);
//                    Toast.makeText(getApplicationContext(),response.message(),Toast.LENGTH_SHORT).show();
                    return;
                } else {

                    ((App) getApplication()).getPrefManager().setIsLoggedIn(true);
                    ((App) getApplication()).getPrefManager().setUserAccessToken(response.body().getApi_token());
                    ((App) getApplication()).getPrefManager().setUSER_Phone(response.body().getPhone());
                    Intent intent = new Intent(MainActivity.this, WalkieTalkieActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiToken> call, Throwable t) {

                Log.d("APP_DEBUG", "ERROR IS " + t.getMessage());

                tvError.setVisibility(View.VISIBLE);
//                tvError.setText(t.getMessage());

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MODIFY_AUDIO_SEETINGS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    Log.d("APP_DEBUG", "onRequestPermissionsResult: permission granted");
//                    make();

                    Intent intent = new Intent(MainActivity.this, WalkieTalkieActivity.class);
                    startActivity(intent);
                    finish();
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


}


