package com.lewiswilson.kiminojisho.mylists

import android.app.*
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.GridLayoutManager
import com.lewiswilson.kiminojisho.DatabaseHelper
import com.lewiswilson.kiminojisho.R
import com.lewiswilson.kiminojisho.databinding.ListSelectionBinding
import java.util.*


class ListSelection : AppCompatActivity() {

    private lateinit var listSelectionBind: ListSelectionBinding

    private var listOfLists: ArrayList<ListSelectionItem>? = ArrayList()
    private var rvAdapter: ListSelectionAdapter? = null
    private var myDB: DatabaseHelper? = DatabaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listSelectionBind = ListSelectionBinding.inflate(layoutInflater)
        setContentView(listSelectionBind.root)

        //initiate recyclerview and set parameters
        listSelectionBind.rvListSelection.setHasFixedSize(true)
        listSelectionBind.rvListSelection.layoutManager = GridLayoutManager(this, 2)

        for (item in listOfLists!!) {
            item.name
        }

        populateRV()

        listSelectionBind.rvListSelection.adapter = rvAdapter

        rvAdapter?.setOnItemClickListener(object: ListSelectionAdapter.OnItemClickListener{
            override fun onItemClick(listId: Int) {
                val intent = Intent(applicationContext, MyList::class.java)
                intent.putExtra("listID", listId)
                startActivity(intent)
            }
            override fun onDeleteClick(listId: Int, pos: Int) {
                if (listOfLists?.size!! <= 1) {
                    Toast.makeText(applicationContext, "You must have at least 1 list!", Toast.LENGTH_SHORT).show()
                } else {
                    warningDialog(listId, pos)
                }
            }
        })

        listSelectionBind.btnCreateList.setOnClickListener { createList() }

    }

    private fun createList() {

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Create a new list")
        val input = EditText(this)
        input.setHint("list name")
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton("Create") { _, _ ->
            val userInput = input.text.toString().trim()
            //check not empty and check the item doesnt exist in the list already
            if (listOfLists?.any { it.name == userInput }!!) {
                Toast.makeText(this, "List already exists!", Toast.LENGTH_SHORT).show()
            }

            if(userInput.isNotEmpty() && !listOfLists?.any { it.name == userInput }!!) {
                    val listName = input.text.toString()
                    myDB?.addList(listName)
                    listOfLists!!.add(ListSelectionItem(listName))
                    rvAdapter?.notifyItemInserted(listOfLists?.size!!)
            }

        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    //populate recyclerview with data
    private fun populateRV() {

        val listArray = myDB!!.getLists()

        for (list in listArray) {
            listOfLists!!.add(ListSelectionItem(list))
            rvAdapter = listOfLists?.let { it -> ListSelectionAdapter(this@ListSelection, it) }
        }
    }

    private fun warningDialog(listId: Int, adapterPos: Int) {
        val listName = myDB?.getListNameFromId(listId)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete List")
            .setMessage("You are about to delete the list \'$listName\'. Removing this list will also delete ALL words in the list and any flashcard progress. Are you sure you want to delete this list?")
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton(
                getString(R.string.Delete_Entry)
            ) { _, _ ->
                try {
                    myDB?.deleteList(listId)
                    Log.d(ContentValues.TAG, "List Deleted")
                } catch (e: java.lang.Exception) {
                    Log.d(ContentValues.TAG, "Could not delete list: ${e.printStackTrace()}")
                }
                listOfLists?.removeAt(adapterPos)
                rvAdapter?.notifyItemRemoved(adapterPos)
            }
            .setNegativeButton(getString(R.string.Cancel)) { _, _ ->
            }
            .setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_info))
            .show()
    }

}