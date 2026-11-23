import SwiftUI

struct ContentView: View {
    @State private var selectedTab: AppTab = .prayer

    var body: some View {
        TabView(selection: $selectedTab) {
            PrayerHomeView()
                .tabItem { Label("الصلاة", systemImage: "clock") }
                .tag(AppTab.prayer)

            QadaView()
                .tabItem { Label("القضاء", systemImage: "checkmark.circle") }
                .tag(AppTab.qada)

            StatisticsView()
                .tabItem { Label("الإحصائيات", systemImage: "chart.bar") }
                .tag(AppTab.statistics)

            SettingsView()
                .tabItem { Label("الإعدادات", systemImage: "gearshape") }
                .tag(AppTab.settings)
        }
        .tint(Color(red: 22/255, green: 75/255, blue: 58/255))
    }
}

private enum AppTab: Hashable {
    case prayer, qada, statistics, settings
}

private struct PrayerHomeView: View {
    @State private var statuses: [String: PrayerStatus] = PrayerStatus.emptyDay

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("﴿إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا﴾")
                        .font(.headline)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                Section("صلوات اليوم") {
                    ForEach(PrayerName.allCases) { prayer in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(prayer.rawValue).font(.headline)
                            Picker("الحالة", selection: Binding(
                                get: { statuses[prayer.rawValue] ?? .notRecorded },
                                set: { statuses[prayer.rawValue] = $0 }
                            )) {
                                ForEach(PrayerStatus.allCases) { status in
                                    Text(status.rawValue).tag(status)
                                }
                            }
                            .pickerStyle(.menu)
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
            .navigationTitle("الصَّلَاةُ جَامِعَةٌ")
        }
    }
}

private struct QadaView: View {
    @State private var completed: [String: Int] = Dictionary(uniqueKeysWithValues: PrayerName.allCases.map { ($0.rawValue, 0) })

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("﴿وَالَّذِينَ جَاهَدُوا فِينَا لَنَهْدِيَنَّهُمْ سُبُلَنَا﴾")
                        .font(.headline)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                    Text("العنكبوت: 69")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Section("متابعة قضاء الصلوات") {
                    ForEach(PrayerName.allCases) { prayer in
                        Stepper("\(prayer.rawValue): \(completed[prayer.rawValue, default: 0])", value: Binding(
                            get: { completed[prayer.rawValue, default: 0] },
                            set: { completed[prayer.rawValue] = max(0, $0) }
                        ), in: 0...100000)
                    }
                }
            }
            .navigationTitle("القضاء")
        }
    }
}

private struct StatisticsView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "الإحصائيات",
                systemImage: "chart.bar",
                description: Text("سيتم نقل إحصائيات الصلوات والقضاء وسجل التقدم من نسخة Android في مراحل التطوير التالية.")
            )
            .navigationTitle("الإحصائيات")
        }
    }
}

private struct SettingsView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "الإعدادات",
                systemImage: "gearshape",
                description: Text("إعدادات التنبيهات والموقع والنسخ الاحتياطي ستُضاف أثناء نقل وظائف التطبيق.")
            )
            .navigationTitle("الإعدادات")
        }
    }
}
