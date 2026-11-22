package com.salati.app

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

data class RamadanPeriod(val hijriYear: Int, val start: LocalDate, val endExclusive: LocalDate) {
    val days: Int get() = ChronoUnit.DAYS.between(start, endExclusive).toInt()
}

data class QadaEstimate(
    val birthHijri: HijrahDate,
    val taklifHijri: HijrahDate,
    val taklifDate: LocalDate,
    val prayerDays: Long,
    val totalPrayers: Long,
    val ramadans: List<RamadanPeriod>,
    val totalRamadanDays: Int,
    val oneHijriYearDays: Int
)

object QadaCalculator {
    fun calculate(birthDate: LocalDate, today: LocalDate = LocalDate.now(), egyptAdjustmentDays: Long = 1L, taklifAgeYears: Int = 15): QadaEstimate {
        require(!birthDate.isAfter(today)) { "تاريخ الميلاد لا يمكن أن يكون في المستقبل" }
        require(taklifAgeYears in 1..40) { "سن بداية الحساب يجب أن يكون بين 1 و40 سنة" }
        // Egypt-facing calibration: apply one consistent, user-visible day adjustment to every converted date.
        // This is an approximation, not an official historical moon-sighting archive.
        val birthHijri = HijrahDate.from(birthDate).plus(egyptAdjustmentDays, ChronoUnit.DAYS) as HijrahDate
        val taklifHijri = birthHijri.plus(taklifAgeYears.toLong(), ChronoUnit.YEARS) as HijrahDate
        val taklifDate = LocalDate.ofEpochDay(taklifHijri.toEpochDay() - egyptAdjustmentDays)
        require(!taklifDate.isAfter(today)) { "لم يصل تاريخ بداية الحساب بعد" }

        val prayerDays = ChronoUnit.DAYS.between(taklifDate, today.plusDays(1))
        val totalPrayers = prayerDays * 5L

        val startHijriYear = taklifHijri.get(ChronoField.YEAR_OF_ERA)
        val endHijriYear = (HijrahDate.from(today).plus(egyptAdjustmentDays, ChronoUnit.DAYS) as HijrahDate).get(ChronoField.YEAR_OF_ERA)
        val periods = mutableListOf<RamadanPeriod>()
        for (year in startHijriYear..endHijriYear) {
            val ramadanStart = LocalDate.ofEpochDay(HijrahDate.of(year, 9, 1).toEpochDay() - egyptAdjustmentDays)
            val nextMonth = LocalDate.ofEpochDay(HijrahDate.of(year, 10, 1).toEpochDay() - egyptAdjustmentDays)
            val from = if (ramadanStart.isBefore(taklifDate)) taklifDate else ramadanStart
            val until = if (nextMonth.isAfter(today.plusDays(1))) today.plusDays(1) else nextMonth
            if (from.isBefore(until)) periods.add(RamadanPeriod(year, from, until))
        }
        val nextHijriYear = taklifHijri.plus(1, ChronoUnit.YEARS) as HijrahDate
        val nextHijriYearDate = LocalDate.ofEpochDay(nextHijriYear.toEpochDay() - egyptAdjustmentDays)
        val oneHijriYearDays = ChronoUnit.DAYS.between(taklifDate, nextHijriYearDate).toInt()
        return QadaEstimate(birthHijri, taklifHijri, taklifDate, prayerDays, totalPrayers, periods, periods.sumOf { it.days }, oneHijriYearDays)
    }

    fun hijriLabel(date: HijrahDate): String {
        val day = date.get(ChronoField.DAY_OF_MONTH)
        val month = date.get(ChronoField.MONTH_OF_YEAR)
        val year = date.get(ChronoField.YEAR_OF_ERA)
        val months = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
        return day.toString() + " " + months[month - 1] + " " + year + " هـ"
    }
}
