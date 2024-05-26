package com.lewiswilson.kiminojisho.search

import DictionaryDatabaseHelper
import android.content.ContentValues.TAG
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.lewiswilson.kiminojisho.AddWord
import com.lewiswilson.kiminojisho.DatabaseHelper
import com.lewiswilson.kiminojisho.R
import com.lewiswilson.kiminojisho.databinding.SearchPageBinding
import com.lewiswilson.kiminojisho.mylists.MyListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class SearchPage : AppCompatActivity(), CoroutineScope {

    private lateinit var searchPageBind: SearchPageBinding

    private var mSearchList: ArrayList<SearchDataItem>? = ArrayList()
    private var mSearchDataAdapter: SearchDataAdapter? = null
    private val myDB = DatabaseHelper(this)
    private lateinit var dictionaryDbHelper: DictionaryDatabaseHelper
    var queryTextChangedJob: Job? = null
    var currentJobText = ""

    private val job = Job()
    override val coroutineContext = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchPageBind = SearchPageBinding.inflate(layoutInflater)
        setContentView(searchPageBind.root)
        sp = this

        dictionaryDbHelper = DictionaryDatabaseHelper(this)

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
                        currentJobText = newText.toString()
                        Log.d(
                            TAG,
                            "onQueryTextChange: Job Started For: $currentJobText, (strlen: ${currentJobText.length})"
                        )
                        if(newText.isNotEmpty()) search(newText)
                    }

                }
                return true
            }
        })

    }

    private fun search(query: String) {
        searchPageBind.tvInfo.visibility = View.INVISIBLE
        //if the search adapter has data in it already, clear the recyclerview
        searchPageBind.rvSearchdata.adapter = mSearchDataAdapter

        val db: SQLiteDatabase = dictionaryDbHelper.readableDatabase

        // Read the whole content of the SQL file as a single string
        val inputStream = this.resources.openRawResource(R.raw.search)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val searchSql = bufferedReader.use { it.readText() }
        val cursor = db.rawQuery(searchSql, arrayOf(query, query, query))

        val searchResults = mutableListOf<Map<String, String>>()
        var resultCount = 0
        while (cursor.moveToNext() && resultCount < 50) {
            val kanji = cursor.getString(cursor.getColumnIndexOrThrow("kanji"))
            val kana = cursor.getString(cursor.getColumnIndexOrThrow("kana"))
            val english = cursor.getString(cursor.getColumnIndexOrThrow("english"))

            val entry = mapOf(
                "kanji" to kanji,
                "kana" to kana,
                "english" to english
            )

            searchResults.add(entry)
            resultCount++
        }
        cursor.close()
        populateList(searchResults)
    }

    private fun populateList(searchResults: List<Map<String, String>>) {
        var kanji: String
        var kana: String
        var english: String
        for ((index, item) in searchResults.withIndex()) {
            kana = item["kana"].toString().split(",")[0]
            kanji = item["kanji"].toString().ifEmpty { kana }
            english = item["english"].toString().split(",").take(3).toString()

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

