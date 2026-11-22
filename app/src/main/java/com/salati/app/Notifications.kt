package com.salati.app

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.ZoneId

private fun prayerReminder(name: String): String = when (name) {
    "الفجر" -> "قال رسول الله ﷺ: «ركعتا الفجر خير من الدنيا وما فيها». (صحيح مسلم: 725)"
    "الظهر" -> "قال رسول الله ﷺ: «من صلى أربع ركعات قبل الظهر وأربعًا بعدها حرمه الله على النار». (الترمذي: 428، صححه الترمذي)"
    "العصر" -> "قال رسول الله ﷺ: «من ترك صلاة العصر فقد حبط عمله». (صحيح البخاري: 553)"
    "المغرب" -> "قال رسول الله ﷺ: «لا تزال أمتي بخير ما لم يؤخروا المغرب حتى تشتبك النجوم». (أبو داود: 418، حسنه أهل العلم)"
    "العشاء" -> "قال رسول الله ﷺ: «ولو يعلمون ما في العتمة والصبح لأتوهما ولو حبوًا». (البخاري: 657، ومسلم: 651)"
    else -> "قال الله تعالى: ﴿إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا﴾ [النساء: 103]."
}

object PrayerNotifications {
    const val CHANNEL = "salati_prayers"
    fun createChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيه الصلاة جماعة قبل الوقت وعند دخوله"
            })
        }
    }
    fun schedule(context: Context, lat: Double, lon: Double) {
        val prefs = context.getSharedPreferences("salati", Context.MODE_PRIVATE)
        val notifyBefore = prefs.getBoolean("notify_before", true)
        val notifyAt = prefs.getBoolean("notify_at", true)
        val notificationTitle = "الصلاة جامعة"
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        for (offset in 0..7) {
            val date = LocalDate.now(zone).plusDays(offset.toLong())
            PrayerTimes.forDate(date, lat, lon, zone).forEachIndexed { index, prayer ->
                listOf(
                    Triple(30, notifyBefore, "باقي نصف ساعة على صلاة " + prayer.name + ". " + prayerReminder(prayer.name)),
                    Triple(0, notifyAt, "دخل وقت صلاة " + prayer.name + ". " + prayerReminder(prayer.name))
                ).forEach { (before, enabled, message) ->
                    val at = prayer.time - before * 60_000L
                    val id = (date.toEpochDay() % 100000).toInt() * 10000 + index * 100 + before
                    val intent = Intent(context, PrayerAlarmReceiver::class.java)
                        .putExtra("title", notificationTitle)
                        .putExtra("message", message).putExtra("id", id)
                    val pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    if (!enabled || at <= now + 5_000L) {
                        alarm.cancel(pi)
                        pi.cancel()
                    } else {
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
            .setContentTitle("الصلاة جامعة")
            .setContentText(intent.getStringExtra("message") ?: "الصلاة جماعة")
            .setStyle(NotificationCompat.BigTextStyle().bigText(intent.getStringExtra("message") ?: "الصلاة جماعة"))
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
