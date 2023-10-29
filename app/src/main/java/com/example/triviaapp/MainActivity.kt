package com.example.triviaapp

import android.annotation.SuppressLint
import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.text.HtmlCompat
import com.example.triviaapp.databinding.ActivityMainBinding
import com.example.triviaapp.data.Result
import com.example.triviaapp.data.OpentdbApi
import com.example.triviaapp.data.Token
import com.example.triviaapp.data.Question
import com.example.triviaapp.database.DBQuestion
import com.example.triviaapp.database.DatabaseHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var questionList: List<Result>
    private lateinit var downloadedQuestions: ArrayList<DBQuestion>
    private lateinit var shuffledQuestions: ArrayList<DBQuestion>
    private var online = true
    private var answerStreak = 0

    //Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://opentdb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val api = retrofit.create(OpentdbApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getSettings()

        binding.retryButton.setOnClickListener { fetchQuestions() }
        binding.trueButton.setOnClickListener { checkAnswer(true) }
        binding.falseButton.setOnClickListener { checkAnswer(false) }

        binding.onlineSwitch.setOnClickListener{
            if (binding.onlineSwitch.isChecked) {
                binding.onlineSwitch.text = "Online"
                online = true
                updateSettings("online", "true")
                fetchQuestions()
            }
            else {
                binding.onlineSwitch.text = "Offline"
                online = false
                updateSettings("online", "false")
                fetchQuestions()
            }
        }

        binding.refreshButton.setOnClickListener{
            deleteQuestions()
        }

        downloadedQuestions = ArrayList()
        readQuestions()
    }

    private fun deleteQuestions() {
        val db = DatabaseHelper(this).writableDatabase
        db.delete("questions", null, null)
        db.close()
        downloadedQuestions.clear()
        Toast.makeText(applicationContext, "Old local questions cleared", Toast.LENGTH_LONG).show()
        fetchQuestions()
    }

    @SuppressLint("Range")
    fun readQuestions() {
        downloadedQuestions.clear()

        val db = DatabaseHelper(this).readableDatabase
        val cursor = db.rawQuery("SELECT * FROM questions", null)
        if (cursor.count != 0) {
            //Toast.makeText(applicationContext, "Questions found in db", Toast.LENGTH_LONG).show()
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val question = cursor.getString(cursor.getColumnIndex("question"))
                        val answer = cursor.getString(cursor.getColumnIndex("answer"))
                        downloadedQuestions.add(DBQuestion(question, answer))
                        Log.d("DatabaseInfo", "Question: $question, Answer: $answer")
                    } while (cursor.moveToNext())
                }
                cursor.close()
                db.close()

                fetchQuestions()
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext, "DB Read error",
                    Toast.LENGTH_SHORT
                ).show()
                e.localizedMessage?.let { Log.d("DB Read exception", it) }
            }
        } else {
            //No local questions found, get from api
            cursor.close()
            downloadQuestions()
        }
    }

    @SuppressLint("Range")
    private fun getSettings() {
        val db = DatabaseHelper(this).readableDatabase
        val cursor = db.rawQuery("SELECT * FROM settings", null)
        if (cursor.count != 0) {
            //Toast.makeText(applicationContext, "Settings found in db", Toast.LENGTH_LONG).show()
            try {
                if (cursor.moveToFirst()) {
                    do {
                        val setting = cursor.getString(cursor.getColumnIndex("setting"))
                        val value = cursor.getString(cursor.getColumnIndex("value"))
                        //Update online status
                        online = value.equals("true")
                        if (online) {
                            binding.onlineSwitch.text = "Online"
                        } else {
                            binding.onlineSwitch.text = "Offline"
                        }
                        binding.onlineSwitch.isChecked = online
                        Log.d("DatabaseInfo", "Setting: $setting, Value: $value")
                    } while (cursor.moveToNext())
                }
                cursor.close()
                db.close()
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext, "DB Read error",
                    Toast.LENGTH_SHORT
                ).show()
                e.localizedMessage?.let { Log.d("DB Read exception", it) }
            }
        } else {
            Toast.makeText(applicationContext, "No settings found in db", Toast.LENGTH_LONG).show()
            cursor.close()
        }
    }

    private fun updateSettings(setting: String, value: String) {
        val db = DatabaseHelper(this).writableDatabase
        val contentValues = ContentValues()
        contentValues.put("setting", setting)
        contentValues.put("value", value)

        val success = db.update("settings", contentValues, "setting=?", arrayOf(setting))
        if (success != -1) {
//            Toast.makeText(
//                applicationContext, "Question saved",
//                Toast.LENGTH_SHORT
//            ).show()
            Log.d("Setting Updated", setting)
        } else {
            Toast.makeText(
                applicationContext, "Unable to update setting",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("DB Error", "Unable to update setting")
        }
        db.close()
    }

    fun addQuestion(question: String, answer: String) {
        val db = DatabaseHelper(this).writableDatabase
        val contentValues = ContentValues()
        contentValues.put("question", question)
        contentValues.put("answer", answer)

        val success = db.insert("questions", null, contentValues)
        if (success != -1L) {
//            Toast.makeText(
//                applicationContext, "Question saved",
//                Toast.LENGTH_SHORT
//            ).show()
            Log.d("Question Inserted", question)
        } else {
            Toast.makeText(
                applicationContext, "Unable to save question",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("DB Error", "Unable to save question")
        }
        db.close()
    }

    private fun downloadQuestions() {
        Toast.makeText(applicationContext, "Downloading questions", Toast.LENGTH_LONG).show()

        api.getToken().enqueue(object : Callback<Token> {
            override fun onResponse(call: Call<Token>, response: Response<Token>) {
                val tokenData = response.body()
                val token = "" + tokenData?.token

                api.downloadQuestions(token).enqueue(object : Callback<Question> {

                    override fun onResponse(call: Call<Question>, response: Response<Question>) {
                        val res = response.body()!!.resultsList
                        for (i in res) {
                            downloadedQuestions.add(DBQuestion(i.question, i.correctAnswer))
                            addQuestion(i.question, i.correctAnswer)
                        }
                        fetchQuestions()
                    }

                    override fun onFailure(call: Call<Question>, t: Throwable) {
                        Log.e("Questions error", t.toString())
                        binding.questionView.text = "Couldn't download questions, try restarting the application or checking your internet connection."
                    }

                })
            }

            override fun onFailure(call: Call<Token>, t: Throwable) {
                Log.e("Token error", t.toString())
                binding.questionView.text = "Couldn't download questions, try restarting the application or checking your internet connection."
            }

        })
    }

    private fun fetchQuestions() {
        //Ensure answer streak is reset
        answerStreak = 0
        binding.streakView.text = "Streak: 0"
        binding.questionView.text = "Loading Questions..."

        if (!online) {
            Toast.makeText(applicationContext, "Getting downloaded questions", Toast.LENGTH_LONG).show()
            //Randomize order
            if (downloadedQuestions.isEmpty()) {
                //Local questions empty, download them again
                downloadQuestions()
            } else {
                shuffledQuestions = downloadedQuestions.shuffled() as ArrayList<DBQuestion>
                //Display first question
                val firstQuestion = HtmlCompat.fromHtml(shuffledQuestions[0].question, HtmlCompat.FROM_HTML_MODE_LEGACY)
                //Initialize UI
                binding.questionView.text = firstQuestion
                binding.retryButton.visibility = View.INVISIBLE
                binding.trueButton.visibility = View.VISIBLE
                binding.falseButton.visibility = View.VISIBLE
            }
        } else {
            Toast.makeText(applicationContext, "Getting online questions", Toast.LENGTH_LONG).show()
            api.getToken().enqueue(object : Callback<Token> {
                override fun onResponse(call: Call<Token>, response: Response<Token>) {
                    val tokenData = response.body()
                    val token = "" + tokenData?.token

                    //Call API with token for 10 unique true false questions
                    api.getQuestions(token).enqueue(object : Callback<Question> {
                        override fun onResponse(call: Call<Question>, response: Response<Question>) {
                            questionList = response.body()!!.resultsList
                            //Display first question
                            val firstQuestion = HtmlCompat.fromHtml(
                                questionList[0].question,
                                HtmlCompat.FROM_HTML_MODE_LEGACY
                            )
                            //Initialize UI
                            runOnUiThread {
                                binding.questionView.text = firstQuestion
                                binding.retryButton.visibility = View.INVISIBLE
                                binding.trueButton.visibility = View.VISIBLE
                                binding.falseButton.visibility = View.VISIBLE
                            }
                        }

                        override fun onFailure(call: Call<Question>, t: Throwable) {
                            Log.e("Questions error", t.toString())
                            binding.questionView.text = "Error fetching questions, try restarting the application or checking your internet connection."
                        }

                    })
                }

                override fun onFailure(call: Call<Token>, t: Throwable) {
                    Log.e("Token error", t.toString())
                    binding.questionView.text = "Error fetching questions, try restarting the application or checking your internet connection."
                }

            })
        }
    }

    private fun checkAnswer(answer: Boolean) {
        //Check if answer correct, then either increase streak or end game
        if (!online) {
            //Toast.makeText(applicationContext, "checking db questions", Toast.LENGTH_LONG).show()
            val currentQuestion = shuffledQuestions[answerStreak]
            if (answer.toString().equals(currentQuestion.answer, true)) {
                answerStreak++
                binding.streakView.text = "Streak: ${answerStreak}"
                if (answerStreak == 10) {
                    binding.questionView.text =
                        "Well done! You successfully answered ${answerStreak} questions."
                    binding.retryButton.visibility = View.VISIBLE
                    binding.trueButton.visibility = View.INVISIBLE
                    binding.falseButton.visibility = View.INVISIBLE
                } else {
                    binding.questionView.text = shuffledQuestions[answerStreak].question?.let {
                        HtmlCompat.fromHtml(
                            it,
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        )
                    }
                }
            } else {
                binding.questionView.text = "Game over. Your streak was ${answerStreak}."
                binding.retryButton.visibility = View.VISIBLE
                binding.trueButton.visibility = View.INVISIBLE
                binding.falseButton.visibility = View.INVISIBLE
            }
        } else {
            //Toast.makeText(applicationContext, "checking api questions", Toast.LENGTH_LONG).show()
            val currentQuestion = questionList[answerStreak]
            val correctAnswer = currentQuestion.correctAnswer == "True"
            if (answer == correctAnswer) {
                answerStreak++
                binding.streakView.text = "Streak: ${answerStreak}"
                if (answerStreak == 10) {
                    binding.questionView.text =
                        "Well done! You successfully answered ${answerStreak} questions."
                    binding.retryButton.visibility = View.VISIBLE
                    binding.trueButton.visibility = View.INVISIBLE
                    binding.falseButton.visibility = View.INVISIBLE
                } else {
                    binding.questionView.text = HtmlCompat.fromHtml(
                        questionList[answerStreak].question,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                }
            } else {
                binding.questionView.text = "Game over. Your streak was ${answerStreak}."
                binding.retryButton.visibility = View.VISIBLE
                binding.trueButton.visibility = View.INVISIBLE
                binding.falseButton.visibility = View.INVISIBLE
            }
        }
    }
}