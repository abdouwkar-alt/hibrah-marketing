package com.salati.app
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
data class PrayerTime(val name: String, val time: Long)
object PrayerTimes {
 private const val PI = Math.PI
 private fun norm(x: Double) = ((x % 360.0) + 360.0) % 360.0
 private fun jd(date: LocalDate): Double {
  val y0=date.year; val m0=date.monthValue; val d=date.dayOfMonth
  val y=if(m0<=2)y0-1 else y0; val m=if(m0<=2)m0+12 else m0
  val a=floor(y/100.0); val b=2-a+floor(a/4.0)
  return floor(365.25*(y+4716))+floor(30.6001*(m+1))+d+b-1524.5
 }
 private fun sun(julian: Double): Pair<Double,Double> {
  val d=julian-2451545.0; val g=norm(357.529+0.98560028*d)*PI/180
  val q=norm(280.459+0.98564736*d); val l=norm(q+1.915*sin(g)+0.020*sin(2*g))
  val e=(23.439-0.00000036*d)*PI/180
  val ra=atan2(cos(e)*sin(l*PI/180),cos(l*PI/180))*180/PI
  val dec=asin(sin(e)*sin(l*PI/180))
  val eq=(((q-norm(ra)+180.0)%360.0+360.0)%360.0-180.0)/15.0
  return dec to eq
 }
 private fun hourAngle(lat:Double,dec:Double,angle:Double):Double {
  val v=(sin(angle*PI/180)-sin(lat*PI/180)*sin(dec))/(cos(lat*PI/180)*cos(dec))
  return if(v < -1 || v > 1) Double.NaN else acos(v)*180/PI/15
 }
 fun forDate(date:LocalDate,lat:Double,lon:Double,zone:ZoneId):List<PrayerTime> {
  val (dec,eq)=sun(jd(date)+0.5)
  val offset=zone.rules.getOffset(date.atTime(12,0).atZone(zone).toInstant()).totalSeconds/3600.0
  val noon=12.0+offset-lon/15.0-eq
  val fajr=noon-hourAngle(lat,dec,-19.5)
  val dhuhr=noon+0.03
  val asrAngle=atan(1.0/(1.0+tan(abs(lat-dec*180/PI)*PI/180)))*180/PI
  val asr=noon+hourAngle(lat,dec,asrAngle)
  val maghrib=noon+hourAngle(lat,dec,-0.833)+0.03
  val isha=noon+hourAngle(lat,dec,-17.5)
  fun millis(hours:Double):Long {
   val minutes=(hours*60).roundToInt()
   return date.atStartOfDay(zone).toInstant().toEpochMilli()+minutes*60_000L
  }
  return listOf(PrayerTime("الفجر",millis(fajr)),PrayerTime("الظهر",millis(dhuhr)),PrayerTime("العصر",millis(asr)),PrayerTime("المغرب",millis(maghrib)),PrayerTime("العشاء",millis(isha)))
 }
}
