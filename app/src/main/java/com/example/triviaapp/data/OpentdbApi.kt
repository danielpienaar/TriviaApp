package com.example.triviaapp.data

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface OpentdbApi {
    @GET("api.php?amount=20&type=boolean") // call for downloading 20 questions
    fun downloadQuestions(@Query("token") token: String): Call<Question>
    @GET("api.php?amount=10&type=boolean") // making get request at opentdb end-point
    fun getQuestions(@Query("token") token: String): Call<Question>
    @GET("api_token.php?command=request")
    fun getToken(): Call<Token>
}