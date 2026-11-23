package com.salati.app

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import java.time.*
import kotlin.math.*

private data class ReminderContent(val title: String, val body: String, val source: String)

private fun contentForPrayer(name: String, slot: Int): ReminderContent {
    val common = listOf(
        ReminderContent("تهيأ للصلاة", "تذكّر الوضوء وإسباغه، واستعد للصلاة بسكينة.", "تذكير عملي"),
        ReminderContent("السواك قبل الصلاة", "قال رسول الله ﷺ: «لولا أن أشق على أمتي لأمرتهم بالسواك عند كل صلاة».", "صحيح البخاري 887، صحيح مسلم 252"),
        ReminderContent("الدعاء بين الأذان والإقامة", "أكثر من الدعاء في هذا الوقت، واسأل الله من خيري الدنيا والآخرة.", "سنن أبي داود 521؛ وجاء أصل المعنى في جامع الترمذي 212"),
        ReminderContent("استعد للخشوع", "أقبل على الصلاة بقلب حاضر، وتدبّر ما تقرأه من القرآن والأذكار.", "تذكير تربوي")
    )
    val specific = when (name) {
        "الفجر" -> listOf(
            ReminderContent("سنة الفجر", "حافظ على ركعتي سنة الفجر؛ قال ﷺ: «ركعتا الفجر خير من الدنيا وما فيها».", "صحيح مسلم 725"),
            ReminderContent("قراءة سنة الفجر", "ورد أن النبي ﷺ كان يقرأ في ركعتي الفجر بسورتي الكافرون والإخلاص.", "صحيح مسلم 726"),
            ReminderContent("صلاة الفجر", "قال ﷺ: «من صلى البردين دخل الجنة». والبردان: الفجر والعصر.", "صحيح البخاري 574، صحيح مسلم 635")
        )
        "الظهر" -> listOf(
            ReminderContent("سنة الظهر", "تذكّر سنة الظهر القبلية والبعدية، وتفصيل عددها وصفة أدائها بحسب المذهب.", "أحاديث الرواتب في صحيح البخاري وصحيح مسلم"),
            ReminderContent("وقت القيلولة", "اجعل لك نصيبًا من الراحة إن تيسّر، دون تفويت الصلاة أو تأخيرها.", "تذكير تنظيمي"),
            ReminderContent("الدعاء قبل الصلاة", "استقبل الصلاة بالتوبة والدعاء، ولا تنشغل حتى يخرج الوقت.", "تذكير تربوي")
        )
        "العصر" -> listOf(
            ReminderContent("فضل صلاة العصر", "قال ﷺ: «من ترك صلاة العصر فقد حبط عمله».", "صحيح البخاري 553"),
            ReminderContent("أربع قبل العصر", "ورد الترغيب في أربع ركعات قبل العصر: «رحم الله امرأ صلى قبل العصر أربعًا». وهي ليست من الرواتب المؤكدة.", "سنن أبي داود 1271؛ جامع الترمذي 430"),
            ReminderContent("حافظ على العصر", "اجعل صلاة العصر في مقدمة جدولك، ولا تؤخرها حتى يضيق وقتها.", "تذكير عملي")
        )
        "المغرب" -> listOf(
            ReminderContent("سنة المغرب", "تذكّر الركعتين بعد فرض المغرب، إن لم تكن هناك إقامة أو عذر.", "صحيح البخاري 1180، صحيح مسلم 729"),
            ReminderContent("لا تؤخر المغرب", "قال ﷺ: «لا تزال أمتي بخير ما لم يؤخروا المغرب حتى تشتبك النجوم».", "سنن أبي داود 418؛ حسّنه عدد من أهل العلم"),
            ReminderContent("أذكار المساء", "حافظ على أذكار المساء في وقتها، واقرأ ما تيسّر منها.", "تذكير بالأذكار")
        )
        else -> listOf(
            ReminderContent("سنة العشاء", "تذكّر الركعتين بعد فرض العشاء، ثم صلاة الوتر.", "صحيح البخاري 1180، صحيح مسلم 729"),
            ReminderContent("الوتر", "اجعل لك نصيبًا من الوتر؛ قال ﷺ: «اجعلوا آخر صلاتكم بالليل وترًا».", "صحيح البخاري 998، صحيح مسلم 751"),
            ReminderContent("قيام الليل", "ولو بركعتين خفيفتين؛ خذ من الليل نصيبًا بحسب استطاعتك.", "تذكير عملي")
        )
    }
    val chosen = if (slot == 0) common[0] else if (slot == 1) specific[0] else common[2]
    return chosen
}

