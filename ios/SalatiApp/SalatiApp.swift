import SwiftUI

@main
struct SalatiApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.layoutDirection, .rightToLeft)
                .preferredColorScheme(.light)
        }
    }
}
