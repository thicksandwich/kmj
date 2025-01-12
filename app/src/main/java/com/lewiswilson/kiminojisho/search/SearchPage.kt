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
import kotlin.math.max

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

        val isEnglish = containsEnglish(query)

        searchPageBind.tvInfo.visibility = View.INVISIBLE
        //if the search adapter has data in it already, clear the recyclerview
        searchPageBind.rvSearchdata.adapter = mSearchDataAdapter

        val db: SQLiteDatabase = dictionaryDbHelper.readableDatabase

        // Read the whole content of the SQL file as a single string
        val inputStream = if (isEnglish) {
            this.resources.openRawResource(R.raw.search_english)
        } else {
            this.resources.openRawResource(R.raw.search_kanji_kana)
        }

        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val searchSql = bufferedReader.use { it.readText() }

        val cursor = if (isEnglish) {
            Log.i(TAG, "Searching in English")
            db.rawQuery(searchSql, arrayOf("$query*"))
        } else {
            Log.i(TAG, "Searching in Japanese")
            db.rawQuery(searchSql, arrayOf("$query*", "$query*"))
        }

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

        if (isEnglish) {
            // process the search results to account for english definition priority
            populateList(sortEnglishPriority(query, searchResults))
        } else {
            // kanji/kana priority already calculated in database
            populateList(searchResults)
        }

    }

    private fun containsEnglish(text: String): Boolean {
        val regex = Regex("[a-zA-Z]")
        return regex.containsMatchIn(text)
    }

    private val similarityCache = mutableMapOf<Pair<String, String>, Double>()
    // Function to calculate similarity between two strings
    private fun calculateSimilarity(definition: String, searchWord: String): Double {
        val key = Pair(definition, searchWord)
        return similarityCache.getOrPut(key) {
            // Normalize the definition by removing "to " prefix if it exists
            val normalisedDefinition = if (definition.lowercase().startsWith("to ") && !searchWord.lowercase().startsWith("to ")) {
                definition.substring(3)
            } else {
                definition
            }

            // Exact match priority
            if (normalisedDefinition.equals(searchWord, ignoreCase = true)) {
                return Double.MAX_VALUE
            }

            // Count common letters between definition and searchWord
            val commonLetters = normalisedDefinition.zip(searchWord).count { (a, b) -> a == b }
            // Find the maximum length between definition and searchWord
            val maxLength = max(normalisedDefinition.length, searchWord.length).toDouble()
            // Calculate similarity as the ratio of common letters to maximum length
            commonLetters / maxLength
        }
    }

    // Function to process search results and return the sorted list
    private fun sortEnglishPriority(searchWord: String, searchResults: MutableList<Map<String, String>>): MutableList<Map<String, String>> {
        // Calculate similarity scores for visible entries in searchResults
        val scoredResults = searchResults.map { result ->
            val definition = result["english"] ?: ""
            // Split the definition into individual items
            val items = definition.split(",").map { it.trim() }
            // Calculate the similarity for each item, considering the position priority
            val maxSimilarity = items.mapIndexedNotNull { index, item ->
                val positionPriority = 1.0 / (index + 1)  // Higher priority for items closer to the beginning
                val similarity = calculateSimilarity(item, searchWord)
                // Exclude items with zero similarity to improve efficiency
                if (similarity > 0.0) similarity * positionPriority else null
            }.maxOrNull() ?: 0.0

            // Adjust the score based on the number of items, ensuring exact matches have precedence
            val numberOfItemsFactor = 1.0 / items.size
            val adjustedScore = if (maxSimilarity == Double.MAX_VALUE) {
                Double.MAX_VALUE
            } else {
                maxSimilarity * numberOfItemsFactor
            }

            result + ("similarityScore" to adjustedScore.toString())
        }.toMutableList()

        // Sort the visible results by similarity score in descending order
        scoredResults.sortByDescending { it["similarityScore"]?.toDoubleOrNull() ?: 0.0 }

        return scoredResults
    }

    private fun populateList(searchResults: List<Map<String, String>>) {
        var kanji: String
        var kana: String
        var english: String
        for ((index, item) in searchResults.withIndex()) {
            kana = item["kana"].toString().replace(",", ", ")
            kanji = if (item["kanji"].isNullOrEmpty()){
                kana
            } else {
                item["kanji"].toString()
            }
            english = item["english"].toString().replace(",", ", ")

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

