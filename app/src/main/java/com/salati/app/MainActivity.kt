package com.salati.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import java.time.*
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = Color(0xFF164B3A)
private val Cream = Color(0xFFF6F2E9)
private val PaleGreen = Color(0xFFE5EBDD)
private val PrayerNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
private val PrayerStatuses = listOf("لم أسجل بعد", "في الوقت جماعة", "في الوقت منفردًا", "بعد الوقت", "لم أصلِّ")

class MainActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { getLocationAndSchedule(); PrayerNotifications.scheduleGazaReminder(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrayerNotifications.createChannel(this)
        setContent { SalatiScreen(onFirstLaunchAccepted = { requestLocationPermissions() }) }
        if (getSharedPreferences("salati", MODE_PRIVATE).getBoolean("intro_seen", false)) requestLocationPermissions()
    }
    private fun requestLocationPermissions() {
        val needed = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (needed.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            permissions.launch(needed)
        } else {
            getLocationAndSchedule()
            PrayerNotifications.scheduleGazaReminder(this)
        }
    }
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) getLocationAndSchedule()
    }
    private fun getLocationAndSchedule() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        try {
            LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { loc ->
                    val prefs = getSharedPreferences("salati", MODE_PRIVATE)
                    val lat = loc?.latitude ?: prefs.getFloat("lat", 30.0444f).toDouble()
                    val lon = loc?.longitude ?: prefs.getFloat("lon", 31.2357f).toDouble()
                    prefs.edit().putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).apply()
                    PrayerNotifications.schedule(this, lat, lon)
                }
        } catch (_: SecurityException) { }
    }
}

@Composable
private fun GuideCard(title: String, items: List<SunnahItem>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Green)
            items.forEach { item ->
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(item.category, color = Color(0xFF56715F), style = MaterialTheme.typography.labelMedium)
                if (item.detail.isNotBlank()) Text(item.detail)
                if (item.evidence.isNotBlank()) Text("الدليل: " + item.evidence, color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(color = Color(0xFFECE8DE))
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = { onValue(it.filter(Char::isDigit).take(4)) },
        label = { Text(label) }, modifier = modifier,
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun CounterCard(title: String, remaining: Int, unit: String, actionLabel: String, onAdd: () -> Unit, onUndo: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
            Text(remaining.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(unit + " متبقية")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(actionLabel) }
                OutlinedButton(onClick = onUndo) { Text("تراجع") }
            }
        }
    }
}

