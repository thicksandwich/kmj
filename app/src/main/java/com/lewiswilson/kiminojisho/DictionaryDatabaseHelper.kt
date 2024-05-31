import android.content.Context
import android.util.Log
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import java.io.BufferedReader
import java.io.InputStreamReader

class DictionaryDatabaseHelper(context: Context) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        // stored at src/main/assets/databases
        private const val DATABASE_NAME = "JMdict_e.db"
        private const val DATABASE_VERSION = 1
    }

    fun executeSqlFile(context: Context, db: android.database.sqlite.SQLiteDatabase, sqlFileResId: Int) {
        val inputStream = context.resources.openRawResource(sqlFileResId)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        var sqlStatement = StringBuilder()
        bufferedReader.useLines { lines ->
            lines.forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("--")) {
                    sqlStatement.append(" ")
                    sqlStatement.append(line)
                    if (line.endsWith(";")) {
                        val sql = sqlStatement.toString()
                        try {
                            db.execSQL(sql)
                        } catch (e: Exception) {
                            Log.e("SQL", "Error executing SQL: $sql", e)
                        }
                        sqlStatement = StringBuilder()
                    }
                }
            }
        }
    }
}
