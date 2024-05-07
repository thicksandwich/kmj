package com.lewiswilson.kiminojisho

import android.annotation.SuppressLint
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.lewiswilson.kiminojisho.databinding.SettingsBinding
import java.text.DecimalFormat
import java.util.*

class Settings : AppCompatActivity() {

    private lateinit var settingsBind: SettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsBind = SettingsBinding.inflate(layoutInflater)
        setContentView(settingsBind.root)

        val prefsName = "MyPrefs"
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val alarmUp = PendingIntent.getBroadcast(applicationContext,
            0,
            Intent(applicationContext, ReminderBroadcast::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) != null

        //sets enabled status based on if alarm exists
        settingsBind.switchNotifications.isChecked = alarmUp
        if(settingsBind.switchNotifications.isChecked) {
            settingsBind.txtStatus.text = getString(R.string.ON)
            //setting time to value in sharedprefs
            val millis = prefs.getLong("daily_notifications", 0)
            val c = Calendar.getInstance()
            c.timeInMillis = millis
            settingsBind.timepicker1.hour = c[Calendar.HOUR_OF_DAY]
            settingsBind.timepicker1.minute = c[Calendar.MINUTE]
            settingsBind.timepicker1.isEnabled = false
        }

        settingsBind.timepicker1.setIs24HourView(true)

        settingsBind.switchNotifications.setOnClickListener {

            if(settingsBind.switchNotifications.isChecked) {
                settingsBind.txtStatus.text = getString(R.string.ON)
                val hour = settingsBind.timepicker1.hour
                val min = settingsBind.timepicker1.minute
                createChannel()
                setNotifications(hour, min)
                settingsBind.timepicker1.isEnabled = false
            } else {
                settingsBind.txtStatus.text = getString(R.string.OFF)
                cancelNotifications()
                settingsBind.timepicker1.isEnabled = true
            }
        }

    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Daily Notifications"
            val description = "Word of the day"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("wotd", name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    // notification functions
    private fun setNotifications(hour: Int, min: Int) {

        val prefsName = "MyPrefs"
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val c = Calendar.getInstance()

        val currentTime = c.timeInMillis

        c[Calendar.HOUR_OF_DAY] = hour
        c[Calendar.MINUTE] = min
        c[Calendar.SECOND] = 0

        val notifyTime = c.timeInMillis

        // if time is in the past, add a day to prevent notification being shown instantly
        if(notifyTime<currentTime){
            c[Calendar.DAY_OF_MONTH] += 1
        }

        val intent = Intent(applicationContext, ReminderBroadcast::class.java)
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)

        // enables notification even after reboot
        val receiver = ComponentName(applicationContext, ReminderBroadcast::class.java)
        applicationContext.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        val dec = DecimalFormat("00")
        val time = "${dec.format(hour)}:${dec.format(min)}"

        //add into shared prefs for if reboot occurs (check ReminderBroadcast.kt)
        prefs.edit().putLong("daily_notifications", c.timeInMillis).apply()

        Toast.makeText(applicationContext, "Notifications set for $time daily", Toast.LENGTH_LONG).show()

    }

    private fun cancelNotifications() {
        //cancel
        PendingIntent.getBroadcast(applicationContext, 0,
            Intent(applicationContext, ReminderBroadcast::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE).cancel()

        // cancels popping up on device reboot
        val receiver = ComponentName(applicationContext, ReminderBroadcast::class.java)
        applicationContext.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        Toast.makeText(applicationContext, "Notifications stopped", Toast.LENGTH_LONG).show()
    }


}

