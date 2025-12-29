//
//  CloudSheetDemoApp.swift
//  CloudSheetDemo
//
//  Created by Rajesh chandolu on 13/12/25.
//

import SwiftUI
import OpenCloudSheet

@main
struct CloudSheetDemoApp: App {
    init() {
        Task {
            await OpenCloudSheetSdk.registerModels([Expense.self])
        }
    }

    var body: some Scene {
        WindowGroup {
            LoginView()
        }
    }
}