@Composable
fun SalatiScreen(onFirstLaunchAccepted: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("salati", 0) }
    var introSeen by remember { mutableStateOf(prefs.getBoolean("intro_seen", false)) }
    var qadaPrayersDone by remember { mutableIntStateOf(prefs.getInt("prayers_done", 0)) }
    var qadaFastsDone by remember { mutableIntStateOf(prefs.getInt("fasts_done", 0)) }
    val qadaDoneByPrayer = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>().apply { PrayerNames.forEach { put(it, prefs.getInt("qada_done_" + it, 0)) } } }
    val missedByPrayer = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>().apply { PrayerNames.forEach { put(it, prefs.getInt("missed_" + it, 0)) } } }
    var missedSinceInstall by remember { mutableIntStateOf(prefs.getInt("missed_since_install", 0)) }
    var prayerBase by remember { mutableIntStateOf(prefs.getInt("prayer_base", -1)) }
    var fastBase by remember { mutableIntStateOf(prefs.getInt("fast_base", -1)) }
    var mainTab by remember { mutableStateOf("الرئيسية") }
    var calculatorView by remember { mutableStateOf("الحاسبة") }
    var selectedPrayerGuide by remember { mutableStateOf("الفجر") }
    var sunnahTab by remember { mutableStateOf("سنن الصلاة") }
    var wuduFiqhTopic by remember { mutableStateOf("أركان الوضوء") }
    var prayerFiqhTopic by remember { mutableStateOf("شروط الصلاة") }
    var fiqhMadhhab by remember { mutableStateOf("الحنفي") }
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notifyBefore by remember { mutableStateOf(prefs.getBoolean("notify_before", true)) }
    var notifyAt by remember { mutableStateOf(prefs.getBoolean("notify_at", true)) }
    var notifyGaza by remember { mutableStateOf(prefs.getBoolean("notify_gaza", true)) }
    var tasbeehCount by remember { mutableIntStateOf(prefs.getInt("tasbeeh_count", 0)) }
    var fastSection by remember { mutableStateOf("رمضان") }
    var fastRamadanCount by remember { mutableIntStateOf(prefs.getInt("fast_ramadan_count", 0)) }
    var fastShawwalCount by remember { mutableIntStateOf(prefs.getInt("fast_shawwal_count", 0)) }
    LaunchedEffect(Unit) { while (true) { delay(1_000L); clockNow = System.currentTimeMillis() } }
    val today = Instant.ofEpochMilli(clockNow).atZone(ZoneId.systemDefault()).toLocalDate()
    var duhaDone by remember(today) { mutableStateOf(prefs.getBoolean("duha_" + today, false)) }
    val sunnahSlots = listOf("قبل الفجر" to 2, "قبل الظهر" to 4, "بعد الظهر" to 2, "بعد المغرب" to 2, "بعد العشاء" to 2)
    val sunnahDoneBySlot = remember(today) {
        androidx.compose.runtime.mutableStateMapOf<String, Int>().apply {
            sunnahSlots.forEach { (slot, _) -> put(slot, prefs.getInt("sunnah_slot_" + today + "_" + slot, 0)) }
        }
    }
    val sunnahRakaatCount = sunnahDoneBySlot.values.sum()
    var fastLogged by remember(today) { mutableStateOf(prefs.getString("fast_type_" + today, "") ?: "") }
    var actionFeedback by remember { mutableStateOf("") }
    LaunchedEffect(actionFeedback) {
        if (actionFeedback.isNotBlank()) {
            delay(1_400L)
            actionFeedback = ""
        }
    }
    val tomorrow = today.plusDays(1)
    val lat = prefs.getFloat("lat", 30.0444f).toDouble()
    val lon = prefs.getFloat("lon", 31.2357f).toDouble()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            PrayerNotifications.schedule(context, lat, lon)
            PrayerNotifications.scheduleGazaReminder(context, notifyGaza)
        }
    }
    val todayTimes = remember(today, lat, lon) { PrayerTimes.forDate(today, lat, lon, ZoneId.systemDefault()) }
    val tomorrowTimes = remember(tomorrow, lat, lon) { PrayerTimes.forDate(tomorrow, lat, lon, ZoneId.systemDefault()) }
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale("ar"))
    val nightStart = todayTimes.first { it.name == "المغرب" }.time
    val nightEnd = tomorrowTimes.first { it.name == "الفجر" }.time
    val lastThirdStart = nightStart + ((nightEnd - nightStart) * 2L / 3L)
    val nightWindow = "الثلث الأخير تقريبًا: " + Instant.ofEpochMilli(lastThirdStart).atZone(ZoneId.systemDefault()).format(timeFormatter)
    val nextPrayer = (todayTimes + tomorrowTimes).firstOrNull { it.time > clockNow } ?: tomorrowTimes.first()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
    val nextDuration = ((nextPrayer.time - clockNow).coerceAtLeast(0L) / 1000L)
    val countdown = "%02d:%02d:%02d".format(nextDuration / 3600, (nextDuration % 3600) / 60, nextDuration % 60)

    val dailyStatus: SnapshotStateMap<String, String> = remember(today) {
        androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
            PrayerNames.forEach { name -> put(name, prefs.getString("status_" + today + "_" + name, "لم أسجل بعد") ?: "لم أسجل بعد") }
        }
    }
    val qadaPrayerTotalForSnapshot = qadaDoneByPrayer.values.sum()
    LaunchedEffect(qadaPrayerTotalForSnapshot, qadaFastsDone, today) {
        prefs.edit()
            .putInt("qada_prayer_snapshot_" + today, qadaPrayerTotalForSnapshot)
            .putInt("qada_fast_snapshot_" + today, qadaFastsDone)
            .apply()
    }

    var birthDay by remember { mutableStateOf("") }
    var birthMonth by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }
    var pubertyAge by remember { mutableStateOf(prefs.getInt("puberty_age_hijri", 15).toString()) }
    var estimate by remember { mutableStateOf<QadaEstimate?>(null) }
    var calcError by remember { mutableStateOf("") }
    var deductOneYear by remember { mutableStateOf(false) }
    var performedPrayers by remember { mutableStateOf("0") }
    var partialFastDays by remember { mutableStateOf("0") }
    var ramadanChecks by remember { mutableStateOf(setOf<Int>()) }

    val prayerRemaining = if (prayerBase >= 0) (prayerBase + missedByPrayer.values.sum() - qadaDoneByPrayer.values.sum()).coerceAtLeast(0) else 0
    val fastRemaining = if (fastBase >= 0) (fastBase - qadaFastsDone).coerceAtLeast(0) else 0

    if (!introSeen) {
        FirstLaunchNotice {
            prefs.edit().putBoolean("intro_seen", true).apply()
            introSeen = true
            onFirstLaunchAccepted()
        }
    } else {
    Surface(Modifier.fillMaxSize(), color = Cream) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("الصَّلَاةُ جَامِعَةٌ", style = MaterialTheme.typography.headlineLarge, color = Green, fontWeight = FontWeight.Bold)
                Text("\u2067﴿إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا﴾ [النساء: 103]\u2069", color = Color(0xFF6D716A))
            }
            if (actionFeedback.isNotBlank()) {
                item { Text(actionFeedback, color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("الرئيسية", "الصلاة", "السنن", "قيام الليل", "الصوم", "القضاء", "الإحصائيات", "الأذكار", "الأدعية", "فقه الوضوء", "فقه الصلاة", "تدبر القرآن", "الإعدادات").forEach { tab ->
                        FilterChip(selected = mainTab == tab, onClick = { mainTab = tab }, label = { Text(tab) })
                    }
                }
            }

            if (mainTab == "الرئيسية") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Green)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("الصَّلَاةُ جَامِعَةٌ", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(today.format(dateFormatter), color = Color.White.copy(alpha = 0.85f))
                            Text("الصلاة القادمة: " + nextPrayer.name + " — " + Instant.ofEpochMilli(nextPrayer.time).atZone(ZoneId.systemDefault()).format(timeFormatter), color = Color.White)
                            Text("متبقي " + countdown, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item { Text("أعمال اليوم", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold) }
                item {
                    Card(onClick = { mainTab = "الصلاة" }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("الصلاة المفروضة", color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("مواقيت الفجر والظهر والعصر والمغرب والعشاء، وتسجيل أداء كل صلاة")
                        }
                    }
                }
                item {
                    Card(onClick = { mainTab = "السنن" }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("السنن الرواتب والضحى", color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("متابعة ركعات السنن اليومية والضحى")
                        }
                    }
                }
                item {
                    Card(onClick = { mainTab = "قيام الليل" }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("قيام الليل", color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(nightWindow + " — تسجيل القيام والقراءة")
                        }
                    }
                }
                item {
                    Card(onClick = { mainTab = "الصوم" }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("الصوم", color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("رمضان، شوال، صيام التطوع، والأيام الفائتة")
                        }
                    }
                }
                item {
                    Card(onClick = { mainTab = "القضاء" }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("الفوائت والقضاء", color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("متابعة رصيد الصلوات والصيام الفائت")
                        }
                    }
                }
                item { Text("كل التسجيلات والإحصائيات في هذا الإصدار تحفظ على جهازك فقط.", color = Color(0xFF6D716A), style = MaterialTheme.typography.bodySmall) }
            } else if (mainTab == "الصلاة") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Green)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("الصلاة القادمة", color = Color.White.copy(alpha = 0.85f))
                            Text(nextPrayer.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(Instant.ofEpochMilli(nextPrayer.time).atZone(ZoneId.systemDefault()).format(timeFormatter), color = Color.White)
                            Text("متبقي " + countdown, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("مواقيت الصلاة اليوم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                            Text(today.format(dateFormatter), color = Color(0xFF6D716A))
                            todayTimes.forEach { prayer ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(prayer.name, fontWeight = FontWeight.SemiBold)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(Instant.ofEpochMilli(prayer.time).atZone(ZoneId.systemDefault()).format(timeFormatter))
                                        TextButton(onClick = { selectedPrayerGuide = prayer.name; sunnahTab = "سنن الصلاة"; mainTab = "السنن" }) { Text("سنة صلاة " + prayer.name, style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFD4DDCE))
                                if (prayer.name == "العشاء") {
                                    Column(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("قيام الليل — نافلة", color = Green, fontWeight = FontWeight.SemiBold)
                                        Text(nightWindow + "، وهو تقدير للثلث الأخير من الليل.")
                                        TextButton(onClick = { mainTab = "قيام الليل" }) { Text("سنن قيام الليل ومتابعتها") }
                                    }
                                }
                            }
                            Text("المواقيت محسوبة محليًا بطريقة تقريبية؛ راجعها مع التقويم الرسمي.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF755F45))
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("صلاة الضحى", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("وقتها من ارتفاع الشمس إلى قبيل الزوال. سجّلها بعد أدائها.")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (duhaDone) "تم تسجيل الضحى اليوم" else "لم تسجل الضحى اليوم")
                                Button(onClick = {
                                    duhaDone = !duhaDone
                                    actionFeedback = "أستغفر الله"
                                    prefs.edit().putBoolean("duha_" + today, duhaDone).apply()
                                }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(if (duhaDone) "تراجع" else "سجل الضحى") }
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تسجيل صلوات اليوم", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("حدد حالة كل صلاة. اختيار «لم أصلِّ» يضيفها إلى رصيد الفوائت.")
                            PrayerNames.forEach { name ->
                                var expanded by remember(today, name) { mutableStateOf(false) }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(name, fontWeight = FontWeight.SemiBold)
                                    Box {
                                        TextButton(onClick = { expanded = true }) { Text(dailyStatus[name] ?: "لم أسجل بعد") }
                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            PrayerStatuses.forEach { status ->
                                                DropdownMenuItem(text = { Text(status) }, onClick = {
                                                    val old = dailyStatus[name] ?: "لم أسجل بعد"
                                                    dailyStatus[name] = status
                                                    if (status != "لم أسجل بعد") actionFeedback = "أستغفر الله"
                                                    prefs.edit().putString("status_" + today + "_" + name, status).apply()
                                                    if (old != "لم أصلِّ" && status == "لم أصلِّ") {
                                                        missedSinceInstall++
                                                        missedByPrayer[name] = (missedByPrayer[name] ?: 0) + 1
                                                        prefs.edit().putInt("missed_since_install", missedSinceInstall).putInt("missed_" + name, missedByPrayer[name] ?: 0).apply()
                                                    } else if (old == "لم أصلِّ" && status != "لم أصلِّ") {
                                                        missedSinceInstall = (missedSinceInstall - 1).coerceAtLeast(0)
                                                        missedByPrayer[name] = ((missedByPrayer[name] ?: 0) - 1).coerceAtLeast(0)
                                                        prefs.edit().putInt("missed_since_install", missedSinceInstall).putInt("missed_" + name, missedByPrayer[name] ?: 0).apply()
                                                    }
                                                    expanded = false
                                                })
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFECE8DE))
                            }
                            Text("الفوائت المسجلة منذ استخدام التطبيق: " + missedSinceInstall + " فرض", color = Green, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("رصيدك الحالي", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text(if (prayerBase < 0) "ابدأ من الحاسبة لتحديد رصيدك." else "قضاء الصلاة: " + prayerRemaining + " فرض")
                            Text(if (fastBase < 0) "لم يتم حساب قضاء الصيام بعد." else "قضاء الصيام: " + fastRemaining + " يوم")
                            TextButton(onClick = { calculatorView = "الحاسبة"; mainTab = "القضاء" }) { Text("ابدأ الحاسبة") }
                        }
                    }
                }
            } else if (mainTab == "القضاء" && calculatorView == "الحاسبة") {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = calculatorView == "الحاسبة", onClick = { calculatorView = "الحاسبة" }, label = { Text("الحاسبة") })
                        FilterChip(selected = calculatorView == "القضاء", onClick = { calculatorView = "القضاء"; mainTab = "القضاء" }, label = { Text("القضاء") })
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EDDF))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("\u200F﴿وَالَّذِينَ جَاهَدُوا فِينَا لَنَهْدِيَنَّهُمْ سُبُلَنَا﴾\u200F", color = Green, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("العنكبوت: 69", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("حاسبة بداية التكليف", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("أدخل تاريخ ميلادك، وحدد سن البلوغ الذي تريد أن يبدأ منه الحساب. التطبيق لا يفرض سنًا واحدًا على الجميع.")
                            OutlinedTextField(value = pubertyAge, onValueChange = { pubertyAge = it.filter(Char::isDigit).take(2); pubertyAge.toIntOrNull()?.let { age -> if (age in 1..40) prefs.edit().putInt("puberty_age_hijri", age).apply() } }, label = { Text("سن البلوغ بالسنوات الهجرية (اختيارك)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            Text("أدخل السن الذي تراه منطبقًا على حالتك وفق ما تعلمه أو ما ثبت لديك. هذا اختيار شخصي للحساب وليس حكمًا شرعيًا من التطبيق.", color = Color(0xFF755F45), style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NumberField("اليوم", birthDay, { birthDay = it }, Modifier.weight(1f))
                                NumberField("الشهر", birthMonth, { birthMonth = it }, Modifier.weight(1f))
                                NumberField("السنة", birthYear, { birthYear = it }, Modifier.weight(1.4f))
                            }
                            Button(onClick = {
                                calcError = ""
                                try {
                                    val date = LocalDate.of(birthYear.toInt(), birthMonth.toInt(), birthDay.toInt())
                                    val selectedAge = pubertyAge.toIntOrNull() ?: throw IllegalArgumentException("أدخل سن البلوغ بالأرقام")
                                    require(selectedAge in 1..40) { "سن البلوغ يجب أن يكون بين 1 و40 سنة" }
                                    prefs.edit().putInt("puberty_age_hijri", selectedAge).apply()
                                    estimate = QadaCalculator.calculate(date, today, taklifAgeYears = selectedAge)
                                    ramadanChecks = estimate!!.ramadans.filter { prefs.getBoolean("ramadan_" + it.hijriYear, false) }.map { it.hijriYear }.toSet()
                                } catch (e: Exception) {
                                    calcError = e.message ?: "راجع تاريخ الميلاد والبيانات المدخلة."
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) { Text("احسب بداية التكليف") }
                            if (calcError.isNotBlank()) Text(calcError, color = Color(0xFFB3261E))
                            estimate?.let { result ->
                                HorizontalDivider()
                                Text("تاريخ الميلاد الهجري: " + QadaCalculator.hijriLabel(result.birthHijri), fontWeight = FontWeight.SemiBold)
                                Text("بداية الحساب حسب السن الذي اخترته (" + pubertyAge + " سنة هجرية): " + QadaCalculator.hijriLabel(result.taklifHijri), fontWeight = FontWeight.SemiBold)
                                Text("الموافق ميلاديًا: " + result.taklifDate.toString())
                                Text("أيام الحساب حتى اليوم: " + result.prayerDays)
                                Text("إجمالي الفروض قبل الخصم: " + result.totalPrayers, color = Green, fontWeight = FontWeight.Bold)
                                HorizontalDivider()
                                Text("التقويم المستخدم: ضبط مصري مبدئي بإضافة يوم واحد لجميع التحويلات. هذا ضبط حسابي تقريبي وليس سجلًا رسميًا لرؤية الهلال؛ راجع التاريخ المحلي عند الحاجة.", color = Color(0xFF755F45), style = MaterialTheme.typography.bodySmall)
                                Text("التخفيف وتقدير ما سبق أداؤه", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = deductOneYear, onCheckedChange = { deductOneYear = it })
                                    Text("خصم سنة هجرية تقديرية من الصلوات (1770 فرضًا)")
                                }
                                OutlinedTextField(value = performedPrayers, onValueChange = { performedPrayers = it.filter(Char::isDigit) }, label = { Text("صلوات سابقة تتذكر أنك أديتها") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                HorizontalDivider()
                                Text("رمضانات الفترة المحسوبة", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                                Text("علّم كل رمضان صمته كاملًا. الأيام الجزئية أدخلها في الحقل التالي.")
                                result.ramadans.forEach { period ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val checked = ramadanChecks.contains(period.hijriYear)
                                        Checkbox(checked = checked, onCheckedChange = { on ->
                                            ramadanChecks = if (on) ramadanChecks + period.hijriYear else ramadanChecks - period.hijriYear
                                            prefs.edit().putBoolean("ramadan_" + period.hijriYear, on).apply()
                                        })
                                        Text("رمضان " + period.hijriYear + " هـ (" + period.days + " يوم)")
                                    }
                                }
                                OutlinedTextField(value = partialFastDays, onValueChange = { partialFastDays = it.filter(Char::isDigit) }, label = { Text("أيام صمتها متفرقة خارج الرمضانات الكاملة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                val checkedRamadanDays = result.ramadans.filter { ramadanChecks.contains(it.hijriYear) }.sumOf { it.days }
                                val prayerDeduction = (if (deductOneYear) result.oneHijriYearDays * 5 else 0) + (performedPrayers.toIntOrNull() ?: 0)
                                val newPrayerBase = (result.totalPrayers - prayerDeduction).coerceAtLeast(0).toInt()
                                val newFastBase = (result.totalRamadanDays - checkedRamadanDays - (partialFastDays.toIntOrNull() ?: 0)).coerceAtLeast(0)
                                Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("النتيجة التقديرية", fontWeight = FontWeight.Bold, color = Green)
                                        Text("قضاء الصلوات: " + newPrayerBase + " فرض")
                                        Text("إجمالي أيام رمضان: " + result.totalRamadanDays)
                                        Text("أيام الصيام المتبقية: " + newFastBase + " يوم")
                                    }
                                }
                                Button(onClick = {
                                    prayerBase = newPrayerBase
                                    fastBase = newFastBase
                                    prefs.edit().putInt("prayer_base", prayerBase).putInt("fast_base", fastBase).apply()
                                    mainTab = "القضاء"
                                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("اعتماد النتيجة وبدء المتابعة") }
                            }
                        }
                    }
                }
            } else if (mainTab == "القضاء" && calculatorView == "القضاء") {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = calculatorView == "الحاسبة", onClick = { calculatorView = "الحاسبة"; mainTab = "القضاء" }, label = { Text("الحاسبة") })
                        FilterChip(selected = calculatorView == "القضاء", onClick = { calculatorView = "القضاء" }, label = { Text("القضاء") })
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EDDF))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("الْمُجَاهِدُ مَنْ جَاهَدَ نَفْسَهُ فِي طَاعَةِ اللَّهِ", color = Green, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("رواه أحمد (23967)، وأصل الحديث في سنن الترمذي (1621)، وقال الترمذي: حسن صحيح.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    if (prayerBase < 0 || fastBase < 0) {
                        Card { Column(Modifier.padding(16.dp)) { Text("لم يتم إعداد رصيد القضاء بعد."); Button(onClick = { calculatorView = "الحاسبة"; mainTab = "القضاء" }) { Text("افتح الحاسبة") } } }
                    } else {
                        Text("قضاء كل صلاة على حدة", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                        PrayerNames.forEachIndexed { index, prayer ->
                            val baseForPrayer = (prayerBase / 5) + if (index < prayerBase % 5) 1 else 0
                            val missed = missedByPrayer[prayer] ?: 0
                            val done = qadaDoneByPrayer[prayer] ?: 0
                            val remaining = (baseForPrayer + missed - done).coerceAtLeast(0)
                            CounterCard("قضاء " + prayer, remaining, "فرض", "سجّلت قضاء " + prayer,
                                onAdd = { qadaDoneByPrayer[prayer] = done + 1; qadaPrayersDone = qadaDoneByPrayer.values.sum(); prefs.edit().putInt("qada_done_" + prayer, done + 1).putInt("prayers_done", qadaPrayersDone).apply() },
                                onUndo = { if (done > 0) { qadaDoneByPrayer[prayer] = done - 1; qadaPrayersDone = qadaDoneByPrayer.values.sum(); prefs.edit().putInt("qada_done_" + prayer, done - 1).putInt("prayers_done", qadaPrayersDone).apply() } })
                        }
                        CounterCard("قضاء الصيام", fastRemaining, "يوم", "سجّل يوم صيام قضيته",
                            onAdd = { qadaFastsDone++; prefs.edit().putInt("fasts_done", qadaFastsDone).apply() },
                            onUndo = { if (qadaFastsDone > 0) { qadaFastsDone--; prefs.edit().putInt("fasts_done", qadaFastsDone).apply() } })
                        Text("الرصيد = الفوائت المقدرة + ما تسجله من صلوات فائتة يوميًا - ما قضيته.")
                        TextButton(onClick = { calculatorView = "الحاسبة"; mainTab = "القضاء" }) { Text("مراجعة الحسبة") }
                    }
                }
            } else if (mainTab == "فقه الوضوء" || mainTab == "فقه الصلاة") {
                item {
                    val isWuduSection = mainTab == "فقه الوضوء"
                    val domainTopics = if (isWuduSection) {
                        listOf("أركان الوضوء", "سنن الوضوء", "نواقض الوضوء")
                    } else {
                        listOf("شروط الصلاة", "أركان الصلاة", "واجبات الصلاة", "سنن الصلاة", "مبطلات الصلاة")
                    }
                    val selectedTopic = if (isWuduSection) wuduFiqhTopic else prayerFiqhTopic
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (isWuduSection) "فقه الوضوء" else "فقه الصلاة", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                        Text("اختر الباب الفقهي ثم المذهب لعرض خلاصة الأحكام.", color = Green)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            domainTopics.forEach { topic ->
                                FilterChip(
                                    selected = selectedTopic == topic,
                                    onClick = { if (isWuduSection) wuduFiqhTopic = topic else prayerFiqhTopic = topic },
                                    label = { Text(topic) }
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("الحنفي", "المالكي", "الشافعي", "الحنبلي").forEach { madhhab ->
                                FilterChip(selected = fiqhMadhhab == madhhab, onClick = { fiqhMadhhab = madhhab }, label = { Text(madhhab) })
                            }
                        }
                        val entry = FiqhGuide.get(selectedTopic, fiqhMadhhab)
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(selectedTopic + " – المذهب " + fiqhMadhhab, style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                                Text(entry.summary)
                                Text("المراجع: " + entry.source, color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                                Button(onClick = {
                                    val query = Uri.encode(selectedTopic + " " + fiqhMadhhab + " شرح تعليمي")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + query)))
                                }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("مشاهدة شرح فيديو على يوتيوب") }
                                Text("تنبيه علمي مهم: هذا القسم مرجع تعريفي مساعد، وليس فتوى ولا مصدرًا نهائيًا للحكم الشرعي. قد تختلف التفاصيل والترجيحات، ويجب مراجعة المعلومات مع عالم مؤهل أو كتاب معتمد في المذهب قبل العمل بها.", color = Color(0xFF8A4B19), style = MaterialTheme.typography.bodySmall)
                                Text("المحتوى الحالي مختصر، ورابط الفيديو يفتح نتائج بحث للموضوع والمذهب المختارين؛ راجع هوية المحاضر ومصدره العلمي.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else if (mainTab == "السنن") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("استثمار يومي في الجنة", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("قال رسول الله ﷺ: «ما من عبد مسلم يصلي لله كل يوم ثنتي عشرة ركعة تطوعًا غير فريضة، إلا بنى الله له بيتًا في الجنة».")
                            Text("صحيح مسلم (728)", color = Color(0xFF6D716A), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("متابعة السنن الرواتب اليوم", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("سجّل كل سنة بعد أدائها. يتجدد العداد يوميًا.")
                            Text(sunnahRakaatCount.toString() + " من 12 ركعة", style = MaterialTheme.typography.headlineMedium, color = Green, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(progress = { (sunnahRakaatCount.toFloat() / 12f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Green)
                            sunnahSlots.forEach { (slot, target) ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(slot + " — " + (sunnahDoneBySlot[slot] ?: 0) + "/" + target)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = {
                                            val next = ((sunnahDoneBySlot[slot] ?: 0) + 2).coerceAtMost(target)
                                            sunnahDoneBySlot[slot] = next
                                            actionFeedback = "أستغفر الله"
                                            prefs.edit().putInt("sunnah_slot_" + today + "_" + slot, next).apply()
                                        }, enabled = (sunnahDoneBySlot[slot] ?: 0) < target) { Text("+2") }
                                        TextButton(onClick = {
                                            val next = ((sunnahDoneBySlot[slot] ?: 0) - 2).coerceAtLeast(0)
                                            sunnahDoneBySlot[slot] = next
                                            prefs.edit().putInt("sunnah_slot_" + today + "_" + slot, next).apply()
                                        }, enabled = (sunnahDoneBySlot[slot] ?: 0) > 0) { Text("تراجع") }
                                    }
                                }
                            }
                            Text("قد تختلف تفاصيل الرواتب وعددها المؤكد باختلاف المذاهب؛ هذا العداد لمتابعة الاثنتي عشرة ركعة الواردة في الحديث، وليس حكمًا على قبول العبادة.", color = Color(0xFF755F45), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item { GuideCard("تفصيل السنن العملية", SunnahGuide.generalPrayer + SunnahGuide.afterPrayer + SunnahGuide.beforePrayer) }
            } else if (mainTab == "الصوم") {
                item {
                    val hijriToday = HijrahDate.from(today)
                    val hijriMonth = hijriToday.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                    val hijriYear = hijriToday.get(java.time.temporal.ChronoField.YEAR)
                    val ramadanYear = if (hijriMonth > 9) hijriYear + 1 else hijriYear
                    val ramadanStart = LocalDate.ofEpochDay(HijrahDate.of(ramadanYear, 9, 1).toEpochDay())
                    val daysToRamadan = java.time.temporal.ChronoUnit.DAYS.between(today, ramadanStart).coerceAtLeast(0)
                    Card(colors = CardDefaults.cardColors(containerColor = Green)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("رمضان القادم", color = Color.White.copy(alpha = 0.85f))
                            Text(if (hijriMonth == 9) "نحن في شهر رمضان" else "متبقي تقريبًا " + daysToRamadan + " يومًا", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("بداية الشهر محسوبة بالتقويم الهجري الحسابي في الجهاز، وقد تختلف عن إعلان الرؤية الرسمي في مصر بيوم.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("رمضان", "شوال", "النوافل", "الفوائت").forEach { section ->
                            FilterChip(selected = fastSection == section, onClick = { fastSection = section }, label = { Text(section) })
                        }
                    }
                }
                if (fastSection == "الفوائت") {
                    item {
                        CounterCard(title = "قضاء الصيام", remaining = fastRemaining, unit = "يوم", actionLabel = "سجل يوم قضاء",
                            onAdd = { qadaFastsDone++; prefs.edit().putInt("fasts_done", qadaFastsDone).apply() },
                            onUndo = { if (qadaFastsDone > 0) { qadaFastsDone--; prefs.edit().putInt("fasts_done", qadaFastsDone).apply() } })
                        TextButton(onClick = { mainTab = "القضاء"; calculatorView = "القضاء" }) { Text("فتح حاسبة الفوائت") }
                    }
                } else {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(when (fastSection) { "رمضان" -> "متابعة صيام رمضان"; "شوال" -> "ستة أيام من شوال"; else -> "صيام التطوع" }, style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                                Text(when (fastSection) {
                                    "رمضان" -> "الأيام المسجلة من رمضان: " + fastRamadanCount
                                    "شوال" -> "الأيام المسجلة من شوال: " + fastShawwalCount + " من 6. سجّل أيام شوال التي صمتها."
                                    else -> "من السنن: الاثنين والخميس، وثلاثة أيام من كل شهر، ويوم عرفة لغير الحاج، وعاشوراء."
                                })
                                if (fastLogged.isNotBlank()) {
                                    Text("تم تسجيل اليوم: " + fastLogged, color = Green, fontWeight = FontWeight.SemiBold)
                                    OutlinedButton(onClick = {
                                        if (fastLogged == "رمضان") { fastRamadanCount = (fastRamadanCount - 1).coerceAtLeast(0); prefs.edit().putInt("fast_ramadan_count", fastRamadanCount).apply() }
                                        if (fastLogged == "شوال") { fastShawwalCount = (fastShawwalCount - 1).coerceAtLeast(0); prefs.edit().putInt("fast_shawwal_count", fastShawwalCount).apply() }
                                        fastLogged = ""
                                        prefs.edit().remove("fast_type_" + today).apply()
                                    }, modifier = Modifier.fillMaxWidth()) { Text("إلغاء تسجيل صيام اليوم") }
                                } else {
                                    Button(onClick = {
                                        fastLogged = fastSection
                                        actionFeedback = "أستغفر الله"
                                        prefs.edit().putString("fast_type_" + today, fastSection).apply()
                                        if (fastSection == "رمضان") { fastRamadanCount++; prefs.edit().putInt("fast_ramadan_count", fastRamadanCount).apply() }
                                        if (fastSection == "شوال") { fastShawwalCount++; prefs.edit().putInt("fast_shawwal_count", fastShawwalCount).apply() }
                                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("سجّلت صيام اليوم") }
                                }
                                if (fastSection == "شوال") Text("العدد المسجل تراكمي؛ راجع أنه داخل شهر شوال نفسه.", color = Color(0xFF755F45), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    val recentFasts = (0..29).mapNotNull { offset ->
                        val loggedDate = today.minusDays(offset.toLong())
                        val kind = prefs.getString("fast_type_" + loggedDate, null)
                        if (kind.isNullOrBlank()) null else Pair(loggedDate, kind)
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("سجل الصيام — آخر 30 يومًا", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                            if (recentFasts.isEmpty()) Text("لم تسجل أيام صيام بعد.")
                            recentFasts.forEach { record -> Text(record.first.toString() + " — " + record.second) }
                        }
                    }
                }
                item {
                    GuideCard("مواسم وصيام مستحب", listOf(
                        SunnahItem("الاثنين والخميس", "صيام متكرر", "كان النبي ﷺ يتحرى صيام يومي الاثنين والخميس.", "جامع الترمذي 747؛ سنن النسائي 2360"),
                        SunnahItem("الأيام البيض", "13 و14 و15 من الشهر الهجري", "صيام ثلاثة أيام من كل شهر، ومنه الأيام البيض.", "صحيح البخاري 1981؛ صحيح مسلم 721"),
                        SunnahItem("عاشوراء", "العاشر من المحرم", "يُستحب صومه، ومعه التاسع مخالفةً لليهود.", "صحيح مسلم 1134"),
                        SunnahItem("يوم عرفة", "التاسع من ذي الحجة لغير الحاج", "صيامه يكفّر السنة الماضية والباقية كما ورد في الحديث.", "صحيح مسلم 1162"),
                        SunnahItem("ستة أيام من شوال", "بعد رمضان", "من صام رمضان ثم أتبعه ستًا من شوال كان كصيام الدهر.", "صحيح مسلم 1164"),
                        SunnahItem("أفضل الصيام", "صيام داود عليه السلام", "كان يصوم يومًا ويفطر يومًا.", "صحيح البخاري 1131؛ صحيح مسلم 1159")
                    ))
                }
            } else if (mainTab == "قيام الليل") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("مراتب القيام بالقرآن", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("قال رسول الله ﷺ: «من قام بعشر آيات لم يكتب من الغافلين، ومن قام بمئة آية كتب من القانتين، ومن قام بألف آية كتب من المقنطرين».")
                            Text("سنن أبي داود (1398) — حسّنه عدد من أهل العلم.", color = Color(0xFF6D716A), style = MaterialTheme.typography.bodySmall)
                            Text(nightWindow + " — وقت تقديري للثلث الأخير.")
                        }
                    }
                }
                item { GuideCard("سنن قيام الليل", listOf(
                    SunnahItem("افتتاح صلاة الليل بركعتين خفيفتين", "هدي نبوي ثابت", "إذا قام أحدكم من الليل فليفتتح صلاته بركعتين خفيفتين.", "صحيح مسلم 768"),
                    SunnahItem("صلاة الليل مثنى مثنى", "صفة ثابتة", "صلاة الليل ركعتين ركعتين، ويختم المصلي بوتر.", "صحيح البخاري 990؛ صحيح مسلم 749"),
                    SunnahItem("إطالة القيام والقراءة بحسب الاستطاعة", "هدي نبوي", "كان النبي ﷺ يطيل القيام والقراءة، ويشرع للمسلم أن يصلي ما تيسر له دون تكلف.", "صحيح البخاري 1135؛ صحيح مسلم 738"),
                    SunnahItem("القنوت في الوتر", "دعاء ثابت", "ورد دعاء: اللهم اهدني فيمن هديت، وعافني فيمن عافيت، وتولني فيمن توليت...", "سنن أبي داود 1425؛ جامع الترمذي 464، صححه أهل العلم")
                )) }
                item { QiyamTrackerScreen(prefs = prefs) }
            } else if (mainTab == "الإحصائيات") {
                item {
                    PrayerStatisticsScreen(
                        prefs = prefs,
                        today = today,
                        prayerBase = prayerBase,
                        qadaDoneByPrayer = qadaDoneByPrayer.toMap(),
                        missedByPrayer = missedByPrayer.toMap(),
                        fastBase = fastBase,
                        qadaFastsDone = qadaFastsDone
                    )
                }
            } else if (mainTab == "الأذكار") {
                item { AuthenticDhikrScreen(prefs = prefs) }
            } else if (mainTab == "الأدعية") {
                item { DuaLibraryScreen() }
            } else if (mainTab == "تدبر القرآن") {
                item { QuranReflectionScreen(prefs = prefs, context = context) }
            } else if (mainTab == "الإعدادات") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("إعدادات التنبيهات", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("اختر توقيت وصول الإشعار.")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = notifyGaza, onCheckedChange = { value ->
                                    notifyGaza = value
                                    prefs.edit().putBoolean("notify_gaza", value).apply()
                                    PrayerNotifications.scheduleGazaReminder(context, value)
                                })
                                Text("تذكير يومي بالدعاء لأهل غزة — الساعة 8 مساءً تقريبًا")
                            }
                            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                OutlinedButton(
                                    onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("السماح بإشعارات التطبيق") }
                                Text("لن تصل تذكيرات غزة أو الصلاة قبل السماح بإشعارات التطبيق من النظام.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A4B19))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = notifyBefore, onCheckedChange = { value ->
                                    notifyBefore = value
                                    prefs.edit().putBoolean("notify_before", value).apply()
                                    PrayerNotifications.schedule(context, lat, lon)
                                })
                                Text("تذكيرات متنوعة قبل الصلاة: قبلها بساعة ونصف ساعة و10 دقائق")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = notifyAt, onCheckedChange = { value ->
                                    notifyAt = value
                                    prefs.edit().putBoolean("notify_at", value).apply()
                                    PrayerNotifications.schedule(context, lat, lon)
                                })
                                Text("أذكار بعد الصلاة وقيام الليل والوتر")
                            }
                                            Text("عنوان الإشعار ثابت: الصلاة جامعة. ويظهر تحته حديث أو آية مرتبطة بالصلاة. لا يصدر التطبيق أذانًا صوتيًا.", color = Color(0xFF6D716A))
                        }
                    }
                }
                item { AppDataBackupPanel(prefs = prefs, context = context) }
            } else {
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("سنن الصلاة", "الاستيقاظ", "النوم", "السواك", "الطعام", "البيت", "المسجد").forEach { tab ->
                            FilterChip(selected = sunnahTab == tab, onClick = { sunnahTab = tab }, label = { Text(tab) })
                        }
                    }
                }
                when (sunnahTab) {
                    "سنن الصلاة" -> item { GuideCard("سنن عامة في الصلاة وسنن صلاة " + selectedPrayerGuide, SunnahGuide.generalPrayer + (SunnahGuide.prayerItems[selectedPrayerGuide] ?: emptyList()) + SunnahGuide.afterPrayer) }
                    "البيت" -> item { GuideCard("سنن وآداب البيت والدخول والخروج", SunnahGuide.home) }
                    "المسجد" -> item { GuideCard("أذكار وآداب المسجد", SunnahGuide.mosque) }
                    "الاستيقاظ" -> item { GuideCard("سنن وأذكار الاستيقاظ", SunnahGuide.waking) }
                    "النوم" -> item { GuideCard("سنن وأذكار النوم", SunnahGuide.sleep) }
                    "السواك" -> item { GuideCard("مواضع السواك", SunnahGuide.waking.filter { it.title.contains("السواك") } + SunnahGuide.beforePrayer.filter { it.title.contains("السواك") }) }
                    "الطعام" -> item { GuideCard("سنن الطعام", SunnahGuide.food) }
                    "القراءة في الصلاة" -> item {
                        GuideCard("ماذا أقرأ في الصلاة؟", listOf(
                            SunnahItem("سنة الفجر", "وردت سنة مخصوصة", "في الركعة الأولى بعد الفاتحة: سورة الكافرون، وفي الثانية: سورة الإخلاص.", "صحيح مسلم 726"),
                            SunnahItem("الفجر المفروضة", "هدي القراءة", "كان النبي ﷺ يطيل القراءة في الفجر، ووردت القراءة بطوال المفصل.", "صحيح مسلم 461"),
                            SunnahItem("الظهر والعصر", "هدي القراءة", "القراءة سرًا؛ وكان يطيل في الأوليين من الظهر، ويقرأ في العصر أقصر من الظهر.", "صحيح البخاري 759؛ صحيح مسلم 451"),
                            SunnahItem("المغرب والعشاء", "هدي القراءة", "يجهر في الأوليين؛ ووردت قراءات متفاوتة الطول، ولا تلزم سورة بعينها لكل صلاة.", "صحيح البخاري 763، 769")
                        ))
                    }
                    else -> item { GuideCard("السنن العملية", SunnahGuide.home + SunnahGuide.waking + SunnahGuide.sleep + SunnahGuide.food) }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    }
}

