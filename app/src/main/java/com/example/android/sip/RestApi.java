package com.example.android.sip;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RestApi {

//    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @POST("login")
    Call<User> login(@Body UserCredentials user_credentials);
    @POST("register")
    Call<User> register(@Body User user);
    @POST("logout")
    Call<Void> logout();

}
