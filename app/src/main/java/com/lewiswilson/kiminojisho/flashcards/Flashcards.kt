package com.lewiswilson.kiminojisho.flashcards

import android.animation.ObjectAnimator
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lewiswilson.kiminojisho.DatabaseHelper
import com.lewiswilson.kiminojisho.mylists.MyListItem
import com.lewiswilson.kiminojisho.R
import kotlinx.android.synthetic.main.flashcards_home.*
import kotlinx.android.synthetic.main.flashcard_back.*
import kotlinx.android.synthetic.main.flashcard_front.*
import kotlinx.android.synthetic.main.flashcards.*
import kotlinx.android.synthetic.main.flashcards_complete.*
import java.util.ArrayList

class Flashcards : AppCompatActivity() {

    private var myDB: DatabaseHelper? = null
    var flashcardList: ArrayList<MyListItem>? = null
    var seen: ArrayList<Int>? = ArrayList()
    private var completeReviews: Int = 0
    private var totalReviews: Int = 0
    private var totalCorrect: Int = 0
    private var totalTries: Int = 0
    private var correctBtn: Int = 0
    private var selectedList = 0
    private var wrongColor: Int = 0
    private var correctColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcards)
        wrongColor = getColor(R.color.flashcard_wrong)
        correctColor = getColor(R.color.flashcard_correct)
        flipview.flipDuration = 200 //ms
        myDB = DatabaseHelper(this)

        selectedList = intent.getIntExtra("listID", 0)

        flashcardList = myDB!!.dueFlashcards(selectedList)
        totalReviews = flashcardList?.size!!
        progressBar.progress = 0

        flashcardSort()

        option1.setOnClickListener{ redirect(1) }
        option2.setOnClickListener{ redirect(2) }
        option3.setOnClickListener{ redirect(3) }
        option4.setOnClickListener{ redirect(4) }

        fc_btn_continue.setOnClickListener {
            flipview.flipTheView()
            flipview.isFlipEnabled = false
            fc_btn_continue.visibility = View.GONE
            answer_grid.visibility = View.VISIBLE
            flashcardSort()
        }

    }

    private fun redirect(selectedBtn: Int) {
        var correct = false

        //if the button selected is the correct value, set the boolean to true
        when(selectedBtn){correctBtn -> correct = true}

        option1.isEnabled = false
        option2.isEnabled = false
        option3.isEnabled = false
        option4.isEnabled = false

        totalTries += 1

        val dbid = flashcardList!!.first().id

        if(correct) {
            totalCorrect += 1
            //if the seen arraylist contains current word
            if( seen?.isNotEmpty() == true && seen?.contains(flashcardList?.first()?.id) == true ) {
                Log.d(TAG, "${flashcardList?.first()?.kanji} correct, seen")
                myDB?.updateFlashcard(dbid, correct, true)
            } else {
                Log.d(TAG, "${flashcardList?.first()?.kanji} correct, NOT seen")
                myDB?.updateFlashcard(dbid, correct, false)
            }

            cv_back.setCardBackgroundColor(correctColor)
            cv_front.setCardBackgroundColor(correctColor)

            flashcardList?.removeAt(0)
            completeReviews++
            val progress = (completeReviews/totalReviews.toDouble()*100).toInt()
            setProgressBar(progress)

        } else {
            //if the seen arraylist contains the current word
            if( seen?.isNotEmpty() == true && seen?.contains(flashcardList?.first()?.id) == true ) {

                Log.d(TAG, "${flashcardList?.first()?.kanji} wrong, seen")
                //database id, correct = false, seen = true
                myDB?.updateFlashcard(dbid, correct, false)

            } else {
                seen?.add(dbid)
                Log.d(TAG, "${flashcardList?.first()?.kanji} wrong, NOT seen")
                myDB?.updateFlashcard(dbid, correct, true)
            }

            cv_back.setCardBackgroundColor(wrongColor)
            cv_front.setCardBackgroundColor(wrongColor)

        }



         //delay running by extra 200ms so that answer doesnt show on flip back
        Handler(Looper.getMainLooper()).postDelayed({
            cv_back.setCardBackgroundColor(getColor(R.color.white))
            cv_front.setCardBackgroundColor(getColor(R.color.white))
            if (correct) {
                if (flashcardList?.isEmpty() == true) {
                    //finished
                    val percentCorrect = totalCorrect.toDouble() / totalTries * 100
                    finish()
                    val intent = Intent(this@Flashcards, FlashcardsComplete::class.java)
                    intent.putExtra("percent", percentCorrect)
                    startActivity(intent)
                } else {
                    flashcardSort()
                }
            } else {
                flipview.isFlipEnabled = true
                flipview.flipTheView(true)
                answer_grid.visibility = View.GONE
                fc_btn_continue.visibility = View.VISIBLE
            }
            option1.isEnabled = true
            option2.isEnabled = true
            option3.isEnabled = true
            option4.isEnabled = true
        }, 1200)
    }

    private fun flashcardSort() {

        //randomise the order of the reviews
        flashcardList?.shuffle()

        //populating flashcard
        fc_front_japanese.text = flashcardList?.first()?.kanji
        fc_back_japanese.text = flashcardList?.first()?.kanji
        fc_back_kana.text = flashcardList?.first()?.kana


        val validateArray = listOf(1, 0, 0, 0).shuffled()

        for (item in validateArray){
            if(item==1){
                correctBtn = validateArray.indexOf(item) + 1 //assigns the correct button
                Log.d(TAG, "correct item: $item , index: ${validateArray.indexOf(item) + 1}") //correct
            }
        }

        val incorrectItems = myDB?.randomThreeWrong(flashcardList?.first()?.kanji!!, selectedList)

        var correctEnglish = ""
        val correctItemText: String
        val wrongItemText1: String
        val wrongItemText2: String
        val wrongItemText3: String

        // if entry has no kanji
        if(flashcardList?.first()?.kana.equals(flashcardList?.first()?.kanji)){
            correctItemText = randomDefinition(flashcardList?.first()?.english.toString())
            wrongItemText1 = randomDefinition(incorrectItems?.elementAt(0)?.english.toString())
            wrongItemText2 = randomDefinition(incorrectItems?.elementAt(1)?.english.toString())
            wrongItemText3 = randomDefinition(incorrectItems?.elementAt(2)?.english.toString())
        } else {
            correctEnglish = randomDefinition(flashcardList?.first()?.english.toString())
            correctItemText = "${flashcardList?.first()?.kana}\n" + correctEnglish

            wrongItemText1 = "${incorrectItems?.elementAt(0)?.kana}\n" +
                    randomDefinition(incorrectItems?.elementAt(0)?.english.toString())
            wrongItemText2 = "${incorrectItems?.elementAt(1)?.kana}\n" +
                    randomDefinition(incorrectItems?.elementAt(1)?.english.toString())
            wrongItemText3 = "${incorrectItems?.elementAt(2)?.kana}\n" +
                    randomDefinition(incorrectItems?.elementAt(2)?.english.toString())
        }

        Log.d(TAG, "wrongitemtext1: $wrongItemText1")
        Log.d(TAG, "wrongitemtext2: $wrongItemText2")
        Log.d(TAG, "wrongitemtext3: $wrongItemText3")

        //caters for the random definition chosen
        fc_back_english.text = correctEnglish

        when (correctBtn) {
            1 -> {
                txt_option1.text = correctItemText
                txt_option2.text = wrongItemText1
                txt_option3.text = wrongItemText2
                txt_option4.text = wrongItemText3
            }
            2 -> {
                txt_option1.text = wrongItemText1
                txt_option2.text = correctItemText
                txt_option3.text = wrongItemText2
                txt_option4.text = wrongItemText3
            }
            3 -> {
                txt_option1.text = wrongItemText1
                txt_option2.text = wrongItemText2
                txt_option3.text = correctItemText
                txt_option4.text = wrongItemText3
            }
            4 -> {
                txt_option1.text = wrongItemText1
                txt_option2.text = wrongItemText2
                txt_option3.text = wrongItemText3
                txt_option4.text = correctItemText
            }
        }

    }

    private fun randomDefinition(unformatted: String) = unformatted.split("@@@").random()

    private fun setProgressBar(progress: Int) {
        ObjectAnimator.ofInt(progressBar, "progress", progress)
            .setDuration(300)
            .start()
    }

}