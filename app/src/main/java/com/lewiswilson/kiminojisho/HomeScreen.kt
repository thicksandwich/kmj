package com.lewiswilson.kiminojisho

import DictionaryDatabaseHelper
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.elconfidencial.bubbleshowcase.BubbleShowCaseBuilder
import com.elconfidencial.bubbleshowcase.BubbleShowCaseSequence
import com.google.android.material.snackbar.Snackbar
import com.lewiswilson.kiminojisho.databinding.HomeScreenBinding
import com.lewiswilson.kiminojisho.flashcards.FlashcardsHome
import com.lewiswilson.kiminojisho.mylists.ListSelection
import com.lewiswilson.kiminojisho.search.SearchPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

class HomeScreen : AppCompatActivity() {

    private lateinit var homeScreenBind: HomeScreenBinding
    // For initializing the embedded dictionary database
    private lateinit var dictionaryDbHelper: DictionaryDatabaseHelper

    private val prefsName = "MyPrefs"
    private var myDB: DatabaseHelper? = DatabaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeScreenBind = HomeScreenBinding.inflate(layoutInflater)
        setContentView(homeScreenBind.root)

        // Initialize ProgressDialog
        val dbInitDialog = Dialog(this)
        dbInitDialog.setContentView(R.layout.db_init_dialog)
        dbInitDialog.setCancelable(false)

        // Initialize dictionaryDbHelper here
        dictionaryDbHelper = DictionaryDatabaseHelper(this)

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        //Check if it is a first time launch
        if (prefs.getBoolean("first_launch", true)) {

            // Initialise the JMDict database
            dbInitDialog.show()
            lifecycleScope.launch {
                delay(1000)
                val result = initialiseDictionaryDb()
                dbInitDialog.dismiss()
                if (result == "success") {
                    Snackbar.make(homeScreenBind.root, "Database initialization complete", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(homeScreenBind.root, "Database initialization error", Snackbar.LENGTH_SHORT).show()
                }
            }

            helpDisplay()
            prefs.edit().putBoolean("first_launch", false).apply()
            prefs.edit().putBoolean("notifications_on", false).apply()
            prefs.edit().putString("sortby_col", "english").apply()
            prefs.edit().putStringSet("list_names", hashSetOf("Main List")).apply()
        }

        homeScreenBind.itemFlashcards.setOnClickListener {
            startActivity(Intent(this@HomeScreen, FlashcardsHome::class.java)) }
        homeScreenBind.itemSearch.setOnClickListener {
            startActivity(Intent(this@HomeScreen, SearchPage::class.java)) }
        homeScreenBind.itemMylists.setOnClickListener {
            startActivity(Intent(this@HomeScreen, ListSelection::class.java)) }
        homeScreenBind.itemSettings.setOnClickListener {
            startActivity(Intent(this@HomeScreen, Settings::class.java)) }

    }

