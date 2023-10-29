package com.example.triviaapp.data

import com.google.gson.annotations.SerializedName

data class Question(
    //https://blog.mindorks.com/using-retrofit-with-kotlin-coroutines-in-android/
    @SerializedName("response_code")
    val responseCode: Int,
    @SerializedName("results")
    val resultsList: List<Result>
)
