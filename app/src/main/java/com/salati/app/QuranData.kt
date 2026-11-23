package com.salati.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private data class QuranSurahData(
    val number: Int,
    val name: String,
    val englishName: String,
    val ayahs: List<QuranAyahData>
)

private data class QuranAyahData(val number: Int, val text: String)

private object QuranTextRepository {
    private const val ENDPOINT = "https://api.alquran.cloud/v1/quran/quran-uthmani"
    private const val CACHE_NAME = "quran-uthmani-alqurancloud-v1.json"

    fun load(context: Context, forceRefresh: Boolean = false): List<QuranSurahData> {
        val cache = File(context.filesDir, CACHE_NAME)
        if (!forceRefresh && cache.isFile && cache.length() > 100_000L) {
            try {
                return parse(cache.readText(Charsets.UTF_8))
            } catch (_: Exception) {
                cache.delete()
            }
        }

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SalatiAndroid/1.1")
        }

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IOException("لم يصل رد من مصدر النص القرآني.")
            if (status !in 200..299) throw IOException("تعذر تحميل النص القرآني (HTTP $status).")

            val verified = parse(response)
            val temporary = File(context.filesDir, "$CACHE_NAME.tmp")
            temporary.writeText(response, Charsets.UTF_8)
            if (cache.exists() && !cache.delete()) throw IOException("تعذر تحديث ملف النص المحفوظ.")
            if (!temporary.renameTo(cache)) {
                temporary.copyTo(cache, overwrite = true)
                temporary.delete()
            }
            return verified
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(raw: String): List<QuranSurahData> {
        val root = JSONObject(raw)
        if (root.optInt("code") != 200) throw IOException("مصدر النص القرآني أعاد استجابة غير مكتملة.")
        val data = root.getJSONObject("data")
        val editionId = data.optJSONObject("edition")?.optString("identifier").orEmpty()
        if (editionId.isNotBlank() && editionId != "quran-uthmani") {
            throw IOException("مصدر النص أعاد إصدارًا مختلفًا عن الرسم العثماني المطلوب.")
        }
        val array = data.getJSONArray("surahs")
        if (array.length() != 114) throw IOException("فشل التحقق من عدد سور القرآن.")

        var totalAyahs = 0
        val result = ArrayList<QuranSurahData>(114)
        for (surahIndex in 0 until array.length()) {
            val obj = array.getJSONObject(surahIndex)
            val number = obj.getInt("number")
            if (number != surahIndex + 1) throw IOException("ترتيب السور في البيانات غير صحيح.")
            val name = obj.getString("name")
            val ayahArray = obj.getJSONArray("ayahs")
            // بعض استجابات المصدر لا ترسل numberOfAyahs؛ عدد عناصر ayahs هو المرجع البنيوي هنا.
            val expectedAyahs = obj.optInt("numberOfAyahs", ayahArray.length())
            if (ayahArray.length() != expectedAyahs || expectedAyahs <= 0) throw IOException("فشل التحقق من آيات سورة $number.")

            val ayahs = ArrayList<QuranAyahData>(ayahArray.length())
            for (ayahIndex in 0 until ayahArray.length()) {
                val ayah = ayahArray.getJSONObject(ayahIndex)
                val ayahNumber = ayah.getInt("numberInSurah")
                val text = ayah.getString("text")
                if (ayahNumber != ayahIndex + 1 || text.isBlank()) {
                    throw IOException("فشل التحقق من نص آية في سورة $number.")
                }
                ayahs.add(QuranAyahData(ayahNumber, text))
            }
            totalAyahs += ayahs.size
            result.add(QuranSurahData(number, name, obj.optString("englishName"), ayahs))
        }
        if (totalAyahs != 6236) throw IOException("فشل التحقق من إجمالي آيات القرآن.")
        return result
    }
}

