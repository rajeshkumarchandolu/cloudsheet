//
//  WorkBooksView.swift
//  CloudSheetDemo
//
//  View for displaying and managing workbooks
//

import SwiftUI
import OpenCloudSheet

struct WorkBooksView: View {
    @StateObject private var viewModel = WorkBooksViewModel()
    @State private var showingCreateDialog = false
    @State private var newWorkbookName = ""
    @State private var newWorkbookDescription = ""

    var body: some View {
        ZStack {
            switch viewModel.uiState {
            case .loading:
                ProgressView("Loading workbooks...")

            case .creating:
                ProgressView("Creating workbook...")

            case .success(let workbooks):
                if workbooks.isEmpty {
                    VStack(spacing: 20) {
                        Text("No Workbooks")
                            .font(.title2)
                            .foregroundColor(.secondary)
                        Text("Create your first workbook to get started")
                            .foregroundColor(.secondary)
                        Button("Create Workbook") {
                            showingCreateDialog = true
                        }
                        .buttonStyle(.borderedProminent)
                    }
                } else {
                    List {
                        ForEach(Array(workbooks.indices), id: \.self) { index in
                            let workbook = workbooks[index]
                            NavigationLink(destination: SheetsView(
                                workbookId: workbook.getId(),
                                workbookName: workbook.getName()
                            )) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(workbook.getName())
                                        .font(.headline)
                                    Text(workbook.getWorkBookEntry().description)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    viewModel.deleteWorkBook(workbook: workbook)
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
                    Button("Retry") {
                        viewModel.loadWorkBooks()
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()
            }
        }
        .navigationTitle("Workbooks")
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
            CreateWorkbookSheet(
                workbookName: $newWorkbookName,
                workbookDescription: $newWorkbookDescription,
                onCreate: {
                    viewModel.createWorkBook(name: newWorkbookName, description: newWorkbookDescription)
                    newWorkbookName = ""
                    newWorkbookDescription = ""
                    showingCreateDialog = false
                },
                onCancel: {
                    newWorkbookName = ""
                    newWorkbookDescription = ""
                    showingCreateDialog = false
                }
            )
        }
    }
}

struct CreateWorkbookSheet: View {
    @Binding var workbookName: String
    @Binding var workbookDescription: String
    let onCreate: () -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Workbook Details")) {
                    TextField("Name", text: $workbookName)
                    TextField("Description", text: $workbookDescription)
                }
            }
            .navigationTitle("Create Workbook")
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
                    .disabled(workbookName.isEmpty)
                }
            }
        }
    }
}
