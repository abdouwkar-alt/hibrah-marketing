package com.salati.app

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.ZoneId

object PrayerNotifications {
    const val CHANNEL = "salati_prayers"
    fun createChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تذكير قبل الصلاة وعند دخول وقتها"
            })
        }
    }
    private fun reminder(name: String): String = when (name) {
        "الفجر" -> "قبل الفجر: ركعتا سنة الفجر المؤكدة."
        "الظهر" -> "قبل الظهر: أربع ركعات راتبة مؤكدة."
        "العصر" -> "قبل العصر: لا توجد راتبة مؤكدة؛ توجد نافلة غير مؤكدة."
        "المغرب" -> "تذكير بالمغرب؛ الراتبة المؤكدة ركعتان بعد الفرض."
        "العشاء" -> "تذكير بالعشاء؛ الراتبة المؤكدة ركعتان بعد الفرض."
        else -> "تذكير بالصلاة."
    }
    fun schedule(context: Context, lat: Double, lon: Double) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        for (offset in 0..7) {
            val date = LocalDate.now(zone).plusDays(offset.toLong())
            PrayerTimes.forDate(date, lat, lon, zone).forEachIndexed { index, prayer ->
                listOf(30 to reminder(prayer.name), 0 to "دخل وقت صلاة ${prayer.name}.").forEach { (before, message) ->
                    val at = prayer.time - before * 60_000L
                    if (at > now + 5_000L) {
                        val id = (date.toEpochDay() % 100000).toInt() * 10000 + index * 100 + before
                        val intent = Intent(context, PrayerAlarmReceiver::class.java)
                            .putExtra("title", if (before == 0) "حان وقت الصلاة" else "تذكير قبل الصلاة")
                            .putExtra("message", message).putExtra("id", id)
                        val pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) {
                                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                            } else {
                                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                            }
                        } catch (_: SecurityException) {
                            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                        }
                    }
                }
            }
        }
    }
}
class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerNotifications.createChannel(context)
        val id = intent.getIntExtra("id", 1)
        val notification = NotificationCompat.Builder(context, PrayerNotifications.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(intent.getStringExtra("title") ?: "صلاتي")
            .setContentText(intent.getStringExtra("message") ?: "حان وقت الصلاة")
            .setStyle(NotificationCompat.BigTextStyle().bigText(intent.getStringExtra("message") ?: "حان وقت الصلاة"))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = context.getSharedPreferences("salati", Context.MODE_PRIVATE)
        if (p.contains("lat") && p.contains("lon")) PrayerNotifications.schedule(context, p.getFloat("lat", 30.0444f).toDouble(), p.getFloat("lon", 31.2357f).toDouble())
    }
}
