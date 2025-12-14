// swift-tools-version: 6.2
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Cloudsheet-ios-sdk",
    products: [
        // Products define the executables and libraries a package produces, making them visible to other packages.
        .library(
            name: "Cloudsheet-ios-sdk",
            targets: ["Cloudsheet-ios-sdk"]
        ),
    ],
    dependencies: [
        .package(
            url: "https://github.com/AzureAD/microsoft-authentication-library-for-objc",
            exact: "2.7.0"
        )
    ],
    targets: [
        // Targets are the basic building blocks of a package, defining a module or a test suite.
        // Targets can depend on other targets in this package and products from dependencies.
        .target(
            name: "Cloudsheet-ios-sdk"
        ),
        .testTarget(
            name: "Cloudsheet-ios-sdkTests",
            dependencies: ["Cloudsheet-ios-sdk"]
        ),
    ]
)