@Composable
fun QuranReaderSection(
    prefs: android.content.SharedPreferences,
    context: Context,
    onPositionSelected: (Int, String, Int) -> Unit
) {
    var surahs by remember { mutableStateOf<List<QuranSurahData>>(emptyList()) }
    var selectedNumber by remember { mutableIntStateOf(prefs.getInt("quran_surah_number", 2).coerceIn(1, 114)) }
    var selectedAyah by remember { mutableIntStateOf(prefs.getString("quran_ayah", "1")?.toIntOrNull() ?: 1) }
    var search by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load(force: Boolean = false) {
        scope.launch {
            loading = true
            error = null
            try {
                surahs = withContext(Dispatchers.IO) { QuranTextRepository.load(context, force) }
                val selected = surahs.firstOrNull { it.number == selectedNumber } ?: surahs.first()
                selectedNumber = selected.number
                selectedAyah = selectedAyah.coerceIn(1, selected.ayahs.size)
            } catch (e: Exception) {
                error = if (e is IOException && !e.message.isNullOrBlank()) e.message else "تعذر تحميل المصحف أو التحقق من بياناته. تحقق من الاتصال ثم أعد المحاولة."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5EBDD))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("مصحف القرآن الكريم", style = MaterialTheme.typography.titleLarge, color = Color(0xFF164B3A), fontWeight = FontWeight.Bold)
            Text("فهرس السور والآيات بالنص العثماني. اضغط على الآية لتسجيل موضع قراءتك.")
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF164B3A))
                Text("يُحمّل النص أو يُقرأ من النسخة المحفوظة على هذا الجهاز…")
            } else if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { load() }, modifier = Modifier.fillMaxWidth()) { Text("إعادة المحاولة") }
            } else {
                val current = surahs.firstOrNull { it.number == selectedNumber }
                OutlinedButton(onClick = { menuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (current == null) "اختر السورة" else "${current.number}. ${current.name}")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.heightIn(max = 460.dp)
                ) {
                    surahs.forEach { surah ->
                        DropdownMenuItem(
                            text = { Text("${surah.number}. ${surah.name}") },
                            onClick = {
                                selectedNumber = surah.number
                                selectedAyah = 1
                                search = ""
                                menuExpanded = false
                                prefs.edit().putInt("quran_surah_number", surah.number)
                                    .putString("quran_surah", surah.name)
                                    .putString("quran_ayah", "1").apply()
                                onPositionSelected(surah.number, surah.name, 1)
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("ابحث عن سورة في الفهرس") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (search.isNotBlank()) {
                    surahs.filter { it.name.contains(search.trim(), ignoreCase = true) || it.englishName.contains(search.trim(), ignoreCase = true) }
                        .take(8)
                        .forEach { match ->
                            TextButton(onClick = {
                                selectedNumber = match.number
                                selectedAyah = 1
                                search = ""
                                prefs.edit().putInt("quran_surah_number", match.number)
                                    .putString("quran_surah", match.name)
                                    .putString("quran_ayah", "1").apply()
                                onPositionSelected(match.number, match.name, 1)
                            }) { Text("${match.number}. ${match.name}") }
                        }
                }
                if (current != null) {
                    Text("اضغط على أي آية لتثبيت موضع القراءة الحالي.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F665F))
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        current.ayahs.forEach { ayah ->
                            Surface(
                                color = if (ayah.number == selectedAyah) Color(0xFFD6E5D7) else MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().clickable {
                                        selectedAyah = ayah.number
                                        prefs.edit().putInt("quran_surah_number", current.number)
                                            .putString("quran_surah", current.name)
                                            .putString("quran_ayah", ayah.number.toString()).apply()
                                        onPositionSelected(current.number, current.name, ayah.number)
                                    }.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text("الآية ${ayah.number}", color = Color(0xFF164B3A), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        ayah.text,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    "النص العثماني: مشروع تنزيل (Tanzil)، عبر AlQuran Cloud. يُحفظ النص كما ورد دون تعديل بعد أول تحميل ناجح.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F665F)
                )
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tanzil.net/docs/Text_License")))
                }) { Text("ترخيص نصّ تنزيل") }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://alquran.cloud/terms-and-conditions")))
                }) { Text("شروط خدمة AlQuran Cloud") }
            }
        }
    }
}
