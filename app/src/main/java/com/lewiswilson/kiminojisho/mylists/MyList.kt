package com.lewiswilson.kiminojisho.mylists

import android.app.*
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.lewiswilson.kiminojisho.DatabaseHelper
import com.lewiswilson.kiminojisho.R
import com.lewiswilson.kiminojisho.databinding.MyListBinding
import com.lewiswilson.kiminojisho.search.SearchPage
import com.lewiswilson.kiminojisho.search.ViewWord
import java.util.*


class MyList : AppCompatActivity(), MyListAdapter.OnItemClickListener, MyListAdapter.OnItemLongClickListener {

    private lateinit var myListBind: MyListBinding

    private val prefsName = "MyPrefs"
    private var myDB: DatabaseHelper? = null
    private var jishoList: ArrayList<MyListItem>? = ArrayList()
    private var searchList: ArrayList<MyListItem>? = ArrayList()
    private var rvAdapter: MyListAdapter? = null
    private var selectedList = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myListBind = MyListBinding.inflate(layoutInflater)
        setContentView(myListBind.root)
        myDB = DatabaseHelper(this)

        //implementing ads
        MobileAds.initialize(this)
        val testDeviceIds = listOf("3EE9A91017FEA4E8E9F0996A4775B406")
        val config = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(config)
        val adRequest = AdRequest.Builder().build()
        myListBind.mylistAdView.loadAd(adRequest)

        multiselectViews()

        selectedList = intent.getIntExtra("listID", 0)

        //initiate recyclerview and set parameters
        myListBind.rvMylist.setHasFixedSize(true)
        myListBind.rvMylist.layoutManager = LinearLayoutManager(this)

        populateRV()
        //displaylist shows when searching in searchview
        searchList!!.addAll(jishoList!!)

        multiSelectMenuSetup()

