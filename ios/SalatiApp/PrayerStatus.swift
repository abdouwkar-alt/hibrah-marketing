import Foundation

enum PrayerName: String, CaseIterable, Identifiable {
    case fajr = "الفجر"
    case dhuhr = "الظهر"
    case asr = "العصر"
    case maghrib = "المغرب"
    case isha = "العشاء"

    var id: String { rawValue }
}

enum PrayerStatus: String, CaseIterable, Identifiable {
    case notRecorded = "لم أسجل بعد"
    case congregation = "في الوقت جماعة"
    case alone = "في الوقت منفردًا"
    case late = "بعد الوقت"
    case missed = "لم أصلِّ"

    var id: String { rawValue }

    static var emptyDay: [String: PrayerStatus] {
        Dictionary(uniqueKeysWithValues: PrayerName.allCases.map { ($0.rawValue, .notRecorded) })
    }
}
