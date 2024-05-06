package com.lewiswilson.kiminojisho.flashcards

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lewiswilson.kiminojisho.R
import com.lewiswilson.kiminojisho.databinding.FlashcardsBinding
import com.lewiswilson.kiminojisho.databinding.FlashcardsCompleteBinding

class FlashcardsComplete : AppCompatActivity() {

    private lateinit var fcCompleteBind: FlashcardsCompleteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fcCompleteBind = FlashcardsCompleteBinding.inflate(layoutInflater)
        setContentView(fcCompleteBind.root)

        val percent = String.format("%.2f", intent.getDoubleExtra("percent", 0.0))
        val statsText = "Review Accuracy: ${percent}%"
        fcCompleteBind.txtStats.text = statsText

        fcCompleteBind.btnBackToReviews.setOnClickListener { _: View? ->
            finish()
            startActivity(Intent(this@FlashcardsComplete, FlashcardsHome::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

    }
}