@Composable
private fun FirstLaunchNotice(onContinue: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Cream) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("قبل أن تبدأ", style = MaterialTheme.typography.headlineMedium, color = Green, fontWeight = FontWeight.Bold)
                Text("الصَّلَاةُ جَامِعَةٌ", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                Text("تطبيق مساعد على تذكّر الصلاة وتعلّم ما يتصل بها، وتنظيم بعض أعمال العبادة والتدبّر.")
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("هدف التطبيق", color = Green, fontWeight = FontWeight.Bold)
                        Text("أن يكون وسيلة تذكير وتعلّم ومتابعة، لا أن يحل محل العبادة نفسها أو يحوّلها إلى أرقام ومنافسة. استخدمه بما يعينك على الإخلاص والمداومة.")
                    }
                }
            }
            item {
                Text("تنبيهات مهمة", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                Text("• المعلومات الشرعية والأحاديث جُمعت للتعريف والتذكير، وقد تحتاج بعض المسائل إلى مراجعة أهل العلم. التطبيق ليس جهة إفتاء، ولا يغني عن سؤال عالم مؤهل.")
                Text("• قد تختلف بعض أحكام الصلاة والسنن وتفاصيلها بين المذاهب الفقهية؛ راعينا الإشارة إلى الخلاف حيث يظهر، فلا تجعل التطبيق سببًا للإنكار على الآخرين.")
                Text("• مواقيت الصلاة المحسوبة تقديرية وتتأثر بالموقع وطريقة الحساب وإعدادات الجهاز؛ راجع تقويم بلدك أو الجهة الرسمية، ولا تعتمد على التطبيق وحده عند الشك.")
                Text("• نص القرآن يُعرض من مصدر رقمي ويُخزّن على الجهاز بعد تحميله؛ راجع المصحف المعتمد عند وجود أي اختلاف أو شك في الرسم أو الضبط.")
                Text("• التنبيهات تحتاج إلى أذونات الإشعارات، وقد تتأثر بإعدادات توفير البطارية أو قيود الجهاز. التطبيق لا يشغّل أذانًا صوتيًا.")
            }
            item {
                Text("الخصوصية والبيانات", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                Text("سجل الصلاة ودفتر التدبّر والإعدادات تُحفظ محليًا على جهازك. لا توجد مزامنة سحابية لحساب شخصي في الوقت الحالي. إذا حذفت التطبيق أو بياناته فقد تفقد سجلك؛ استخدم النسخة الاحتياطية من الإعدادات واحتفظ بها في مكان آمن.")
                Text("يُستخدم إذن الموقع لحساب مواقيت الصلاة، ويمكنك رفضه واستخدام الموقع الافتراضي أو إدخال/تعديل الموقع لاحقًا بحسب ما يتيحه التطبيق.")
            }
            item {
                Text("تنبيه أخير", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                Text("هذا التطبيق عمل مساعد قابل للتطوير، وقد توجد أخطاء أو معلومات ناقصة. نرحب بالتنبيه عنها ومراجعتها، ولا تنسب كل ما يظهر فيه إلى الشرع قبل التحقق من دليله.")
            }
            item {
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                    Text("قرأت التنبيهات — ابدأ الاستخدام")
                }
            }
        }
    }
}