private fun dailyLearning(date: LocalDate): ReminderContent {
    val lessons = listOf(
        ReminderContent("تعلّم: أركان الصلاة", "راجع أركان الصلاة كما يقررها المذهب الذي تتبعه؛ فعدّ الأركان وتفصيلها فيه خلاف فقهي.", "باب صفة الصلاة في كتب المذاهب الأربعة"),
        ReminderContent("تعلّم: فرائض الوضوء", "راجع فرائض الوضوء وشروطه ونواقضه على المذهب المالكي من قسم فقه الوضوء والصلاة داخل التطبيق.", "مختصر خليل وشروحه؛ ويحتاج التفصيل إلى مراجعة معتمدة"),
        ReminderContent("تعلّم: سنن الوضوء", "تعلّم سنن الوضوء وآدابه على المذهب المالكي، ولا تخلط بين الفرض والسنة والمندوب.", "كتب الفقه المالكي المعتمدة"),
        ReminderContent("سنة نبوية: السواك", "قال ﷺ: «لولا أن أشق على أمتي لأمرتهم بالسواك عند كل صلاة».", "صحيح البخاري 887، صحيح مسلم 252"),
        ReminderContent("تعلّم: سنن الصلاة", "راجع السنن القبلية والبعدية، وميّز بين الراتبة المؤكدة وغير المؤكدة بحسب المذهب.", "كتب الفقه المعتمدة في المذاهب الأربعة"),
        ReminderContent("تعلّم: أذكار الصلاة", "راجع أذكار ما بعد الصلاة، وابدأ بالاستغفار ثلاثًا ثم «اللهم أنت السلام ومنك السلام».", "صحيح مسلم 591"),
        ReminderContent("سنة الاثنين والخميس", "من هدي النبي ﷺ صيام الاثنين، وورد في الحديث أنه يوم وُلد فيه ويوم بُعث أو أُنزل عليه فيه.", "صحيح مسلم 1162")
    )
    return lessons[(date.toEpochDay() % lessons.size).toInt()]
}

private fun nightContent(kind: String): ReminderContent = if (kind == "قيام الليل")
    ReminderContent("حان وقت قيام الليل", "قال رسول الله ﷺ: «أفضل الصلاة بعد الفريضة صلاة الليل». صلّ ما تيسّر لك ولو ركعتين.", "صحيح مسلم 1163")
else
    ReminderContent("تذكير بصلاة الوتر", "قال رسول الله ﷺ: «اجعلوا آخر صلاتكم بالليل وترًا». لا تنس الوتر إن لم تكن أوترت.", "صحيح البخاري 998، صحيح مسلم 751")

