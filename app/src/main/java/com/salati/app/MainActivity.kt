package com.salati.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = Color(0xFF24483D)
private val Cream = Color(0xFFF6F2E9)
private val PaleGreen = Color(0xFFE5EBDD)
private val PrayerNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")

class MainActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { getLocationAndSchedule() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrayerNotifications.createChannel(this)
        val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= 33) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (needed.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) permissions.launch(needed.toTypedArray()) else getLocationAndSchedule()
        setContent { SalatiScreen() }
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
            val client = LocationServices.getFusedLocationProviderClient(this)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
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
                Text(item.detail)
                Text("الدليل: ${item.evidence}", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(color = Color(0xFFECE8DE))
            }
        }
    }
}

@Composable
private fun CounterCard(
    title: String,
    remaining: Int,
    unit: String,
    actionLabel: String,
    onAdd: () -> Unit,
    onUndo: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
            Text(remaining.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("$unit متبقية")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(actionLabel) }
                OutlinedButton(onClick = onUndo) { Text("تراجع") }
            }
        }
    }
}

@Composable
fun SalatiScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("salati", 0) }
    var qadaPrayers by remember { mutableIntStateOf(prefs.getInt("prayers_done", 0)) }
    var qadaFasts by remember { mutableIntStateOf(prefs.getInt("fasts_done", 0)) }
    var mainTab by remember { mutableStateOf("الرئيسية") }
    var sunnahTab by remember { mutableStateOf("سنن الصلاة") }
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            clockNow = System.currentTimeMillis()
        }
    }
    val today = Instant.ofEpochMilli(clockNow).atZone(ZoneId.systemDefault()).toLocalDate()
    val tomorrow = today.plusDays(1)
    val lat = prefs.getFloat("lat", 30.0444f).toDouble()
    val lon = prefs.getFloat("lon", 31.2357f).toDouble()
    val todayTimes = remember(today, lat, lon) { PrayerTimes.forDate(today, lat, lon, ZoneId.systemDefault()) }
    val tomorrowTimes = remember(tomorrow, lat, lon) { PrayerTimes.forDate(tomorrow, lat, lon, ZoneId.systemDefault()) }
    val nextPrayer = (todayTimes + tomorrowTimes).firstOrNull { it.time > clockNow } ?: tomorrowTimes.first()
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale("ar"))
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
    val nextDuration = ((nextPrayer.time - clockNow).coerceAtLeast(0L) / 1000L)
    val countdown = "%02d:%02d:%02d".format(nextDuration / 3600, (nextDuration % 3600) / 60, nextDuration % 60)

    val dailyStatus: SnapshotStateMap<String, Boolean> = remember(today) {
        androidx.compose.runtime.mutableStateMapOf<String, Boolean>().apply {
            PrayerNames.forEach { name -> put(name, prefs.getBoolean("daily_${today}_$name", false)) }
        }
    }

    Surface(Modifier.fillMaxSize(), color = Cream) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("صَلَاتِي", style = MaterialTheme.typography.headlineLarge, color = Green, fontWeight = FontWeight.Bold)
                Text("رفيقك لمتابعة الصلاة والقضاء", color = Color(0xFF6D716A))
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("الرئيسية", "السنن والأذكار").forEachIndexed { index, tab ->
                        SegmentedButton(mainTab == tab, onClick = { mainTab = tab }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(tab) }
                    }
                }
            }
            if (mainTab == "الرئيسية") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Green)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("الصلاة القادمة", color = Color.White.copy(alpha = 0.85f))
                            Text(nextPrayer.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(Instant.ofEpochMilli(nextPrayer.time).atZone(ZoneId.systemDefault()).format(timeFormatter), color = Color.White)
                            Text("متبقي $countdown", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("مواقيت الصلاة اليوم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                            Text(today.format(dateFormatter), color = Color(0xFF6D716A))
                            todayTimes.forEach { prayer ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(prayer.name, fontWeight = FontWeight.SemiBold)
                                    Text(Instant.ofEpochMilli(prayer.time).atZone(ZoneId.systemDefault()).format(timeFormatter), fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(color = Color(0xFFD4DDCE))
                            }
                            Text("الموقع: ${"%.3f".format(lat)}, ${"%.3f".format(lon)}", style = MaterialTheme.typography.bodySmall)
                            Text("المواقيت محسوبة محليًا بطريقة تقريبية؛ يُرجى مراجعتها مع التقويم الرسمي لمدينتك.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF755F45))
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("متابعة فروض اليوم", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("علّم الصلاة بعد أدائها. يتم حفظ تسجيل اليوم على جهازك.")
                            PrayerNames.forEach { name ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = dailyStatus[name] == true,
                                        onCheckedChange = { checked ->
                                            dailyStatus[name] = checked
                                            prefs.edit().putBoolean("daily_${today}_$name", checked).apply()
                                        }
                                    )
                                    Text(name, fontWeight = FontWeight.Medium)
                                }
                            }
                            Text("${dailyStatus.values.count { it }} من 5 صلوات مسجلة اليوم", color = Green, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    Text("القضاء", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                }
                item {
                    CounterCard(
                        title = "قضاء الصلوات",
                        remaining = (23115 - qadaPrayers).coerceAtLeast(0),
                        unit = "فرض",
                        actionLabel = "سجّل فرضًا قضيته",
                        onAdd = { qadaPrayers++; prefs.edit().putInt("prayers_done", qadaPrayers).apply() },
                        onUndo = { if (qadaPrayers > 0) { qadaPrayers--; prefs.edit().putInt("prayers_done", qadaPrayers).apply() } }
                    )
                }
                item {
                    CounterCard(
                        title = "قضاء الصيام",
                        remaining = (350 - qadaFasts).coerceAtLeast(0),
                        unit = "يوم",
                        actionLabel = "سجّل يوم صيام قضيته",
                        onAdd = { qadaFasts++; prefs.edit().putInt("fasts_done", qadaFasts).apply() },
                        onUndo = { if (qadaFasts > 0) { qadaFasts--; prefs.edit().putInt("fasts_done", qadaFasts).apply() } }
                    )
                }
            } else {
                item {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("سنن الصلاة", "البيت والمسجد", "قبل النوم").forEachIndexed { index, tab ->
                            SegmentedButton(sunnahTab == tab, onClick = { sunnahTab = tab }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(tab) }
                        }
                    }
                }
                when (sunnahTab) {
                    "سنن الصلاة" -> {
                        item { GuideCard("السنن الرواتب والأذكار بعد الصلاة", PrayerNames.flatMap { name -> listOf(SunnahItem(name, "سنن الصلاة", "", "")) + (SunnahGuide.prayerItems[name] ?: emptyList()) } + SunnahGuide.afterPrayer) }
                    }
                    "البيت والمسجد" -> {
                        item { GuideCard("أذكار وآداب البيت", SunnahGuide.home) }
                        item { GuideCard("أذكار وآداب المسجد", SunnahGuide.mosque) }
                    }
                    else -> item { GuideCard("أذكار النوم", SunnahGuide.sleep) }
                }
                item {
                    Text("تختلف بعض تفاصيل السنن الرواتب وتصنيفها بين المذاهب. يعرض التطبيق المشهور في المنهج الحنفي المصري، ولا يُعدّ فتوى.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF755F45))
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
