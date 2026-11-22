package com.salati.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.time.*
import java.time.format.DateTimeFormatter

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
                    val lat = loc?.latitude ?: getSharedPreferences("salati", MODE_PRIVATE).getFloat("lat", 30.0444f).toDouble()
                    val lon = loc?.longitude ?: getSharedPreferences("salati", MODE_PRIVATE).getFloat("lon", 31.2357f).toDouble()
                    getSharedPreferences("salati", MODE_PRIVATE).edit().putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).apply()
                    PrayerNotifications.schedule(this, lat, lon)
                }
        } catch (_: SecurityException) { }
    }
}

@Composable
private fun GuideCard(title: String, items: List<SunnahItem>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF24483D))
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
fun SalatiScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("salati", 0) }
    var prayers by remember { mutableIntStateOf(prefs.getInt("prayers_done", 0)) }
    var fasts by remember { mutableIntStateOf(prefs.getInt("fasts_done", 0)) }
    var selected by remember { mutableStateOf("الصلوات") }
    val lat = prefs.getFloat("lat", 30.0444f).toDouble()
    val lon = prefs.getFloat("lon", 31.2357f).toDouble()
    val times = remember(lat, lon) { PrayerTimes.forDate(LocalDate.now(), lat, lon, ZoneId.systemDefault()) }
    val fmt = DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale("ar"))
    Surface(Modifier.fillMaxSize(), color = Color(0xFFF6F2E9)) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("صَلَاتِي", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF24483D), fontWeight = FontWeight.Bold)
                Text("متابعة الصلاة والقضاء والسنن والأذكار", color = Color(0xFF6D716A))
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("الصلوات", "البيت والمسجد", "قبل النوم").forEachIndexed { index, tab ->
                        SegmentedButton(selected == tab, onClick = { selected = tab }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(tab) }
                    }
                }
            }
            if (selected == "الصلوات") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5EBDD))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("مواقيت الصلاة اليوم", fontWeight = FontWeight.Bold)
                            times.forEach { Text("${it.name} • ${Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).format(fmt)}") }
                            Text("الموقع: %.3f, %.3f".format(lat, lon), style = MaterialTheme.typography.bodySmall)
                            Text("طريقة الحساب مبدئية؛ راجع تقويم الهيئة المصرية العامة للمساحة.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                items(listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")) { name ->
                    GuideCard("سنن وأذكار صلاة $name", (SunnahGuide.prayerItems[name] ?: emptyList()) + SunnahGuide.afterPrayer)
                }
                item {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("رصيد القضاء", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("الصلاة المتبقية: ${(23115 - prayers).coerceAtLeast(0)} فرضًا")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { prayers++; prefs.edit().putInt("prayers_done", prayers).apply() }) { Text("سجّل قضاء صلاة") }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { if (prayers > 0) { prayers--; prefs.edit().putInt("prayers_done", prayers).apply() } }) { Text("تراجع") }
                            }
                            HorizontalDivider()
                            Text("الصيام المتبقي: ${(350 - fasts).coerceAtLeast(0)} يومًا")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { fasts++; prefs.edit().putInt("fasts_done", fasts).apply() }) { Text("سجّل يوم صيام") }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { if (fasts > 0) { fasts--; prefs.edit().putInt("fasts_done", fasts).apply() } }) { Text("تراجع") }
                            }
                        }
                    }
                }
            } else if (selected == "البيت والمسجد") {
                item { GuideCard("أذكار وآداب دخول البيت والخروج منه", SunnahGuide.home) }
                item { GuideCard("أذكار وآداب دخول المسجد والخروج منه", SunnahGuide.mosque) }
            } else {
                item { GuideCard("هدي النبي ﷺ قبل النوم", SunnahGuide.sleep) }
            }
            item {
                Text("تنبيه علمي: السنن الرواتب المؤكدة تختلف بعض تفاصيل تصنيفها وعددها بين المذاهب. يعرض التطبيق المشهور في المنهج الحنفي المصري، ويفصل بين الراتبة المؤكدة والذكر الثابت والآداب. التطبيق للتذكير ولا يصدر فتوى.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF755F45))
            }
        }
    }
}