object PrayerNotifications {
    const val CHANNEL = "salati_prayers"
    const val GAZA_CHANNEL = "salati_gaza"
    private const val GAZA_NOTIFICATION_ID = 980001
    fun createChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "تنبيهات الصلاة والعبادات", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تذكيرات متنوعة للصلاة والسنن والذكر وقيام الليل"
            })
            manager.createNotificationChannel(NotificationChannel(GAZA_CHANNEL, "تذكير لأهل غزة", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "تذكير يومي بالدعاء لأهل غزة"
            })
        }
    }

    fun scheduleGazaReminder(context: Context, enabled: Boolean = context.getSharedPreferences("salati", Context.MODE_PRIVATE).getBoolean("notify_gaza", true)) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .putExtra("id", GAZA_NOTIFICATION_ID)
            .putExtra("gaza_reminder", true)
            .putExtra("message", "لا تنسَ أهل غزة من دعائك؛ اسأل الله أن يفرّج كربهم ويحفظهم. وانصر المظلوم بما تستطيع من دعاء وعمل مشروع.")
        val pending = PendingIntent.getBroadcast(
            context, GAZA_NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (!enabled) {
            alarm.cancel(pending)
            pending.cancel()
            return
        }
        val now = ZonedDateTime.now()
        var first = now.toLocalDate().atTime(20, 0).atZone(now.zone)
        if (!first.isAfter(now)) first = first.plusDays(1)
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            first.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }

    fun schedule(context: Context, lat: Double, lon: Double) {
        val prefs = context.getSharedPreferences("salati", Context.MODE_PRIVATE)
        val notifyBefore = prefs.getBoolean("notify_before", true)
        val notifyAt = prefs.getBoolean("notify_at", true)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        fun setReminder(at: Long, id: Int, body: String, tag: String, isEnabled: Boolean) {
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
                .putExtra("message", body).putExtra("id", id).putExtra("tag", tag)
            val pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            if (!isEnabled || at <= now + 5_000L) {
                alarm.cancel(pi); pi.cancel()
            } else {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                    else alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
            }
        }
        for (offset in 0..7) {
            val date = LocalDate.now(zone).plusDays(offset.toLong())
            val prayers = PrayerTimes.forDate(date, lat, lon, zone)
            prayers.forEachIndexed { index, prayer ->
                // Three reminders before each prayer and one gentle follow-up after it.
                val offsets = listOf(60, 30, 10)
                offsets.forEachIndexed { slot, minutes ->
                    val item = contentForPrayer(prayer.name, slot)
                    val id = (date.toEpochDay() % 100000).toInt() * 10000 + index * 100 + slot + 1
                    setReminder(prayer.time - minutes * 60_000L, id, item.title + "\n" + item.body + "\nالمصدر: " + item.source, item.title, notifyBefore)
                }
                val adhanDua = ReminderContent("الدعاء بعد الأذان", "قل: «اللهم رب هذه الدعوة التامة، والصلاة القائمة، آت محمدًا الوسيلة والفضيلة، وابعثه مقامًا محمودًا الذي وعدته».", "صحيح البخاري 614")
                val adhanId = (date.toEpochDay() % 100000).toInt() * 10000 + index * 100 + 80
                setReminder(prayer.time + 60_000L, adhanId, adhanDua.title + "\\n" + adhanDua.body + "\\nالمصدر: " + adhanDua.source, adhanDua.title, notifyAt)
                val after = ReminderContent("أذكار ما بعد الصلاة", "لا تنس الاستغفار ثلاثًا، ثم: «اللهم أنت السلام ومنك السلام تباركت يا ذا الجلال والإكرام».", "صحيح مسلم 591")
                val afterId = (date.toEpochDay() % 100000).toInt() * 10000 + index * 100 + 90
                setReminder(prayer.time + 7 * 60_000L, afterId, after.title + "\n" + after.body + "\nالمصدر: " + after.source, after.title, notifyAt)
            }
            val learning = dailyLearning(date)
            val learningAt = date.atTime(11, 0).atZone(zone).toInstant().toEpochMilli()
            val learningId = (date.toEpochDay() % 100000).toInt() * 10000 + 950
            setReminder(learningAt, learningId, learning.title + "\\n" + learning.body + "\\nالمصدر: " + learning.source, learning.title, notifyBefore)
            // Night reminders: middle of the night and the final third, using Maghrib-to-Fajr interval.
            val maghrib = prayers.first { it.name == "المغرب" }.time
            val fajr = PrayerTimes.forDate(date.plusDays(1), lat, lon, zone).first().time
            val nightLength = fajr - maghrib
            val qiyamAt = maghrib + nightLength * 2 / 3
            val witrAt = maghrib + nightLength * 5 / 6
            val qiyam = nightContent("قيام الليل")
            val witr = nightContent("الوتر")
            val base = (date.toEpochDay() % 100000).toInt() * 10000 + 800
            setReminder(qiyamAt, base + 1, qiyam.title + "\n" + qiyam.body + "\nالمصدر: " + qiyam.source, qiyam.title, notifyAt)
            setReminder(witrAt, base + 2, witr.title + "\n" + witr.body + "\nالمصدر: " + witr.source, witr.title, notifyAt)
            if (date.dayOfWeek == DayOfWeek.MONDAY || date.dayOfWeek == DayOfWeek.THURSDAY) {
                val fasting = ReminderContent("تذكير بصيام " + if (date.dayOfWeek == DayOfWeek.MONDAY) "الاثنين" else "الخميس", "تذكير بسنة صيام يوم الاثنين والخميس لمن أراد واستطاع.", "صحيح مسلم 1162 في صيام الاثنين؛ وأحاديث عرض الأعمال يوم الاثنين والخميس في جامع الترمذي 747")
                setReminder(fajr - 45 * 60_000L, base + 3, fasting.title + "\n" + fasting.body + "\nالمصدر: " + fasting.source, fasting.title, notifyBefore)
            }
        }
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerNotifications.createChannel(context)
        val id = intent.getIntExtra("id", 1)
        val message = intent.getStringExtra("message") ?: "تذكير بالعبادة"
        val isGazaReminder = intent.getBooleanExtra("gaza_reminder", false)
        val channel = if (isGazaReminder) PrayerNotifications.GAZA_CHANNEL else PrayerNotifications.CHANNEL
        val title = if (isGazaReminder) "تذكير لأهل غزة" else "الصَّلَاةُ جَامِعَةٌ"
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(message.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = context.getSharedPreferences("salati", Context.MODE_PRIVATE)
        PrayerNotifications.scheduleGazaReminder(context)
        if (p.contains("lat") && p.contains("lon")) PrayerNotifications.schedule(context, p.getFloat("lat", 30.0444f).toDouble(), p.getFloat("lon", 31.2357f).toDouble())
    }
}