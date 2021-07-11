package com.lewiswilson.kiminojisho.flashcards

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lewiswilson.kiminojisho.DatabaseHelper
import com.lewiswilson.kiminojisho.MyListItem
import com.lewiswilson.kiminojisho.R
import kotlinx.android.synthetic.main.flashcards_home.*
import java.util.ArrayList

class Flashcards : AppCompatActivity() {

    private var myDB: DatabaseHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcards)
        myDB = DatabaseHelper(this)

        val flashcardList: ArrayList<MyListItem>? = myDB!!.dueFlashcards()

        if (flashcardList != null) {
            for (fc in flashcardList) {
                Log.d(TAG, "${fc.kanji} ${fc.kana} ${fc.english}")
            }
        }


    }
}