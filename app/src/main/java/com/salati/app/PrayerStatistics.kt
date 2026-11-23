package com.salati.app

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val StatsGreen = Color(0xFF164B3A)
private val StatsPale = Color(0xFFE5EBDD)
private val PrayerStatsNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")

private data class PrayerDayRecord(val date: LocalDate, val status: String)

@Composable
fun PrayerStatisticsScreen(
    prefs: SharedPreferences,
    today: LocalDate,
    prayerBase: Int,
    qadaDoneByPrayer: Map<String, Int>,
    missedByPrayer: Map<String, Int>,
    fastBase: Int,
    qadaFastsDone: Int
) {
    val allPrefs = prefs.all
    val records = remember(allPrefs) {
        allPrefs.entries.mapNotNull { entry ->
            if (!entry.key.startsWith("status_")) return@mapNotNull null
            val rest = entry.key.removePrefix("status_")
            val splitAt = rest.lastIndexOf('_')
            if (splitAt <= 0) return@mapNotNull null
            val date = runCatching { LocalDate.parse(rest.substring(0, splitAt)) }.getOrNull() ?: return@mapNotNull null
            val status = entry.value as? String ?: return@mapNotNull null
            if (status == "لم أسجل بعد") return@mapNotNull null
            PrayerDayRecord(date, status)
        }.sortedBy { it.date }
    }
    val counts = records.groupingBy { it.status }.eachCount()
    val lastSevenStart = today.minusDays(6)
    val recent = records.filter { !it.date.isBefore(lastSevenStart) && !it.date.isAfter(today) }
    val recentCounts = recent.groupingBy { it.status }.eachCount()
    val qadaPrayerTotal = qadaDoneByPrayer.values.sum()
    val qadaPrayerRemaining = if (prayerBase >= 0) (prayerBase + missedByPrayer.values.sum() - qadaPrayerTotal).coerceAtLeast(0) else null
    val qadaFastRemaining = if (fastBase >= 0) (fastBase - qadaFastsDone).coerceAtLeast(0) else null
    val prayerSnapshots = remember(allPrefs) {
        allPrefs.entries.mapNotNull { (key, value) ->
            if (!key.startsWith("qada_prayer_snapshot_")) return@mapNotNull null
            val date = runCatching { LocalDate.parse(key.removePrefix("qada_prayer_snapshot_")) }.getOrNull() ?: return@mapNotNull null
            (value as? Int)?.let { date to it }
        }.sortedBy { it.first }
    }
    val fastSnapshots = remember(allPrefs) {
        allPrefs.entries.mapNotNull { (key, value) ->
            if (!key.startsWith("qada_fast_snapshot_")) return@mapNotNull null
            val date = runCatching { LocalDate.parse(key.removePrefix("qada_fast_snapshot_")) }.getOrNull() ?: return@mapNotNull null
            (value as? Int)?.let { date to it }
        }.sortedBy { it.first }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("إحصائياتك", style = MaterialTheme.typography.headlineMedium, color = StatsGreen, fontWeight = FontWeight.Bold)
        Text("مراجعة هادئة لما سجلته أنت في التطبيق، وليست تقييمًا لقبول العبادة أو ميزانًا للتقوى.", color = Color(0xFF77736B))
        Card(colors = CardDefaults.cardColors(containerColor = StatsPale)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("متابعة الصلوات اليومية", style = MaterialTheme.typography.titleLarge, color = StatsGreen, fontWeight = FontWeight.Bold)
                Text("إجمالي الصلوات التي سجلت لها حالة: ${records.size}", fontWeight = FontWeight.SemiBold)
                Text("آخر 7 أيام: ${recent.size} صلاة مسجلة من أصل 35 موعدًا.")
                StatsCountRow("في الوقت جماعة", counts["في الوقت جماعة"] ?: 0, recentCounts["في الوقت جماعة"] ?: 0)
                StatsCountRow("في الوقت منفردًا", counts["في الوقت منفردًا"] ?: 0, recentCounts["في الوقت منفردًا"] ?: 0)
                StatsCountRow("بعد الوقت", counts["بعد الوقت"] ?: 0, recentCounts["بعد الوقت"] ?: 0)
                StatsCountRow("لم أصلِّ", counts["لم أصلِّ"] ?: 0, recentCounts["لم أصلِّ"] ?: 0)
                Text("الأيام غير المسجلة لا نفترض فيها أنك صليت أو لم تصلِّ.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6D716A))
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("متابعة قضاء الصلاة", style = MaterialTheme.typography.titleLarge, color = StatsGreen, fontWeight = FontWeight.Bold)
                Text("إجمالي ما سجلته من صلوات مقضية: $qadaPrayerTotal فرض", fontWeight = FontWeight.SemiBold)
                Text(if (qadaPrayerRemaining == null) "لم تُعتمد حسبة الرصيد بعد." else "المتبقي حسب آخر حسبة: $qadaPrayerRemaining فرض")
                PrayerStatsNames.forEach { name -> Text("$name: ${qadaDoneByPrayer[name] ?: 0} مقضية") }
                if (prayerSnapshots.size >= 2) {
                    val first = prayerSnapshots.first()
                    val last = prayerSnapshots.last()
                    val delta = last.second - first.second
                    Text("التغير منذ ${first.first}: ${if (delta >= 0) "زادت" else "انخفضت"} بمقدار ${kotlin.math.abs(delta)} فرض. آخر تسجيل: ${last.first}.", color = StatsGreen, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("بدأنا حفظ سجل التقدم من الآن؛ بعد مرور أيام ستظهر مقارنة التغير. لا توجد بيانات تاريخية أقدم محفوظة.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF77736B))
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("متابعة قضاء الصيام", style = MaterialTheme.typography.titleLarge, color = StatsGreen, fontWeight = FontWeight.Bold)
                Text("أيام الصيام التي سجلت قضاءها: $qadaFastsDone")
                Text(if (qadaFastRemaining == null) "لم تُعتمد حسبة قضاء الصيام بعد." else "المتبقي حسب آخر حسبة: $qadaFastRemaining يوم")
                if (fastSnapshots.size >= 2) {
                    val delta = fastSnapshots.last().second - fastSnapshots.first().second
                    Text("التغير منذ ${fastSnapshots.first().first}: ${if (delta >= 0) "زادت" else "انخفضت"} بمقدار ${kotlin.math.abs(delta)} يوم.")
                } else {
                    Text("سيظهر سجل التقدم بعد تسجيل قضاء الصيام عبر أيام مختلفة.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF77736B))
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = StatsPale)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المحبة قبل عدّ الأعمال", style = MaterialTheme.typography.titleLarge, color = StatsGreen, fontWeight = FontWeight.Bold)
                Text("سأل رجلٌ النبي ﷺ عن الساعة، فقال: «ما أعددتَ لها؟» قال: ما أعددتُ لها من كثير صلاة ولا صوم ولا صدقة، ولكني أحب الله ورسوله. فقال ﷺ: «أنت مع من أحببت».")
                Text("صحيح البخاري (6171)، وصحيح مسلم (2639).", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6D716A))
                Text("هذه الإحصائيات وسيلة للمراجعة والتنظيم، لا للمقارنة بين الناس ولا للحكم على الإخلاص أو القبول. والحديث لا يعني الاستغناء عن الفرائض؛ بل يذكّر بعِظم محبة الله ورسوله.")
            }
        }
    }
}

@Composable
private fun StatsCountRow(label: String, total: Int, recent: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text("$total  •  آخر 7 أيام: $recent", color = StatsGreen, fontWeight = FontWeight.SemiBold)
    }
}
