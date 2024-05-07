package com.lewiswilson.kiminojisho.search

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.lewiswilson.kiminojisho.AddWord
import com.lewiswilson.kiminojisho.DatabaseHelper
import com.lewiswilson.kiminojisho.databinding.SearchPageBinding
import com.lewiswilson.kiminojisho.mylists.MyListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray

class SearchPage : AppCompatActivity(), CoroutineScope {

    private lateinit var searchPageBind: SearchPageBinding

    private var mSearchList: ArrayList<SearchDataItem>? = ArrayList()
    private var mSearchDataAdapter: SearchDataAdapter? = null
    private val myDB = DatabaseHelper(this)
    var queryTextChangedJob: Job? = null
    var currentJobText = ""

    private val job = Job()
    override val coroutineContext = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchPageBind = SearchPageBinding.inflate(layoutInflater)
        setContentView(searchPageBind.root)
        sp = this

        // setting autofocus on searchview when activity is started
        searchPageBind.svSearchfield.isIconifiedByDefault = false
        searchPageBind.svSearchfield.isFocusable = true
        searchPageBind.svSearchfield.requestFocusFromTouch()

        //initiate recyclerview and set parameters
        searchPageBind.rvSearchdata.setHasFixedSize(true)
        searchPageBind.rvSearchdata.setLayoutManager(LinearLayoutManager(this))

        searchPageBind.btnManual.setOnClickListener {
            startActivity(Intent(this@SearchPage, AddWord::class.java)) }

        //reload datafromnetwork on text input
        searchPageBind.svSearchfield.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(searchtext: String): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {

                if (newText != null) {
                    try {
                        mSearchList?.let {
                            queryTextChangedJob?.cancel()
                            clearData()
                        }
                    } catch (e: NullPointerException) {
                        Log.d(TAG, "${e.printStackTrace()}")
                    }

                    queryTextChangedJob = launch {
                        delay(350)
                        currentJobText = newText.toString()
                        Log.d(
                            TAG,
                            "onQueryTextChange: Job Started For: $currentJobText, (strlen: ${currentJobText.length})"
                        )
                        dataFromJamdict(newText)
                    }

                }

                return true
            }
        })

    }

    //Jamdict searchData
    private fun dataFromJamdict(query: String) {

        searchPageBind.tvInfo.visibility = View.INVISIBLE
        //if the search adapter has data in it already, clear the recyclerview
        searchPageBind.rvSearchdata.adapter = mSearchDataAdapter

        if(!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        } else {
            return
        }

        val py = Python.getInstance()
        val pyObj = py.getModule("search")
        val searchResultsJson = pyObj.callAttr("lookup", query).toString()

        val searchResults = parseJson(searchResultsJson)

        var kanji: String
        var kana: String
        var english: String
        for ((index, item) in searchResults.withIndex()) {
            kana = item.kana[0].text
            kanji = if (item.kanji.isNotEmpty()) item.kanji[0].text!! else kana

            english = item.senses.flatMap { it ->
                it.senseGloss.map { it.text }
            }.joinToString(", ")

            starFilled = myDB.checkStarred(kanji, english)

            // items to view in searchpage activity
            mSearchList!!.add(
                SearchDataItem(kanji, kana, english, starFilled)
            )

            // items to view in viewwordremote and viewword
            dataItems!!.add(MyListItem(index, kanji, kana, english, "pos", "notes"))

            mSearchDataAdapter = mSearchList?.let { it ->
                SearchDataAdapter(this@SearchPage, it)
            }
            searchPageBind.rvSearchdata.adapter = mSearchDataAdapter

        }
    }

    @Serializable
    data class Kanji(
        val text: String?,
        val pri: List<String>? = null
    )

    @Serializable
    data class Kana(
        val text: String,
        val nokanji: Int,
        val pri: List<String>? = null
    )

    @Serializable
    data class SenseGloss(
        val lang: String,
        val text: String
    )

    @Serializable
    data class Sense(
        val pos: List<String>,
        @SerialName("SenseGloss")
        val senseGloss: List<SenseGloss>
    )

    @Serializable
    data class JMDEntry(
        val idseq: Int,
        val kanji: List<Kanji>,
        val kana: List<Kana>,
        val senses: List<Sense>
    )

    private fun parseJson(jsonData: String): Array<JMDEntry> {
        val jsonArray = JSONArray(jsonData)
        val arrayofresults = Array(jsonArray.length()) { i ->
            val jsonString = jsonArray.getJSONObject(i).toString()
            Json.decodeFromString<JMDEntry>(jsonString)
        }
        return arrayofresults
    }

    fun clearData() {
        dataItems?.clear()
        mSearchList?.clear() // clear list
        mSearchDataAdapter?.notifyDataSetChanged() // let your adapter know about the changes and reload view.
    }

    override fun onDestroy() {
        queryTextChangedJob?.cancel()
        clickedItem = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (clickedItem != null ) {
            mSearchList?.get(clickedItem!!)?.starFilled = starFilled
            mSearchDataAdapter?.notifyItemChanged(clickedItem!!)
        }

    }

    companion object {
        var sp: AppCompatActivity? = null
        //items for carrying over to viewwordremote
        var dataItems: ArrayList<MyListItem>? = ArrayList()
        var starFilled: Boolean = false
        var clickedItem: Int? = null
    }

}

