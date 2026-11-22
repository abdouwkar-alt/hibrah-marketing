package com.salati.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = Color(0xFF164B3A)
private val Cream = Color(0xFFF6F2E9)
private val PaleGreen = Color(0xFFE5EBDD)
private val PrayerNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
private val PrayerStatuses = listOf("لم أسجل بعد", "في الوقت جماعة", "في الوقت منفردًا", "بعد الوقت", "لم أصلِّ")

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
fun SalatiScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("salati", 0) }
    var qadaPrayersDone by remember { mutableIntStateOf(prefs.getInt("prayers_done", 0)) }
    var qadaFastsDone by remember { mutableIntStateOf(prefs.getInt("fasts_done", 0)) }
    val qadaDoneByPrayer = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>().apply { PrayerNames.forEach { put(it, prefs.getInt("qada_done_" + it, 0)) } } }
    val missedByPrayer = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>().apply { PrayerNames.forEach { put(it, prefs.getInt("missed_" + it, 0)) } } }
    var missedSinceInstall by remember { mutableIntStateOf(prefs.getInt("missed_since_install", 0)) }
    var prayerBase by remember { mutableIntStateOf(prefs.getInt("prayer_base", -1)) }
    var fastBase by remember { mutableIntStateOf(prefs.getInt("fast_base", -1)) }
    var mainTab by remember { mutableStateOf("الرئيسية") }
    var sunnahTab by remember { mutableStateOf("سنن الصلاة") }
    var fiqhTopic by remember { mutableStateOf("أركان الصلاة") }
    var fiqhMadhhab by remember { mutableStateOf("الحنفي") }
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000L); clockNow = System.currentTimeMillis() } }
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

    val dailyStatus: SnapshotStateMap<String, String> = remember(today) {
        androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
            PrayerNames.forEach { name -> put(name, prefs.getString("status_" + today + "_" + name, "لم أسجل بعد") ?: "لم أسجل بعد") }
        }
    }

    var birthDay by remember { mutableStateOf("") }
    var birthMonth by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }
    var estimate by remember { mutableStateOf<QadaEstimate?>(null) }
    var calcError by remember { mutableStateOf("") }
    var deductOneYear by remember { mutableStateOf(false) }
    var performedPrayers by remember { mutableStateOf("0") }
    var partialFastDays by remember { mutableStateOf("0") }
    var ramadanChecks by remember { mutableStateOf(setOf<Int>()) }

    val prayerRemaining = if (prayerBase >= 0) (prayerBase + missedByPrayer.values.sum() - qadaDoneByPrayer.values.sum()).coerceAtLeast(0) else 0
    val fastRemaining = if (fastBase >= 0) (fastBase - qadaFastsDone).coerceAtLeast(0) else 0

    Surface(Modifier.fillMaxSize(), color = Cream) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("صَلَاتِي", style = MaterialTheme.typography.headlineLarge, color = Green, fontWeight = FontWeight.Bold)
                Text("حساب الفوائت ومتابعة الصلاة يومًا بيوم", color = Color(0xFF6D716A))
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("الرئيسية", "الحاسبة", "القضاء", "السنن").forEach { tab ->
                        FilterChip(selected = mainTab == tab, onClick = { mainTab = tab }, label = { Text(tab) })
                    }
                }
            }

            if (mainTab == "الرئيسية") {
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
                                    Text(Instant.ofEpochMilli(prayer.time).atZone(ZoneId.systemDefault()).format(timeFormatter))
                                }
                                HorizontalDivider(color = Color(0xFFD4DDCE))
                            }
                            Text("المواقيت محسوبة محليًا بطريقة تقريبية؛ راجعها مع التقويم الرسمي.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF755F45))
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
                                                    prefs.edit().putString("status_" + today + "_" + name, status).apply()
                                                    if (old != "لم أصلِّ" && status == "لم أصلِّ") {
                                                        missedSinceInstall++
                                                        prefs.edit().putInt("missed_since_install", missedSinceInstall).apply()
                                                    } else if (old == "لم أصلِّ" && status != "لم أصلِّ") {
                                                        missedSinceInstall = (missedSinceInstall - 1).coerceAtLeast(0)
                                                        prefs.edit().putInt("missed_since_install", missedSinceInstall).apply()
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
                            TextButton(onClick = { mainTab = "الحاسبة" }) { Text("ابدأ حساب الفوائت") }
                        }
                    }
                }
            } else if (mainTab == "الحاسبة") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("حاسبة بداية التكليف", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("أدخل تاريخ ميلادك الميلادي. سيحوّله التطبيق إلى هجري، ثم يضيف 15 سنة هجرية.")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NumberField("اليوم", birthDay, { birthDay = it }, Modifier.weight(1f))
                                NumberField("الشهر", birthMonth, { birthMonth = it }, Modifier.weight(1f))
                                NumberField("السنة", birthYear, { birthYear = it }, Modifier.weight(1.4f))
                            }
                            Button(onClick = {
                                calcError = ""
                                try {
                                    val date = LocalDate.of(birthYear.toInt(), birthMonth.toInt(), birthDay.toInt())
                                    estimate = QadaCalculator.calculate(date, today)
                                    ramadanChecks = estimate!!.ramadans.filter { prefs.getBoolean("ramadan_" + it.hijriYear, false) }.map { it.hijriYear }.toSet()
                                } catch (e: Exception) {
                                    calcError = e.message ?: "راجع تاريخ الميلاد والبيانات المدخلة."
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) { Text("احسب بداية التكليف") }
                            if (calcError.isNotBlank()) Text(calcError, color = Color(0xFFB3261E))
                            estimate?.let { result ->
                                HorizontalDivider()
                                Text("تاريخ الميلاد الهجري: " + QadaCalculator.hijriLabel(result.birthHijri), fontWeight = FontWeight.SemiBold)
                                Text("بداية التكليف: " + QadaCalculator.hijriLabel(result.taklifHijri), fontWeight = FontWeight.SemiBold)
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
                                    qadaPrayersDone = 0
                                    qadaFastsDone = 0
                                    prefs.edit().putInt("prayer_base", prayerBase).putInt("fast_base", fastBase).putInt("prayers_done", 0).putInt("fasts_done", 0).apply()
                                    mainTab = "القضاء"
                                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("اعتماد النتيجة وبدء المتابعة") }
                            }
                        }
                    }
                }
            } else if (mainTab == "القضاء") {
                item {
                    if (prayerBase < 0 || fastBase < 0) {
                        Card { Column(Modifier.padding(16.dp)) { Text("لم يتم إعداد رصيد القضاء بعد."); Button(onClick = { mainTab = "الحاسبة" }) { Text("افتح الحاسبة") } } }
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
                        TextButton(onClick = { mainTab = "الحاسبة" }) { Text("مراجعة الحسبة") }
                    }
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("سنن الصلاة", "البيت والمسجد", "الاستيقاظ", "قبل الصلاة", "قبل النوم", "القراءة في الصلاة", "فقه الوضوء والصلاة").forEach { tab ->
                            FilterChip(selected = sunnahTab == tab, onClick = { sunnahTab = tab }, label = { Text(tab) })
                        }
                    }
                }
                when (sunnahTab) {
                    "سنن الصلاة" -> item {
                        GuideCard("السنن الرواتب والأذكار بعد الصلاة", PrayerNames.flatMap { name ->
                            listOf(SunnahItem(name, "سنن الصلاة", "", "")) + (SunnahGuide.prayerItems[name] ?: emptyList())
                        } + SunnahGuide.afterPrayer)
                    }
                    "البيت والمسجد" -> {
                        item { GuideCard("أذكار وآداب البيت", SunnahGuide.home) }
                        item { GuideCard("أذكار وآداب المسجد", SunnahGuide.mosque) }
                    }
                    "الاستيقاظ" -> item { GuideCard("سنن وأذكار الاستيقاظ", SunnahGuide.waking) }
                    "قبل الصلاة" -> item { GuideCard("سنن وآداب قبل الصلاة", SunnahGuide.beforePrayer) }
                    "القراءة في الصلاة" -> item {
                        GuideCard("ماذا أقرأ في الصلاة؟", listOf(
                            SunnahItem("سنة الفجر", "وردت سنة مخصوصة", "في الركعة الأولى بعد الفاتحة: سورة الكافرون، وفي الثانية: سورة الإخلاص.", "صحيح مسلم 726"),
                            SunnahItem("الفجر المفروضة", "هدي القراءة", "كان النبي ﷺ يطيل القراءة في الفجر، ووردت القراءة بطوال المفصل.", "صحيح مسلم 461"),
                            SunnahItem("الظهر والعصر", "هدي القراءة", "القراءة سرًا؛ وكان يطيل في الأوليين من الظهر، ويقرأ في العصر أقصر من الظهر.", "صحيح البخاري 759؛ صحيح مسلم 451"),
                            SunnahItem("المغرب والعشاء", "هدي القراءة", "يجهر في الأوليين؛ ووردت قراءات متفاوتة الطول، ولا تلزم سورة بعينها لكل صلاة.", "صحيح البخاري 763، 769")
                        ))
                    }
                    "فقه الوضوء والصلاة" -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("اختر الموضوع ثم المذهب لعرض خلاصة الأحكام.", color = Green)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FiqhGuide.topics.forEach { topic -> FilterChip(selected = fiqhTopic == topic, onClick = { fiqhTopic = topic }, label = { Text(topic) }) }
                            }
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("الحنفي", "المالكي", "الشافعي", "الحنبلي").forEach { madhhab -> FilterChip(selected = fiqhMadhhab == madhhab, onClick = { fiqhMadhhab = madhhab }, label = { Text(madhhab) }) }
                            }
                            val entry = FiqhGuide.get(fiqhTopic, fiqhMadhhab)
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(fiqhTopic + " – المذهب " + fiqhMadhhab, style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                                    Text(entry.summary)
                                    Text("المراجع: " + entry.source, color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    else -> item { GuideCard("أذكار النوم", SunnahGuide.sleep) }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
