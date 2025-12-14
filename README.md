# CloudSheet SDK

A lightweight, privacy-first framework for building collaborative, transparent record-keeping applications using user-owned cloud storage.

## Overview

CloudSheet SDK enables multiple users to record, view, and maintain shared transactional data in real-time using spreadsheets as the persistence layer, eliminating the need for traditional backend infrastructure.

## Features

- **Zero Backend**: Data lives in user's personal cloud storage (Google Sheets, OneDrive)
- **Real-time Collaboration**: Multi-user sync with append-only audit trails
- **Privacy First**: Complete user data ownership
- **Framework Approach**: Configurable for different use cases (expense tracking, inventory, etc.)
- **Multi-Platform**: iOS, Android, Web, Desktop support

## Project Structure

```
├── docs/                          # Documentation
├── code/                          # Source code
│   ├── sdk/                       # Core framework libraries
│   │   ├── android/               # Android SDK library
│   │   ├── ios/                   # iOS Swift Package
│   │   ├── web/                   # Web implementation
│   │   └── desktop/               # Desktop implementation
│   └── examples/                  # Demo applications
│       └── Demo-app/              # Main demo app
│           ├── android/           # Android demo app
│           ├── ios/               # iOS demo app
│           ├── web/               # Web demo app
│           └── desktop/           # Desktop demo app
├── scripts/                       # Build and deployment scripts
├── LICENSE
├── README.md
└── .gitignore                    # Git ignore rules
```

## Getting Started

See [Product Requirements Document](./docs/cloudsheet-sdk-prd.md) for detailed specifications.

## Contributing

This project is open source. Contributions welcome!

## License

See [LICENSE](./LICENSE) for details.