@Composable
private fun QuranReflectionScreen(prefs: android.content.SharedPreferences, context: android.content.Context) {
    val today = LocalDate.now().toString()
    var currentSurah by remember { mutableStateOf(prefs.getString("quran_surah", "البقرة") ?: "البقرة") }
    var currentAyah by remember { mutableStateOf(prefs.getString("quran_ayah", "1") ?: "1") }
    val dailyTarget = 10
    var enteredAyahCount by remember { mutableStateOf("") }
    var quranSection by remember { mutableStateOf("المصحف") }
    var progressDate by remember { mutableStateOf(prefs.getString("quran_daily_ayah_progress_date", today) ?: today) }
    var dailyProgress by remember { mutableIntStateOf(if (progressDate == today) prefs.getInt("quran_daily_ayah_progress", 0) else 0) }
    var sessionSurah by remember { mutableStateOf("البقرة") }
    var sessionAyah by remember { mutableStateOf("") }
    var reflection by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var deleteIndex by remember { mutableIntStateOf(-1) }
    var sessions by remember { mutableStateOf(prefs.getString("quran_sessions", "[]") ?: "[]") }
    fun persist(array: org.json.JSONArray) {
        sessions = array.toString()
        prefs.edit().putString("quran_sessions", sessions).apply()
    }
    val saved = try { org.json.JSONArray(sessions) } catch (_: Exception) { org.json.JSONArray() }
    if (progressDate != today) {
        LaunchedEffect(today) {
            progressDate = today
            dailyProgress = 0
            prefs.edit().putString("quran_daily_ayah_progress_date", today).putInt("quran_daily_ayah_progress", 0).apply()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = quranSection == "المصحف",
                onClick = { quranSection = "المصحف" },
                label = { Text("المصحف") }
            )
            FilterChip(
                selected = quranSection == "دفتر التدبّر",
                onClick = { quranSection = "دفتر التدبّر" },
                label = { Text("دفتر التدبّر") }
            )
        }
        if (quranSection == "المصحف") {
            QuranReaderSection(prefs = prefs, context = context) { surahNumber, surahName, ayahNumber ->
                currentSurah = surahName
                currentAyah = ayahNumber.toString()
                sessionSurah = surahName
                sessionAyah = ayahNumber.toString()
                prefs.edit().putInt("quran_surah_number", surahNumber)
                    .putString("quran_surah", surahName)
                    .putString("quran_ayah", ayahNumber.toString()).apply()
            }
        } else {
        Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الورد اليومي المقترح", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                Text("الهدف: $dailyTarget آيات — قرأت اليوم: $dailyProgress آية")
                LinearProgressIndicator(progress = { (dailyProgress.toFloat() / dailyTarget).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Green)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = enteredAyahCount,
                        onValueChange = { enteredAyahCount = it.filter(Char::isDigit).take(4) },
                        label = { Text("عدد الآيات التي قرأتها") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(
                        onClick = {
                            val amount = enteredAyahCount.toIntOrNull()
                            if (amount != null && amount in 1..1000) {
                                dailyProgress += amount
                                enteredAyahCount = ""
                                prefs.edit().putString("quran_daily_ayah_progress_date", today)
                                    .putInt("quran_daily_ayah_progress", dailyProgress).apply()
                            }
                        },
                        enabled = enteredAyahCount.toIntOrNull()?.let { it in 1..1000 } == true,
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) { Text("تسجيل") }
                }
                Text(
                    if (dailyProgress >= dailyTarget) "أتممت الورد المقترح. الزيادة خير، من غير إلزام."
                    else "عشر آيات هدف مقترح للتدرّب على المداومة، وليس حدًا شرعيًا لازمًا. يتجدد العداد يوميًا.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("موضع القراءة الحالي", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = currentSurah, onValueChange = { currentSurah = it; prefs.edit().putString("quran_surah", it).apply() }, label = { Text("السورة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = currentAyah, onValueChange = { currentAyah = it; prefs.edit().putString("quran_ayah", it).apply() }, label = { Text("رقم الآية أو موضع القراءة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (editingIndex >= 0) "تعديل جلسة تدبّر" else "تسجيل جلسة تدبّر", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = sessionSurah, onValueChange = { sessionSurah = it }, label = { Text("السورة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = sessionAyah, onValueChange = { sessionAyah = it }, label = { Text("الآيات من - إلى (اختياري)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = reflection, onValueChange = { reflection = it }, label = { Text("ما الذي لفت انتباهك أو تدبّرته؟") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("أسئلة تحتاج إلى بحث أو مراجعة") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = action, onValueChange = { action = it }, label = { Text("كيف تريد العمل بما تدبّرته؟") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(onClick = {
                    if (sessionSurah.isNotBlank() && (reflection.isNotBlank() || question.isNotBlank() || action.isNotBlank())) {
                        val array = try { org.json.JSONArray(sessions) } catch (_: Exception) { org.json.JSONArray() }
                        val entry = org.json.JSONObject()
                        entry.put("surah", sessionSurah.trim()); entry.put("ayah", sessionAyah.trim())
                        entry.put("reflection", reflection.trim()); entry.put("question", question.trim()); entry.put("action", action.trim())
                        entry.put("date", if (editingIndex >= 0) (array.optJSONObject(editingIndex)?.optString("date") ?: today) else today)
                        if (editingIndex >= 0 && editingIndex < array.length()) array.put(editingIndex, entry) else array.put(entry)
                        persist(array)
                        currentSurah = sessionSurah.trim()
                        if (sessionAyah.isNotBlank()) currentAyah = sessionAyah.trim()
                        prefs.edit().putString("quran_surah", currentSurah).putString("quran_ayah", currentAyah).apply()
                        editingIndex = -1; sessionSurah = "البقرة"; sessionAyah = ""; reflection = ""; question = ""; action = ""
                    }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(if (editingIndex >= 0) "حفظ التعديلات" else "حفظ جلسة التدبّر") }
                if (editingIndex >= 0) TextButton(onClick = { editingIndex = -1; sessionSurah = "البقرة"; sessionAyah = ""; reflection = ""; question = ""; action = "" }) { Text("إلغاء التعديل") }
                Text("خواطرك وأسئلتك الشخصية ليست تفسيرًا معتمدًا؛ راجع كتب التفسير وأهل العلم عند البحث عن معنى الآية.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF755F45))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("دفتر جلساتك السابقة", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية من دفتر تدبّر القرآن")
                    putExtra(Intent.EXTRA_TEXT, sessions)
                }
                context.startActivity(Intent.createChooser(send, "تصدير نسخة احتياطية"))
            }) { Text("تصدير") }
        }
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("ابحث في السورة أو الملاحظات أو الأسئلة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        val matching = (0 until saved.length()).filter { index ->
            val e = saved.optJSONObject(index) ?: return@filter false
            val haystack = listOf("surah", "ayah", "reflection", "question", "action", "date").joinToString(" ") { e.optString(it) }
            haystack.contains(search.trim(), ignoreCase = true)
        }
        if (matching.isEmpty()) Text(if (search.isBlank()) "لا توجد جلسات محفوظة حتى الآن." else "لا توجد نتائج مطابقة.")
        matching.asReversed().forEach { index ->
            val entry = saved.optJSONObject(index) ?: return@forEach
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(entry.optString("surah") + if (entry.optString("ayah").isNotBlank()) " — " + entry.optString("ayah") else "", color = Green, fontWeight = FontWeight.Bold)
                    Text(entry.optString("date"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (entry.optString("reflection").isNotBlank()) Text("تدبّري: " + entry.optString("reflection"))
                    if (entry.optString("question").isNotBlank()) Text("سؤال للبحث: " + entry.optString("question"))
                    if (entry.optString("action").isNotBlank()) Text("عمل: " + entry.optString("action"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            editingIndex = index
                            sessionSurah = entry.optString("surah"); sessionAyah = entry.optString("ayah")
                            reflection = entry.optString("reflection"); question = entry.optString("question"); action = entry.optString("action")
                        }) { Text("تعديل") }
                        TextButton(onClick = { deleteIndex = index }) { Text("حذف", color = Color(0xFFB3261E)) }
                    }
                }
            }
        }
        }
    }
    if (deleteIndex >= 0) {
        AlertDialog(
            onDismissRequest = { deleteIndex = -1 },
            title = { Text("حذف جلسة التدبّر؟") },
            text = { Text("لن يمكن استعادة هذه الجلسة بعد حذفها.") },
            confirmButton = { TextButton(onClick = {
                val array = try { org.json.JSONArray(sessions) } catch (_: Exception) { org.json.JSONArray() }
                if (deleteIndex in 0 until array.length()) array.remove(deleteIndex)
                persist(array); if (editingIndex == deleteIndex) editingIndex = -1
                deleteIndex = -1
            }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleteIndex = -1 }) { Text("إلغاء") } }
        )
    }
}
