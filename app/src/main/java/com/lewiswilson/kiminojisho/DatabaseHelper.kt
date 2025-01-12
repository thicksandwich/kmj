package com.lewiswilson.kiminojisho

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.lewiswilson.MyApplication
import com.lewiswilson.kiminojisho.mylists.MyListItem
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DatabaseHelper internal constructor(private val myContext: Context) : SQLiteOpenHelper(myContext, DATABASE_NAME, null, 16) {

    private var db: SQLiteDatabase? = null

    fun createDatabase(uri: Uri?) {
        this.readableDatabase
        var errorThrown = false
        try {
            copyDatabase(uri)
        } catch (e: IOException) {
            errorThrown = true
            throw Error("Error Importing Database")
        } finally {
            if (!errorThrown) {
                Log.d("#DB", "Database Import Successful")
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS jisho_data ($colId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$colList INTEGER, " +
                    "$colKanji TEXT, " +
                    "$colKana TEXT, " +
                    "$colEnglish TEXT, " +
                    "$colPos TEXT, " +
                    "$colNotes TEXT, " +
                    "$colFCBox INTEGER, " +
                    "$colLastReviewed TEXT, " +
                    "$colNextReview TEXT, " +
                    "$colTimesSeen INTEGER, " +
                    "$colTimesCorrect INTEGER)")

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS lists ($listsColId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "$listsColName TEXT NOT NULL)")

        db.execSQL("INSERT INTO lists (list_name) VALUES (\'Main List\')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "DATABASE UPGRADE FROM ${oldVersion} to ${newVersion}")

    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.version = oldVersion
    }

    //Import DB from Assets folder
    @Throws(IOException::class)
    private fun copyDatabase(uri: Uri?) {
        val mInput = myContext.contentResolver.openInputStream(uri!!)  //myContext.getAssets().open("kiminojisho.db");
        val outFileName = DATABASE_PATH.toString()

        Log.d(TAG, "copyDatabase: $outFileName")
        val os = FileOutputStream(outFileName)

        val buffer = ByteArray(10)
        while (mInput!!.read(buffer) > 0) {
            os.write(buffer)
            Log.d("#DB", "writing>>")
        }

        os.flush()
        os.close()
        mInput.close()
    }


    @Synchronized
    override fun close() {
        val db = writableDatabase
        db?.close()
        super.close()
    }

    // number of items in database
    fun itemCount(listId: Int): Int {

        //list "0" denotes "all lists" as autoincrrment starts at 1 in sqlite
        val cur: Cursor = if(listId == 0){
            readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        } else {
            readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME" +
                    " WHERE $colList = ?", arrayOf(listId.toString()))
        }
        cur.moveToFirst()
        val count = cur.getInt(0)
        cur.close()
        return count
    }

    fun deleteData(itemId: String?) {
        //PreparedStatement (Avoiding SQL Injection & Crash)
        try {
            db = writableDatabase
            db?.beginTransaction()
            val sql = "DELETE FROM jisho_data WHERE $colId = ?"
            val statement = db?.compileStatement(sql)
            statement?.bindString(1, itemId)
            statement?.executeUpdateDelete()
            db?.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.w("Exception:", e)
        } finally {
            db?.endTransaction()
        }
    }

    fun deleteFromRemote(kanji: String, english: String) {
        //PreparedStatement (Avoiding SQL Injection & Crash)
        try {
            db = writableDatabase
            db?.beginTransaction()
            val sql = "DELETE FROM jisho_data WHERE $colKanji = ? AND $colEnglish = ?"
            val statement = db?.compileStatement(sql)
            statement?.bindString(1, kanji)
            statement?.bindString(2, english)
            statement?.executeUpdateDelete()
            db?.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.w("Exception:", e)
        } finally {
            db?.endTransaction()
        }
    }

    fun deleteList(listId: Int) {
        //PreparedStatement (Avoiding SQL Injection & Crash)
        val strListId = listId.toString()
        try {
            db = writableDatabase
            db?.beginTransaction()
            val sql = "DELETE FROM jisho_data WHERE $colList = ?"
            val statement = db?.compileStatement(sql)
            statement?.bindString(1, strListId)
            statement?.executeUpdateDelete()
            db?.setTransactionSuccessful()
        } catch (e: SQLException) {
            Log.w("Exception:", e)
        } finally {
            db?.execSQL("DELETE FROM $LISTS_TABLE_NAME WHERE $listsColId = ?", arrayOf(listId.toString()))
            db?.endTransaction()
        }
    }

    fun getData(itemId: Int): HashMap<String, String> {
        db = readableDatabase

        val cur = db?.rawQuery("SELECT * FROM " + TABLE_NAME +
        " WHERE $colId = ?", arrayOf(itemId.toString()))

        val hashMap = HashMap<String, String>()

        hashMap["id"] = itemId.toString()

        if (cur?.moveToFirst() == true) {
            hashMap["list"] = cur.getString(cur.getColumnIndex(colList))
            hashMap["kanji"] = cur.getString(cur.getColumnIndex(colKanji))
            hashMap["kana"] = cur.getString(cur.getColumnIndex(colKana))
            hashMap["english"] = cur.getString(cur.getColumnIndex(colEnglish))
            hashMap["pos"] = cur.getString(cur.getColumnIndex(colPos))
            hashMap["notes"] = cur.getString(cur.getColumnIndex(colNotes))
        }
        cur?.close()
        return hashMap
    }

    fun getLists(): List<String> {
        val cur = readableDatabase.rawQuery("SELECT $listsColName FROM $LISTS_TABLE_NAME", null)
        val listsArr = mutableListOf<String>()
        while (cur.moveToNext()) {
            listsArr.add(cur.getString(cur.getColumnIndex(listsColName)))
        }
        cur?.close()
        return listsArr
    }

    fun addList(name: String): Boolean {
        db = writableDatabase
        val contentValues = ContentValues()
        contentValues.put(listsColName, name)
        val str = LISTS_TABLE_NAME
        val sb = "addList: Adding list $name"
        Log.d(TAG, sb)
        return db?.insert(str, null, contentValues) != -1L
    }

    fun checkStarred(kanji: String, english: String): Boolean {
        db = readableDatabase

        val cur = db?.rawQuery("SELECT EXISTS(SELECT 1 FROM " + TABLE_NAME +
                " WHERE $colKanji = ? AND $colEnglish LIKE ?)", arrayOf(kanji, "$english%"))
        var bool = false
        if (cur?.moveToFirst() == true) {
            bool = cur.getInt(0) == 1
        }
        cur?.close()
        return bool
    }

    fun addData(list: Int, kanji: String, kana: String?, english: String?, pos: String?, notes: String?): Boolean {
        db = writableDatabase

        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date: Calendar = Calendar.getInstance()
        val dateNow: String = df.format(date.time)
        //increment day by 1
        date.add(Calendar.DAY_OF_MONTH, 1)
        val dateTmrw: String = df.format(date.time)

        Log.d(TAG, "addData: $date")

        val contentValues = ContentValues()
        contentValues.put(colList, list)
        contentValues.put(colKanji, kanji)
        contentValues.put(colKana, kana)
        contentValues.put(colEnglish, english)
        contentValues.put(colPos, pos)
        contentValues.put(colNotes, notes)
        contentValues.put(colFCBox, 1) //flashcard_box
        contentValues.put(colLastReviewed, dateNow) //date_reviewed
        contentValues.put(colNextReview, dateTmrw) //next_review
        contentValues.put(colTimesSeen, 0) //times_seen
        contentValues.put(colTimesCorrect, 0) //times_correct
        val str = TABLE_NAME
        val sb = "addData: Adding " +
                kanji +
                " to " +
                str
        Log.d(TAG, sb)
        return db?.insert(str, null, contentValues) != -1L
    }

    fun changeList(wordIds: ArrayList<Int>, list: Int) {
        db = writableDatabase

        for (id in wordIds) {
            val contentValues = ContentValues()
            contentValues.put(colList, list)
            val where = "$colId = ?"
            val whereArgs = arrayOf(id.toString())

            try {
                db?.update(TABLE_NAME, contentValues, where, whereArgs)
                Log.d(TAG, "List Updated")
            } catch (e: SQLException) {
                Log.d(TAG, "Could not change list: ${e.printStackTrace()}")
            }
        }
    }

    fun editNotes(id: String, notes: String){
        db = writableDatabase

        val contentValues = ContentValues()
        contentValues.put(colNotes, notes)
        val where = "$colId = ?"
        val whereArgs = arrayOf(id)

        try {
            db?.update(TABLE_NAME, contentValues, where, whereArgs)
            Toast.makeText(myContext, "Notes Saved", Toast.LENGTH_LONG).show()
        } catch (e: SQLException) {
            Log.d(TAG, "ERROR (editNotes): ${e.printStackTrace()}")
            Toast.makeText(myContext, "Error Saving Notes", Toast.LENGTH_LONG).show()
        }
    }

    fun listContents(list: Int, column: String): Cursor {
        val args = arrayOf(list.toString())
        return writableDatabase.rawQuery("SELECT $colId, $colKanji, $colKana, $colEnglish, $colPos, $colNotes FROM $TABLE_NAME " +
                "WHERE $colList = ? " +
                "ORDER BY $column " +
                "COLLATE NOCASE ASC", args)
    }

    fun random(): ArrayList<String> {

        val cursor = readableDatabase.rawQuery("SELECT $colKanji, $colKana, $colEnglish FROM $TABLE_NAME " +
                "WHERE $colId IN (SELECT $colId FROM $TABLE_NAME ORDER BY RANDOM() LIMIT 1)", null)

        var kanji = ""
        var kana = ""
        var english = ""

        if (cursor.moveToFirst()) {
            kanji = cursor.getString(cursor.getColumnIndex(colKanji))
            kana = cursor.getString(cursor.getColumnIndex(colKana))
            english = cursor.getString(cursor.getColumnIndex(colEnglish)).split("@@@")[0]
        }
        cursor.close()
        return arrayListOf(kanji, kana, english)
    }

    fun dueFlashcards(list: Int): ArrayList<MyListItem> {

        val reviewNo = 25
        //list "0" denotes "all lists" as autoincrrment starts at 1 in sqlite
        val cur: Cursor = if(list == 0){
            //all reviews
            readableDatabase.rawQuery("SELECT $colId, $colKanji, $colKana, $colEnglish, $colPos, $colNotes FROM $TABLE_NAME" +
                    " WHERE $colNextReview <= date('now') LIMIT ?", arrayOf(reviewNo.toString())) //next_review < date now
        } else {
            //filter by list
            readableDatabase.rawQuery("SELECT $colId, $colKanji, $colKana, $colEnglish, $colPos, $colNotes FROM $TABLE_NAME" +
                    " WHERE $colNextReview <= date('now')" +
                    " AND $colList = ?" +
                    " LIMIT ?",
                arrayOf(list.toString(), reviewNo.toString())) //next_review < date now
        }
        val flashcardList: ArrayList<MyListItem> = ArrayList()

        while (cur.moveToNext()) {
            flashcardList.add(
                MyListItem(
                    cur.getInt(0), //id
                    cur.getString(1), //kanji
                    cur.getString(2), //kana
                    cur.getString(3), //english
                    "", //pos
                    cur.getString(5) // unimportant here
                )
            )
        }

        cur.close()
        db?.close()
        return flashcardList
    }

    fun randomThreeWrong(correctKanji: String, listId: Int): ArrayList<MyListItem> {

        val wrongItems: ArrayList<MyListItem> = ArrayList()

        val cur: Cursor = if(listId == 0) {
            readableDatabase.rawQuery("SELECT DISTINCT $colId, $colKanji, $colKana, $colEnglish, $colPos, $colNotes FROM " + TABLE_NAME +
                        " WHERE $colKanji NOT IN (SELECT $colKanji FROM $TABLE_NAME WHERE $colKanji = ?)" +
                        " ORDER BY RANDOM() LIMIT 3",
                arrayOf(correctKanji))
        } else {
            readableDatabase.rawQuery("SELECT DISTINCT $colId, $colKanji, $colKana, $colEnglish, $colPos, $colNotes FROM " + TABLE_NAME +
                    " WHERE $colKanji NOT IN (SELECT $colKanji FROM $TABLE_NAME WHERE $colKanji = ?)" +
                    " AND $colList = ?" +
                    " ORDER BY RANDOM() LIMIT 3",
                arrayOf(correctKanji, listId.toString()))
        }

        while (cur.moveToNext()) {
            wrongItems.add(
                MyListItem(
                    cur.getInt(0), //id
                    cur.getString(1), //kanji
                    cur.getString(2), //kana
                    cur.getString(3), //english
                    "", //pos
                    cur.getString(5) // unimportant here
            )
            )
        }

        cur.close()
        db?.close()
        return wrongItems
    }

    // number of flashcards due
    fun flashcardCount(list: Int): Int {

        val cur: Cursor = if(list == 0){
            //all reviews
            readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME" +
                    " WHERE $colNextReview <= date('now')", null)
        } else {
            //filter by list
            readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME" +
                    " WHERE $colNextReview <= date('now') " +
                    " AND $colList = ?",
                arrayOf(list.toString())
            )
        }

        cur.moveToFirst()
        val count = cur.getInt(0)
        cur.close()
        return count
    }

    fun getListIdFromName(name: String): Int {
        val cur = readableDatabase.rawQuery("SELECT $listsColId FROM $LISTS_TABLE_NAME" +
                " WHERE $listsColName = ?", arrayOf(name))
        cur.moveToFirst()
        val id = cur.getInt(0)
        cur.close()
        return id
    }

    fun getListNameFromId(id: Int): String {
        val cur = readableDatabase.rawQuery("SELECT $listsColName FROM $LISTS_TABLE_NAME" +
                " WHERE $listsColId = ?", arrayOf(id.toString()))
        cur.moveToFirst()
        val name = cur.getString(0)
        cur.close()
        return name
    }

    fun updateFlashcard(id: Int, correct: Boolean, seen: Boolean) {
        val idString = id.toString()

        //get current record data
        val cur = readableDatabase.rawQuery("SELECT $colFCBox, $colTimesSeen, $colTimesCorrect FROM $TABLE_NAME" +
                " WHERE $colId = $idString", null)
        cur.moveToFirst()
        val box = cur.getInt(0) //flashcard_box
        var timesSeen = cur.getInt(1) //times seen
        var timesCorrect = cur.getInt(2) //times correct
        cur.close()

        val newDateStr = boxProcess(box, correct, seen)

        timesSeen++

        writableDatabase.execSQL("UPDATE $TABLE_NAME SET $colFCBox = $box WHERE $colId = $idString")
        writableDatabase.execSQL("UPDATE $TABLE_NAME SET $colTimesSeen = $timesSeen WHERE $colId = $idString")
        if(correct) {
            timesCorrect++
            writableDatabase.execSQL("UPDATE $TABLE_NAME SET $colLastReviewed = date('now') WHERE $colId = $idString")
            writableDatabase.execSQL("UPDATE $TABLE_NAME SET $colNextReview = date('now', '$newDateStr') WHERE $colId = $idString")
            writableDatabase.execSQL("UPDATE $TABLE_NAME SET $colTimesCorrect = $timesCorrect WHERE $colId = $idString")
        }
        writableDatabase.close()
    }

    private fun boxProcess(box: Int, correct: Boolean, seen: Boolean): String {

        var newBox = box
        if (correct && newBox < 9 && !seen) {
            newBox++
        } else if (!correct && newBox > 1 && seen) { //prevents moving down many boxes in 1 session
            newBox--
        } //otherwise, no change in box

        // days interval (fibonacci from n=2)
        val interval: Int = when (newBox) {
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 5
            5 -> 8
            6 -> 13
            7 -> 21
            8 -> 34
            9 -> 55
            else -> 55
        }
        return if (interval == 1) {
            "+$interval day"
        } else {
            "+$interval days"
        }
    }

        companion object {
        private const val colId = "ID"
        private const val colList = "list"
        private const val colKanji = "kanji"
        private const val colKana = "kana"
        private const val colEnglish = "english"
        private const val colPos = "part_of_speech"
        private const val colNotes = "notes"
        private const val colFCBox = "flashcard_box"
        private const val colLastReviewed = "last_reviewed"
        private const val colNextReview = "next_review"
        private const val colTimesSeen = "times_seen"
        private const val colTimesCorrect = "times_correct"
        private const val DATABASE_NAME = "kiminojisho.db"
        private val DATABASE_PATH = MyApplication.appContext?.getDatabasePath(DATABASE_NAME)//"/data/data/com.lewiswilson.kiminojisho/databases/"
        private const val TABLE_NAME = "jisho_data"
        private const val LISTS_TABLE_NAME = "lists"
        private const val listsColId = "ID"
        private const val listsColName = "list_name"
        private const val TAG = "DatabaseHelper"
    }

    init {
        Log.e("Database Path:", DATABASE_PATH.toString())
    }
}