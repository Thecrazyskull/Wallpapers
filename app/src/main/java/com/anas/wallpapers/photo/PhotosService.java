package com.anas.wallpapers.photo;

import com.anas.wallpapers.connection.AccessToken;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface PhotosService {
    @POST
    Call<AccessToken> getAccessToken(@Url String str, @Query("client_id") String str2, @Query("grant_type") String str3, @Query("scope") String str4, @Query("client_secret") String str5);

    @POST
    Call<PhotosData> getPhotos(@Url String str, @Query("schedule") String str2, @Query("queryType") String str3, @Query("count") String str4, @Query("timeZoneId") String str5);
}