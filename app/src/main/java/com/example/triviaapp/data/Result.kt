package com.example.triviaapp.data

import com.google.gson.annotations.SerializedName

data class Result(
    val category: String,
    val type: String,
    val difficulty: String,
    val question: String,
    @SerializedName("correct_answer")
    val correctAnswer: String,
    @SerializedName("incorrect_answers")
    val incorrectAnswers: List<Boolean>
)
