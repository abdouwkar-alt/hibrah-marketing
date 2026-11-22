package com.salati.app

import android.app.Activity
import android.content.Context
import android.widget.Toast
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private const val BACKUP_SCHEMA_VERSION = 1
private val BACKUP_PROTECTED_KEYS = setOf("lat", "lon")

private fun createAppBackup(prefs: SharedPreferences): String {
    val stored = JSONObject()
    prefs.all.forEach { (key, value) ->
        if (key in BACKUP_PROTECTED_KEYS) return@forEach
        val item = JSONObject()
        when (value) {
            is String -> item.put("type", "string").put("value", value)
            is Int -> item.put("type", "int").put("value", value)
            is Long -> item.put("type", "long").put("value", value)
            is Float -> item.put("type", "float").put("value", value.toDouble())
            is Boolean -> item.put("type", "boolean").put("value", value)
            is Set<*> -> {
                val values = JSONArray()
                value.filterIsInstance<String>().sorted().forEach { values.put(it) }
                item.put("type", "string_set").put("value", values)
            }
            else -> return@forEach
        }
        stored.put(key, item)
    }

    return JSONObject()
        .put("app_id", "com.salati.app")
        .put("schema_version", BACKUP_SCHEMA_VERSION)
        .put("created_at", java.time.Instant.now().toString())
        .put("preferences", stored)
        .toString(2)
}

private fun validateAppBackup(raw: String): JSONObject {
    val root = JSONObject(raw)
    if (root.optString("app_id") != "com.salati.app") {
        throw IllegalArgumentException("هذا الملف ليس نسخة احتياطية من تطبيق الصلاة جامعة.")
    }
    if (root.optInt("schema_version", -1) != BACKUP_SCHEMA_VERSION) {
        throw IllegalArgumentException("إصدار ملف النسخة الاحتياطية غير مدعوم.")
    }
    val values = root.optJSONObject("preferences")
        ?: throw IllegalArgumentException("ملف النسخة الاحتياطية لا يحتوي على البيانات المطلوبة.")

    val iterator = values.keys()
    while (iterator.hasNext()) {
        val key = iterator.next()
        if (key in BACKUP_PROTECTED_KEYS) continue
        val item = values.optJSONObject(key)
            ?: throw IllegalArgumentException("تعذر قراءة أحد عناصر النسخة الاحتياطية.")
        when (item.optString("type")) {
            "string", "int", "long", "float", "boolean" -> if (!item.has("value")) {
                throw IllegalArgumentException("توجد قيمة ناقصة في ملف النسخة الاحتياطية.")
            }
            "string_set" -> if (item.optJSONArray("value") == null) {
                throw IllegalArgumentException("توجد قائمة غير مكتملة في ملف النسخة الاحتياطية.")
            }
            else -> throw IllegalArgumentException("نوع بيانات غير معروف في ملف النسخة الاحتياطية.")
        }
    }
    return root
}

private fun restoreAppBackup(prefs: SharedPreferences, backup: JSONObject): String {
    val values = backup.getJSONObject("preferences")
    val editor = prefs.edit()

    prefs.all.keys.filter { it !in BACKUP_PROTECTED_KEYS }.forEach { editor.remove(it) }

    val iterator = values.keys()
    while (iterator.hasNext()) {
        val key = iterator.next()
        if (key in BACKUP_PROTECTED_KEYS) continue
        val item = values.getJSONObject(key)
        val value = item.get("value")
        when (item.getString("type")) {
            "string" -> editor.putString(key, value as String)
            "int" -> editor.putInt(key, (value as Number).toInt())
            "long" -> editor.putLong(key, (value as Number).toLong())
            "float" -> editor.putFloat(key, (value as Number).toFloat())
            "boolean" -> editor.putBoolean(key, value as Boolean)
            "string_set" -> {
                val array = value as JSONArray
                val set = buildSet {
                    for (i in 0 until array.length()) add(array.getString(i))
                }
                editor.putStringSet(key, set)
            }
        }
    }

    return if (editor.commit()) {
        "تمت استعادة بيانات التطبيق. أعد فتح الأقسام لمراجعة بياناتك."
    } else {
        "لم تكتمل الاستعادة. لم يتم تأكيد حفظ البيانات."
    }
}

@Composable
fun AppDataBackupPanel(prefs: SharedPreferences, context: Context) {
    var status by remember { mutableStateOf("") }
    var pendingBackup by remember { mutableStateOf<JSONObject?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            status = try {
                val json = createAppBackup(prefs)
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("تعذر فتح مكان حفظ النسخة.")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                "تم حفظ النسخة الاحتياطية بنجاح."
            } catch (e: Exception) {
                "تعذر حفظ النسخة: ${e.message ?: "خطأ غير معروف"}"
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException("تعذر قراءة الملف المختار.")
                pendingBackup = validateAppBackup(raw)
                status = "تم التحقق من الملف. راجع رسالة التأكيد قبل الاستعادة."
            } catch (e: Exception) {
                pendingBackup = null
                status = "لم يتم قبول الملف: ${e.message ?: "ملف غير صالح"}"
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("حفظ بياناتك واستعادتها", color = Color(0xFF164B3A), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("بيانات الصلاة والقضاء والأذكار ودفتر التدبّر تُحفظ حاليًا على جهازك. أنشئ ملفًا احتياطيًا قبل تغيير الهاتف أو حذف التطبيق.")
            Button(
                onClick = { createBackupLauncher.launch("salati-backup-${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF164B3A))
            ) { Text("إنشاء نسخة احتياطية") }
            OutlinedButton(
                onClick = { openBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("استعادة نسخة احتياطية") }
            Text(
                "يمكن اختيار Google Drive من نافذة الحفظ إذا كان متاحًا على جهازك. لا يرفع التطبيق بياناتك إلى خادم مركزي. ملف النسخة يحتوي على ملاحظاتك الخاصة؛ احتفظ به في مكان آمن. لا يتضمن إحداثيات موقعك.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D716A)
            )
            if (status.isNotBlank()) {
                Text(status, color = Color(0xFF164B3A), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    pendingBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            title = { Text("استعادة بيانات التطبيق؟") },
            text = {
                Text(
                    "سيتم استبدال البيانات المحلية الحالية بما في ملف النسخة المؤرخ ${backup.optString("created_at", "بتاريخ غير محدد")}. احتفظ بنسخة من بياناتك الحالية أولًا إن كنت تحتاج إليها."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    status = try {
                        restoreAppBackup(prefs, backup).also { message ->
                            if (message.startsWith("تمت استعادة")) {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                (context as? Activity)?.recreate()
                            }
                        }
                    } catch (e: Exception) {
                        "تعذرت الاستعادة: ${e.message ?: "خطأ غير معروف"}"
                    }
                    pendingBackup = null
                }) { Text("استعادة البيانات") }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackup = null }) { Text("إلغاء") }
            }
        )
    }
}