    // Menu icons are inflated just as they were with actionbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home_screen_menu, menu)
        return true
    }

    //Toolbar Menu Option Activities
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_help -> {
                helpDisplay()
                true
            }
            R.id.action_import -> {
                val diaBox = importWarning()
                diaBox.show()
                true
            }
            R.id.action_export -> {
                exportDatabase()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initialiseDictionaryDb(): String? {
        return try {
            val db: SQLiteDatabase = dictionaryDbHelper.writableDatabase
            dictionaryDbHelper.executeSqlFile(this@HomeScreen, db, R.raw.create_fts_table)

            // Perform a sample query to verify the data
            val cursor = db.rawQuery("SELECT * FROM jmdict_fts WHERE entry_id MATCH '9999999';", null)

            // Check if the query returned any rows
            val success = if (cursor.moveToFirst()) {
                // Retrieve the value of the "kanji" column from the cursor
                val kanjiColumnIndex = cursor.getColumnIndexOrThrow("kanji")
                val kanjiValue = cursor.getString(kanjiColumnIndex)

                // Check if the retrieved value matches the expected value
                kanjiValue == "ＪＭｄｉｃｔ"
            } else {
                false
            }
            cursor.close()
            db.close()

            if (success) {
                "success"
            } else {
                "Table creation failed (entry_id 9999999 in the JMdict_fts table, does not correspond to the kanji value \"ＪＭｄｉｃｔ\")"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Database initialization error: ${e.message}"
        }
    }

    private fun helpDisplay() {
        val title = BubbleShowCaseBuilder(this)
            .title("Welcome to KiminoJisho!")
            .description("This app will help you learn Japanese by using spaced repitition (SRS) style flashcards on the Japanese words YOU choose!")
            .targetView(homeScreenBind.homescreenBlankView)

        val flashcards = BubbleShowCaseBuilder(this)
            .title("Flashcards")
            .description("Here, you can test yourself using flashcards on the words in your lists. (You will need to add some words first!)")
            .targetView(homeScreenBind.itemFlashcards)

        val search = BubbleShowCaseBuilder(this)
            .title("Search")
            .description("Search for a words to add to one of your lists.")
            .targetView(homeScreenBind.itemSearch)

        val myLists = BubbleShowCaseBuilder(this)
            .title("My Lists")
            .description("Check out and review the words you've added.")
            .targetView(homeScreenBind.itemMylists)

        val settings = BubbleShowCaseBuilder(this)
            .title("Settings")
            .description("Enable daily notifications etc.")
            .targetView(homeScreenBind.itemSettings)


        val final = BubbleShowCaseBuilder(this)
            .title("That's all there is to it!")
            .description("We hope you enjoy Kiminojisho. To see this help display again, click the icon in the toolbar.")
            .targetView(homeScreenBind.homescreenBlankView)

        BubbleShowCaseSequence()
            .addShowCase(title)
            .addShowCase(flashcards)
            .addShowCase(search)
            .addShowCase(myLists)
            .addShowCase(settings)
            .addShowCase(final)
            .show()
    }

    private fun importWarning(): AlertDialog {
        return AlertDialog.Builder(this)
            .setTitle("Import")
            .setMessage("Select your previously exported 'kiminojisho.db' file. " +
                    "IMPORTANT: All data will be completely overwritten. Are you SURE you want to overwrite everything?")
            .setPositiveButton("Import") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.type = "*/*"
                startForResult.launch(intent)
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .create()
    }

    private fun importDatabase(uri: Uri?) {
        try {
            myDB!!.createDatabase(uri)
            finish()
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun exportDatabase() {

        var errorThrown = false
        try {
            val fileToWrite = getDatabasePath(myDB?.databaseName).toString()
            val output = openFileOutput(myDB?.databaseName, Context.MODE_PRIVATE)
            val input = FileInputStream(fileToWrite)
            val buffer = ByteArray(1024)
            var length: Int
            while (input.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }
            output.write(fileToWrite.toByteArray())
            output.flush()
            output.close()
            input.close()

            //exporting
            val context = applicationContext
            val filelocation = File(filesDir, myDB!!.databaseName)
            val path = FileProvider.getUriForFile(context, "com.lewiswilson.kiminojisho.fileprovider", filelocation)
            val fileIntent = Intent(Intent.ACTION_SEND)
            fileIntent.type = "application/x-sqlite3"
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, myDB?.databaseName)
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            fileIntent.putExtra(Intent.EXTRA_STREAM, path)

            val chooser = Intent.createChooser(fileIntent, "Export Database")
            val resInfoList = this.packageManager.queryIntentActivities(
                chooser,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                grantUriPermission(
                    packageName,
                    path,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            startActivity(chooser)
        } catch (e: Exception) {
            errorThrown = true
            e.printStackTrace()
        } finally {
            if (!errorThrown) {
                Log.d(TAG, "Database export sucessful.")
            }
        }

    }

    //Request Permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 || !(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Permission denied to read External storage", Toast.LENGTH_SHORT).show()
        }
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if(result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            var fileUri: Uri? = null
            if (data != null) {
                fileUri = data.data
            }
            importDatabase(fileUri)
        }
    }

    companion object{
        const val REQUEST_CODE = 10
    }

}