        myListBind.flbtnAdd.setOnClickListener { _: View? ->
            finish()
            startActivity(Intent(this@MyList, SearchPage::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        if(deleted) {
            jishoList?.removeAt(clickedItemPos!!)
            rvAdapter?.notifyItemRemoved(clickedItemPos!!)
            deleted = false
        }
    }

    //populate recyclerview with data
    private fun populateRV() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val column = prefs.getString("sortby_col", "english")
        val data = myDB!!.listContents(selectedList, column!!)

        //Checks if database is empty and lists entries if not
        while (data.moveToNext()) {

            //ListView Data Layout
            jishoList!!.add(
                MyListItem(
                    data.getInt(0), //id
                    data.getString(1), //kanji
                    data.getString(2), //kana
                    data.getString(3).split("@@@")[0], //english
                    data.getString(4), //pos
                    data.getString(5) // unimportant here
                )
            )

            rvAdapter = jishoList?.let { it -> MyListAdapter(this@MyList, it, this, this) }
            myListBind.rvMylist.adapter = rvAdapter

        }

    }

    fun clearData() {
        jishoList!!.clear() // clear list
        rvAdapter!!.notifyDataSetChanged() // let your adapter know about the changes and reload view.
        populateRV()

    }

    // Menu icons are inflated just as they were with actionbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.mylist_menu, menu)

        //find search menuitem
        val menuItem = menu.findItem(R.id.searchView)
        //Initialize searchview
        val searchView = menuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(searchtext: String): Boolean {
                return false
            }

            override fun onQueryTextChange(searchtext: String?): Boolean {
                // populate the recyclerview on search query change
                if(searchtext!!.isNotEmpty()) {
                    jishoList!!.clear()
                    val search = searchtext.lowercase(Locale.getDefault())
                    searchList!!.forEach {
                        if (it.english.lowercase(Locale.getDefault()).contains(search) or
                            it.kana.lowercase(Locale.getDefault()).contains(search) or
                            it.kanji.lowercase(Locale.getDefault()).contains(search)
                        ) {

                            jishoList!!.add(it)
                        }
                    }

                    rvAdapter!!.notifyDataSetChanged()

                } else {
                    clearData()
                }

                return false
            }
        })
        return true
    }

    //Toolbar Menu Option Activities
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return when (item.itemId) {
            R.id.english -> {
                prefs.edit().putString("sortby_col", "english").apply()
                prefs.edit().putString("sort_style", "ASC").apply()
                clearData()
                true
            }

            R.id.kana -> {
                prefs.edit().putString("sortby_col", "kana").apply()
                prefs.edit().putString("sort_style", "ASC").apply()
                clearData()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun multiSelectMenuSetup() {
        myListBind.btnDelete.setOnClickListener{
            warningDialog()
        }
        myListBind.btnSelectall.setOnClickListener{
            rvAdapter?.selectAll()
            clearData()
            if(MyListAdapter.selectedIds.isNullOrEmpty()){
                MyListAdapter.multiSelectMode = false
                multiselectViews()
            }
        }
        myListBind.btnMove.setOnClickListener{
            listSelectDialog()
        }
    }

    private fun warningDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete entry")
            .setMessage("Removing these words will also reset any flashcard progress for this word. Are you sure you want to delete them?")
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton(
                getString(R.string.Delete_Entry)
            ) { _, _ ->
                val idList = MyListAdapter.selectedIds
                for (id in idList) {
                    try {
                        myDB?.deleteData(id.toString())
                        Log.d(TAG, "Delete Successful")
                    } catch (e: java.lang.Exception) {
                        Log.d(TAG, "Could not delete item from list: ${e.printStackTrace()}")
                    }
                }
                MyListAdapter.selectedIds.clear()
                MyListAdapter.multiSelectMode = false
                multiselectViews()
                clearData()
                Toast.makeText(this, "Items deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.Cancel)) { _, _ ->
            }
            .setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_info))
            .show()
    }

    private fun listSelectDialog() {

        val listArray = myDB!!.getLists().toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select List to Move To")
            .setItems(listArray) { _, which ->
                val wordIds = MyListAdapter.selectedIds
                val selected = listArray[which]
                val listId = myDB!!.getListIdFromName(selected)
                try {
                    myDB?.changeList(wordIds, listId)
                } catch (e: Exception) {
                    Log.d(TAG, "multiSelectMenuSetup: Could not move items to list #$which")
                    Log.d(TAG, e.printStackTrace().toString())
                }
                MyListAdapter.selectedIds.clear()
                MyListAdapter.multiSelectMode = false
                multiselectViews()
                clearData()
                Toast.makeText(this, "Items moved to list: ${listArray.get(which)}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // recyclerview item click handling
    override fun onItemClick(itemId: Int, adapterPos: Int, ready: Boolean) {
        if (ready) {
            Log.d(TAG, "onItemClick: ${adapterPos}")
            Log.d(TAG, "onItemClick: ${jishoList?.get(adapterPos)?.kanji}")
            clickedItemPos = adapterPos
            clickedItemId = itemId
            startActivity(Intent(this@MyList, ViewWord::class.java))
        }
        multiselectViews()
    }

    override fun onItemLongClick(itemId: Int) {
        clickedItemId = itemId
        multiselectViews()
    }

    private fun multiselectViews() {
        if(MyListAdapter.multiSelectMode) {
            myListBind.multiselectmenuMylist.visibility = View.VISIBLE
            myListBind.myListSeparator.visibility = View.VISIBLE
        } else {
            myListBind.multiselectmenuMylist.visibility = View.GONE
            myListBind.myListSeparator.visibility = View.GONE
        }
    }

    companion object {
        @JvmField
        var clickedItemId //use item_id to get and display database data
                : Int? = null
        @JvmField
        var ma: AppCompatActivity? = null

        var clickedItemPos : Int? = null
        var deleted = false
    }


    override fun onBackPressed() {
        super.onBackPressed()
        MyListAdapter.selectedIds.clear()
        MyListAdapter.multiSelectMode = false
        jishoList?.clear() // clear list
    }

}