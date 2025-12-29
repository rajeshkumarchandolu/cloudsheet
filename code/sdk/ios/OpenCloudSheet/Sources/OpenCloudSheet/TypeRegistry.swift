//
//  TypeRegistry.swift
//  OpenCloudSheet
//
//  Internal registry for mapping iOS class name strings to IWorksheetRow types
//

import Foundation

internal actor TypeRegistry {
    internal static let shared = TypeRegistry()

    private var typeMap: [String: IWorksheetRow.Type] = [:]

    private init() {}

    internal func register(type: IWorksheetRow.Type) {
        let className = type.getIosClassName()
        typeMap[className] = type
    }

    internal func register(types: [IWorksheetRow.Type]) {
        for type in types {
            let className = type.getIosClassName()
            typeMap[className] = type
        }
    }

    internal func resolve(className: String) -> IWorksheetRow.Type? {
        return typeMap[className]
    }
}
