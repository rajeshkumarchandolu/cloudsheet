//
//  SheetsView.swift
//  CloudSheetDemo
//
//  View for displaying and managing worksheets
//

import SwiftUI
import Combine
import OpenCloudSheet

struct SheetsView: View {
    let workbookId: String
    let workbookName: String

    @StateObject private var viewModel: SheetsViewModel
    @State private var showingCreateDialog = false
    @State private var selectedMonth = "January"
    @State private var selectedYear = Calendar.current.component(.year, from: Date())

    let months = ["January", "February", "March", "April", "May", "June",
                  "July", "August", "September", "October", "November", "December"]

    init(workbookId: String, workbookName: String) {
        self.workbookId = workbookId
        self.workbookName = workbookName
        _viewModel = StateObject(wrappedValue: SheetsViewModel(workbookId: workbookId, workbookName: workbookName))
    }

    var body: some View {
        ZStack {
            switch viewModel.uiState {
            case .loading:
                ProgressView("Loading sheets...")

            case .creating:
                ProgressView("Creating sheet...")

            case .success(_, let sheets):
                if sheets.isEmpty {
                    VStack(spacing: 20) {
                        Text("No Sheets")
                            .font(.title2)
                            .foregroundColor(.secondary)
                        Text("Create your first sheet to track expenses")
                            .foregroundColor(.secondary)
                        Button("Create Sheet") {
                            showingCreateDialog = true
                        }
                        .buttonStyle(.borderedProminent)
                    }
                } else {
                    List {
                        ForEach(Array(sheets.indices), id: \.self) { index in
                            let sheet = sheets[index]
                            NavigationLink(destination: {
                                ExpensesView(
                                    workbookId: workbookId,
                                    sheetName: sheet.sheetName
                                )
                            }()) {
                                HStack {
                                    Image(systemName: "doc.text")
                                        .foregroundColor(.blue)
                                    VStack(alignment: .leading) {
                                        Text(sheet.sheetName)
                                            .font(.body)
                                        if !sheet.description.isEmpty {
                                            Text(sheet.description)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }
                                    }
                                }
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    viewModel.deleteSheet(sheet: sheet)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                    }
                }

            case .error(let message):
                VStack(spacing: 20) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundColor(.red)
                    Text("Error")
                        .font(.title2)
                    Text(message)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.secondary)
                }
                .padding()
            }
        }
        .navigationTitle(workbookName)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showingCreateDialog = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingCreateDialog) {
            CreateSheetDialog(
                selectedMonth: $selectedMonth,
                selectedYear: $selectedYear,
                months: months,
                onCreate: {
                    viewModel.createSheet(month: selectedMonth, year: selectedYear)
                    showingCreateDialog = false
                },
                onCancel: {
                    showingCreateDialog = false
                }
            )
        }
    }
}

struct CreateSheetDialog: View {
    @Binding var selectedMonth: String
    @Binding var selectedYear: Int
    let months: [String]
    let onCreate: () -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Sheet Details")) {
                    Picker("Month", selection: $selectedMonth) {
                        ForEach(months, id: \.self) { month in
                            Text(month).tag(month)
                        }
                    }
                    Stepper("Year: \(selectedYear)", value: $selectedYear, in: 2000...2100)
                }
            }
            .navigationTitle("Create Sheet")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onCancel()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        onCreate()
                    }
                }
            }
        }
    }
}
