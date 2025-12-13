# CloudSheet SDK Class Structure for Development

## Overview

This document outlines the complete interface structure for implementing the CloudSheet SDK. It provides the concrete interfaces that developers need to implement and extend the SDK.

## Core Interfaces

### CloudSheetSDK (Abstract Factory)

The main factory interface with explicit provider initialization methods.

**Interface:**
```
interface CloudSheetSDK {
    // Provider initialization - explicit methods, no switch statements
    + initializeMicrosoftOnedriveProvider(config: MicrosoftSDKConfig): Promise<void>
    + initializeGoogleDriveProvider(config: GoogleSDKConfig): Promise<void>

    // Provider discovery
    + getInitializedProviders(): CloudProvider[]
    + getStorageProvider(provider: CloudProvider): StorageProvider

    // Sheet management
    + getSheets(): Promise<SheetInfo[]>

    // Utilities
    + reset(): void
}
```

### StorageProvider (Abstract Product)

The core interface for spreadsheet operations with model-driven design.

**Interface:**
```
interface StorageProvider {
    + createSheet<Model>(name: string, model: Model): Promise<string>
    + getRows<Model>(sheetId: string, filter?: RowFilter): Promise<Model[]>
    + addRow<Model>(sheetId: string, data: Model): Promise<void>
}
```

### Authenticator (Abstract Product)

Abstraction for OAuth provider implementations.

**Interface:**
```
interface Authenticator {
    + initialize(): Promise<void>
    + getAuthToken(): Promise<AuthResult>
    + getCurrentUser(): UserInfo | null
    + logout(): Promise<void>
}

interface AuthResult {
    success: boolean
    token?: string
    refreshToken?: string
    user?: UserInfo
    error?: string
}

interface UserInfo {
    id: string
    name: string
    email: string
}
```

## Supporting Interfaces

### Configuration System

Provider-specific configurations using discriminated unions for type safety.

**Interfaces:**
```
enum CloudProvider {
    MICROSOFT_EXCEL
    GOOGLE_SHEETS
}

interface SDKConfig {
    provider: CloudProvider
}

interface MicrosoftSDKConfig extends SDKConfig {
    provider: CloudProvider.MICROSOFT_EXCEL
    clientId: string
    tenantId?: string
    redirectUri?: string
}

interface GoogleSDKConfig extends SDKConfig {
    provider: CloudProvider.GOOGLE_SHEETS
    clientId: string
    apiKey?: string
    redirectUri?: string
}
```

### Model-Driven Schema System

Users define data models that automatically map to spreadsheet structures.

**Interfaces:**
```typescript
interface SchemaDefinable {
    schema: SchemaDefinition
}

interface SchemaDefinition {
    name: string
    version: string
    fields: FieldDefinition[]
}

interface FieldDefinition {
    name: string
    type: FieldType
    required: boolean
    defaultValue?: any
}

enum FieldType {
    STRING, NUMBER, DATE, BOOLEAN
}
```

### Sheet Management

**Interface:**
```
interface SheetInfo {
    id: string
    name: string
    provider: CloudProvider
    createdAt: string
    createdBy: string
    schema: SchemaDefinition
    memberCount?: number
}
```

## Usage Patterns

### Initialization
```typescript
// Initialize providers explicitly
await CloudSheetSDK.initializeMicrosoftOnedriveProvider(msConfig)
await CloudSheetSDK.initializeGoogleDriveProvider(googleConfig)

// Check available providers
const providers = CloudSheetSDK.getInitializedProviders()
// [MICROSOFT_EXCEL, GOOGLE_SHEETS]
```

### Sheet Operations
```typescript
// Get provider instance
const storage = CloudSheetSDK.getStorageProvider(MICROSOFT_EXCEL)

// Define model
interface Expense extends SchemaDefinable {
    id: string
    amount: number
    user: string
    schema: SchemaDefinition
}

// Create sheet
const sheetId = await storage.createSheet("Expenses", Expense)

// Add data
await storage.addRow(sheetId, { id: "1", amount: 25.50, user: "John" })

// Get data
const expenses = await storage.getRows(sheetId)
```

### Sheet Discovery
```typescript
// Get all sheets across providers
const sheets = await CloudSheetSDK.getSheets()
// Returns SheetInfo[] from all initialized providers
```

## Error Handling

```
enum SDKError {
    PROVIDER_ALREADY_INITIALIZED
    PROVIDER_NOT_INITIALIZED
    INVALID_CONFIG
    AUTHENTICATION_FAILED
    NETWORK_ERROR
    SCHEMA_VALIDATION_ERROR
    SHEET_NOT_FOUND
}
```

## Extension Points

### Adding New Providers

1. Add new `CloudProvider` enum value
2. Create provider-specific config interface
3. Add initialization method to `CloudSheetSDK`
4. Implement `StorageProvider` and `Authenticator`
5. No changes to existing interfaces required

### Adding New Operations

1. Extend `StorageProvider` interface (if core operation)
2. Or create specialized interfaces for advanced features
3. Implement in concrete provider classes

## Implementation Notes

- **Token Management**: Authenticators handle token caching and refresh
- **Error Isolation**: Provider failures don't affect others
- **Thread Safety**: SDK methods should be thread-safe
- **Memory Management**: Provider instances cached for performance
- **Testing**: `reset()` method enables test isolation

This structure provides a complete interface specification for implementing and extending the CloudSheet SDK.
