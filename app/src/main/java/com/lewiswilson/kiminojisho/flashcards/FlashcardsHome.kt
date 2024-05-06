package com.lewiswilson.kiminojisho.flashcards

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.lewiswilson.kiminojisho.DatabaseHelper
import com.lewiswilson.kiminojisho.R
import com.lewiswilson.kiminojisho.databinding.FlashcardsHomeBinding
import com.lewiswilson.kiminojisho.mylists.MyList
import java.util.*

class FlashcardsHome : AppCompatActivity() {

    private lateinit var fcHomeBind: FlashcardsHomeBinding

    private var listOfLists: ArrayList<FCListItem>? = ArrayList()
    private var rvAdapter: FCListAdapter? = null
    private var myDB: DatabaseHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fcHomeBind = FlashcardsHomeBinding.inflate(layoutInflater)
        setContentView(fcHomeBind.root)
        myDB = DatabaseHelper(this)

        //implementing ads
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        val testDeviceIds = Arrays.asList("3EE9A91017FEA4E8E9F0996A4775B406")
        val config = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(config)
        fcHomeBind.fcHomeAdView.loadAd(adRequest)

        val reviewsDue = myDB?.flashcardCount(0)
        val noOfFlashcards = "Total Reviews: $reviewsDue"
        fcHomeBind.fcNo.text = noOfFlashcards

        if (reviewsDue!! <= 0) {
            fcHomeBind.btnStart.text = this.getString(R.string.Complete)
            fcHomeBind.btnStart.isEnabled = false
        }


        fcHomeBind.btnStart.setOnClickListener { _: View? ->
            if (myDB!!.itemCount(0) < 4) {
                Toast.makeText(this, "Not enough words to start reviewing! Add some new words to get started!", Toast.LENGTH_LONG).show()
            } else {
                finish()
                startActivity(Intent(this@FlashcardsHome, Flashcards::class.java))
            }
        }

        //initiate recyclerview and set parameters
        fcHomeBind.rvFlashcardsHome.setHasFixedSize(true)
        fcHomeBind.rvFlashcardsHome.layoutManager = GridLayoutManager(this, 2)

        populateRV()

        fcHomeBind.rvFlashcardsHome.adapter = rvAdapter

        rvAdapter?.setOnItemClickListener(object: FCListAdapter.OnItemClickListener{
            override fun onItemClick(listId: Int) {
                val intent = Intent(applicationContext, MyList::class.java)
                intent.putExtra("listID", listId)
                startActivity(intent)
            }

            override fun onButtonClick(listId: Int) {
                val intent = Intent(applicationContext, Flashcards::class.java)
                intent.putExtra("listID", listId)
                startActivity(intent)
            }
        }
        )
    }

    override fun onRestart() {
        super.onRestart()
        listOfLists?.clear()
        rvAdapter?.notifyDataSetChanged()
        populateRV()
    }

    //populate recyclerview with data
    private fun populateRV() {

        val listArray = myDB!!.getLists()

        for (list in listArray) {
            val listId = myDB?.getListIdFromName(list)
            val reviews = myDB?.flashcardCount(listId!!)
            listOfLists!!.add(FCListItem(list, reviews!!))
            rvAdapter = listOfLists?.let { it -> FCListAdapter(this@FlashcardsHome, it) }
        }
    }
}