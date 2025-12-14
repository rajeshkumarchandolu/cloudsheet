# Product Requirements Document: CloudSheetSDK

**Framework Name:** CloudSheetSDK  
**Document Version:** 1.0  
**Last Updated:** December 2025  
**Document Owner:** Product Team  
**Status:** Proposal - Seeking Stakeholder Approval

---

## Executive Summary

**CloudSheetSDK** is a lightweight, privacy-first framework for building collaborative, transparent record-keeping applications. It enables multiple users to record, view, and maintain shared transactional data in real-time using user-owned cloud storage as the persistence layer, eliminating the need for traditional backend infrastructure.

### What is CloudSheetSDK?

CloudSheetSDK is not an application—it's a **foundational framework** that provides the core infrastructure for any use case requiring:
- Multiple users recording transactions
- Real-time visibility across stakeholders
- Append-only audit trails
- Data ownership and privacy
- Zero server maintenance

### Core Value Proposition

Traditional SaaS applications for record-keeping (expense trackers, inventory systems, credit ledgers) require:
- Centralized servers and databases
- Ongoing operational costs
- User data stored on company servers
- Subscription-based pricing models
- Vendor lock-in

**CloudSheetSDK inverts this model:**
- **Zero backend** - Data lives in user's personal cloud storage
- **Zero maintenance** - No servers to operate post-release
- **Complete ownership** - Users control their data
- **One-time cost** - Pay once, use forever
- **Open source** - Transparent, auditable code

### Framework Philosophy

CloudSheetSDK provides a **generic collaborative transaction engine** that can be configured for different domains:

```
CloudSheetSDK = 
    User Authentication (OAuth) +
    Spreadsheet Storage (Cloud Provider APIs) +
    Multi-user Sync (Real-time polling) +
    Append-only Transactions (Audit trail) +
    QR-based Sharing (Group invitations) +
    Configurable Schema (Use-case templates)
```

Different applications are built by:
1. Defining a transaction schema (what fields to track)
2. Creating a UI for data entry (forms)
3. Designing analytics/views (dashboards)
4. Setting up export formats (CSV/PDF)

**Development time:** First app = 6-8 weeks, subsequent apps = 1-2 weeks each.

---

## Target Spreadsheet Providers

CloudSheetSDK is designed to work with cloud-based spreadsheet services that provide programmatic API access. The framework currently targets:

### 1. Google Drive (Google Sheets)
- **API:** Google Sheets API v4
- **Authentication:** OAuth 2.0 via Google Sign-In
- **Permissions Required:** `drive.file` scope (access only to files created by the app)
- **Storage Location:** User's personal Google Drive
- **Features:** Real-time collaboration, edit history, sharing controls
- **Quota:** 100 requests per 100 seconds per user (sufficient for normal usage)

### 2. Microsoft OneDrive (Excel Online)
- **API:** Microsoft Graph API
- **Authentication:** OAuth 2.0 via Microsoft Authentication Library
- **Permissions Required:** `Files.ReadWrite` scope (app-created files only)
- **Storage Location:** User's personal OneDrive
- **Features:** Real-time collaboration, version history, sharing controls
- **Quota:** Similar to Google, adequate for typical use cases

### Design Principles
- **Provider Abstraction:** Framework uses a storage abstraction layer, making it easy to add new providers
- **User Choice:** Users select their preferred provider during setup
- **No Vendor Lock-in:** Data remains in standard spreadsheet format, portable between providers
- **Future Extensibility:** Architecture supports additional providers (e.g., Apple iCloud Numbers, local storage)

**Note:** Throughout this document, "cloud spreadsheet provider" or "spreadsheet API" refers to any of these supported services.

---

## Market Opportunity

### Problem Statement

Millions of households, small businesses, and informal economy participants need simple, transparent record-keeping but face a dilemma:

**Option 1: Manual Methods**
- Paper notebooks, spreadsheets, receipts in boxes
- ❌ No collaboration, no backup, error-prone
- ❌ Difficult to analyze, hard to share

**Option 2: Enterprise SaaS Tools**
- Expensive ($10-50/user/month)
- ❌ Overcomplicated for simple needs
- ❌ Privacy concerns, subscription fatigue
- ❌ Vendor lock-in, data ownership issues

**Option 3: Consumer Apps**
- Free but ad-supported or limited
- ❌ Data sold to third parties
- ❌ Features locked behind paywalls
- ❌ Service discontinuation risk

### CloudSheetSDK's Solution

A **middle path** that combines the simplicity of manual methods with the power of digital tools, while preserving privacy and ownership:

- Simple like a notebook, powerful like software
- Collaborative like shared spreadsheets, structured like an app
- Private like local files, accessible like cloud storage
- Affordable like a one-time purchase, sustainable like open source

### Target Market Segments

1. **Households** (50M+ globally)
   - Need: Track family expenses, bills, shared purchases
   - Current solution: Nothing, or expensive budgeting apps

2. **Small Businesses** (100M+ globally)
   - Need: Simple expense tracking, inventory, time logging
   - Current solution: Spreadsheets or overpriced enterprise tools

3. **Informal Economy** (1B+ in developing markets)
   - Need: Credit ledgers (khata books), customer tracking
   - Current solution: Paper books or ad-heavy apps

4. **Freelancers/Contractors** (60M+ globally)
   - Need: Time tracking, client invoicing, expense management
   - Current solution: Manual logs or monthly subscriptions

---

## Framework Architecture

### High-Level System Design

```
┌────────────────────────────────────────────┐
│         CloudSheetSDK Framework Core       │
│  ┌──────────────────────────────────────┐  │
│  │     Authentication & Authorization    │  │
│  │        (OAuth: Cloud Providers)       │  │
│  └──────────────────────────────────────┘  │
│                     │                       │
│  ┌──────────────────────────────────────┐  │
│  │      Storage Abstraction Layer       │  │
│  │    (Cloud Spreadsheet Provider APIs) │  │
│  └──────────────────────────────────────┘  │
│                     │                       │
│  ┌──────────────────────────────────────┐  │
│  │     Transaction Engine (Core)        │  │
│  │  • Append-only operations            │  │
│  │  • Conflict resolution               │  │
│  │  • Audit trail maintenance           │  │
│  │  • Soft delete pattern               │  │
│  └──────────────────────────────────────┘  │
│                     │                       │
│  ┌──────────────────────────────────────┐  │
│  │        Sync & Collaboration          │  │
│  │  • Real-time polling                 │  │
│  │  • Multi-user coordination           │  │
│  │  • Change detection                  │  │
│  └──────────────────────────────────────┘  │
│                     │                       │
│  ┌──────────────────────────────────────┐  │
│  │         Sharing Infrastructure       │  │
│  │  • QR code generation                │  │
│  │  • Permission management             │  │
│  │  • Group invitation system           │  │
│  └──────────────────────────────────────┘  │
│                     │                       │
│  ┌──────────────────────────────────────┐  │
│  │        Offline Cache Layer           │  │
│  │  • Local database read-only mirror   │  │
│  │  • Sync on reconnection              │  │
│  └──────────────────────────────────────┘  │
└────────────────┬───────────────────────────┘
                 │
                 │ Use-Case Modules (Pluggable)
                 │
    ┌────────────┴─────────────────────────────┐
    │                                          │
┌───▼────────────┐              ┌──────────────▼──┐
│  Expense App   │              │  Khata Book App │
│  • Schema      │              │  • Schema       │
│  • UI Forms    │              │  • UI Forms     │
│  • Analytics   │              │  • Analytics    │
│  • Exports     │              │  • Exports      │
└────────────────┘              └─────────────────┘
```

### Core Framework Components

#### 1. Authentication & Authorization Module
**Purpose:** Secure user identity and cloud storage access

**Features:**
- OAuth 2.0 integration with supported cloud providers
- Token management and refresh
- Permission scoping (app-created files only)
- Secure credential storage (platform keychain/keystore)

**Technology Requirements:**
- OAuth 2.0 compliant authentication library
- Secure credential storage (platform keychain/keystore)
- Token refresh handling

---

#### 2. Storage Abstraction Layer
**Purpose:** Generic interface for cloud spreadsheet operations

**Features:**
- Provider-agnostic API for supported spreadsheet services
- CRUD operations on rows
- Batch read/write for performance
- Schema versioning support
- Metadata management

**API Design:**

The SDK must provide the following core storage operations:

1. **Create Sheet** - Initialize a new spreadsheet with defined schema
2. **Append Row** - Add a new transaction record to the sheet
3. **Read Rows** - Retrieve transaction records with optional filtering
4. **Update Row** - Modify existing row data (used for soft deletes only)
5. **Delete Row** - Mark a row as deleted without removing it (soft delete)

Each operation should:
- Return success/failure status
- Handle provider-specific errors gracefully
- Support batch operations where possible for performance

**Implementation:**
- See "Target Spreadsheet Providers" section for supported services
- Request batching and rate limiting
- Error handling and retry logic

---

#### 3. Transaction Engine (Core)
**Purpose:** Ensure data integrity across multi-user operations

**Principles:**
- **Append-only:** New rows added, existing rows never modified directly
- **Audit trail:** All changes recorded with timestamp and author
- **Soft delete:** Deleted rows marked as "deleted", never removed
- **Idempotency:** Same operation executed twice yields same result
- **Eventually consistent:** All users converge to same state

**Transaction Types:**

The framework supports three types of operations:

1. **CREATE** - Add a new transaction record (append a row)
2. **DELETE** - Mark an existing record as deleted (soft delete via update)
3. **REPLACE** - Create a corrected version that supersedes an original (combination of DELETE + CREATE)

**Conflict Resolution:**

The append-only design eliminates most conflicts:

- **Concurrent appends:** No conflicts possible. When two users add transactions simultaneously, both succeed because they're simply appending new rows. The cloud spreadsheet API handles this natively - each append gets the next available row.

- **Soft delete race condition:** If two users try to mark the same row as deleted simultaneously, both updates succeed (the row is marked deleted twice, which is fine - end result is the same).

- **ID collisions:** Impossible. Each record ID combines UUID with nanosecond timestamp, making duplicate identifiers impossible even with concurrent operations from multiple users.

- **Data consistency:** Eventually consistent model - all users see the same final state after sync, even if operations occurred in different orders.

---

#### 4. Sync & Collaboration Module
**Purpose:** Keep all users viewing the same data in real-time

**Strategies:**
- **Polling:** Query sheet every 30 seconds for changes
- **Change detection:** Track last-modified timestamp
- **Optimistic updates:** Show local changes immediately, sync in background
- **Conflict notification:** Alert user if their view is stale

**Sync Algorithm:**
```
1. Local state: Last known rows + timestamp
2. Poll server: Get rows modified after timestamp
3. Merge: Add new rows, update changed rows
4. Notify UI: Display new data with animation
5. Update timestamp: Store new last-sync time
```

**Network optimization:**
- Delta sync (only changed rows)
- Compression for large datasets
- Request coalescing (batch multiple polls)

---

#### 5. Sharing Infrastructure
**Purpose:** Enable easy multi-user group creation and joining

**Features:**
- QR code generation with group metadata
- Deep link support for desktop/web sharing
- Permission request automation
- Group membership management

**Sharing Flow:**
```
1. Creator: Generates QR with {sheetId, groupName, schema}
2. Joiner: Scans QR → extracts metadata
3. Framework: Requests drive permission for specific sheet
4. User: Approves OAuth prompt
5. Framework: Verifies access, loads data
6. User: Now part of group, can read/write
```

**Security:**
- No sensitive data in QR (only sheet ID)
- Permission scoped to specific sheet
- OAuth ensures only authorized users

---

#### 6. Offline Cache Layer
**Purpose:** Enable read-only access without network

**Features:**
- Local database mirror of remote sheet
- Periodic background sync
- Offline indicator in UI
- Queue write operations for later sync

**Data Flow:**
```
Online:  Remote Sheet ←→ Framework ←→ UI
                ↓
            Local Cache

Offline: Local Cache ←→ UI (read-only)
         Write Queue ↓
         
Online:  Write Queue → Remote Sheet
```

---

### Framework Configuration System

Each use case is defined by a **configuration schema:**

**Use Case Configuration Components:**

1. **Schema Definition**
   - Column names and data types (e.g., Amount, Description, Category)
   - Schema version for future migrations

2. **UI Configuration**
   - Entry form: Which fields to show when adding a record
   - List view: How to display records in the feed
   - Detail view: How to show full record details (optional)

3. **Analytics Configuration**
   - Calculations: What aggregations to perform (sum by category, trends, etc.)
   - Visualizations: Charts and graphs to display

4. **Export Configuration**
   - Supported formats (CSV, PDF, etc.)
   - Export templates for each format

5. **Validation Rules**
   - Field requirements (e.g., amount must be positive)
   - Data constraints and error messages

6. **Business Logic** (optional)
   - Custom calculations specific to the use case

**Example: Expense Tracking Configuration**

**Schema:**
- Columns: ID, Timestamp, RecordedBy, Amount, Description, Category, Status
- Required fields: Amount, Description
- Auto-generated: ID, Timestamp, RecordedBy
- Version: 1.0

**UI Configuration:**
- Entry Form: Show Amount (currency input), Description (text), Category (dropdown)
- List View: Display Amount, Description, Category badge, Recorded By, Relative timestamp

**Analytics:**
- Total spending by category (pie chart)
- Daily spending trend (line chart)
- Spending by person breakdown

**Export:**
- Formats: CSV, PDF
- Template: expense_report.csv with columns Date, Category, Amount, Description

**Validation:**
- Amount must be positive number
- Description minimum 3 characters

---

## Technical Specifications

### Target Platforms

The CloudSheetSDK framework is designed to be implemented across multiple platforms:

**Primary Targets:**
- **iOS** - Native mobile applications
- **Android** - Native mobile applications
- **Web** - Browser-based applications (JavaScript)

**Platform-Agnostic Design:**
The framework's core architecture (authentication, storage abstraction, sync engine, transaction management) can be implemented in any language or platform that supports:
- OAuth 2.0 authentication
- HTTP/REST API calls to cloud spreadsheet providers
- Local data storage (caching)
- QR code generation/scanning

**Implementation Approach:**
- Each platform can have its own SDK implementation
- Core concepts and patterns remain consistent
- Platform-specific optimizations encouraged
- Cross-platform code sharing possible but not required

---

### Data Model

#### Generic Transaction Row Structure

Every transaction record in CloudSheetSDK contains these core fields:

- **ID** - Unique identifier (UUID + nanosecond timestamp - collision impossible)
- **Timestamp** - When the record was created (ISO 8601 format)
- **RecordedBy** - Name of the user who created it
- **Status** - Current state: 'active' or 'deleted'
- **ReplacesID** - (Optional) Points to original record if this is a correction

Additional fields are added based on the specific use case (e.g., Amount, Description, Category for expenses).

#### Sheet Metadata

Each spreadsheet stores configuration information:

- **Schema Version** - For handling future schema updates
- **Use Case Type** - Identifies the application (e.g., 'expense_tracking', 'khata_book')
- **Group Name** - Display name for this tracking group
- **Currency** - (Optional) For financial use cases
- **Created At/By** - When and who created this group
- **Members** - List of users with access
- **Last Modified** - Timestamp of most recent change
- **Configuration** - Full use case config for the app

Metadata is stored in:
- Sheet properties (key-value pairs)
- First row of sheet (schema version, use case type)
- Hidden metadata sheet (full configuration)

---

### API Rate Limits & Quotas

**Example: Google Sheets API:**
- Read requests: 100 per 100 seconds per user
- Write requests: 100 per 100 seconds per user
- Total requests: 500 per 100 seconds per project

*Note: Other providers have similar quotas. See "Target Spreadsheet Providers" section for details.*

**Framework Optimizations:**
- Batch reads: Fetch 100 rows at once
- Batch writes: Queue multiple appends
- Debounce: Poll at most every 30 seconds
- Cache: Minimize redundant reads

**Expected usage per user:**
- Read: 2-3 per minute (polling) = ~180/hour → Well under 6,000/hour limit
- Write: 5-10 per day (adding transactions) → Well under 8,640/day limit

**Conclusion:** Normal usage won't hit quotas. Only concern is initial load with 500+ rows.

---

### Security & Privacy

#### Authentication Security
- OAuth 2.0 with PKCE (Proof Key for Code Exchange)
- Tokens stored in device keychain (encrypted)
- Automatic token refresh
- Scoped permissions (app-created files only, not full storage access)

#### Data Privacy
- No data passes through our servers (direct to user's cloud storage)
- User data encrypted at rest by cloud provider
- No tracking pixels or analytics by default
- Optional telemetry requires explicit opt-in

#### Audit Trail
- Every transaction records who and when
- Soft deletes preserve history
- Native version history from cloud provider
- Users can inspect raw spreadsheet anytime

---

### Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| App launch to data load | <3 seconds | Cold start with cached data |
| Add transaction | <500ms | Local write + background sync |
| Sync new data | <5 seconds | From other user's write to display |
| Scroll 500-row list | 60 FPS | Virtual scrolling |
| Export 1000 rows to CSV | <2 seconds | Client-side processing |
| QR scan to join group | <10 seconds | Including OAuth approval |

---

## Framework Extensibility

### Adding a New Use Case

**Step 1: Define Configuration** (30 minutes)

Create a use case configuration that specifies:
- Schema: Column names and types
- UI: Form fields and list views
- Analytics: Calculations and visualizations
- Export: Formats and templates

**Step 2: Register Use Case** (5 minutes)

Register the configuration with the SDK so it knows how to handle this use case type.

**Step 3: Test & Deploy** (varies)
- Framework handles: Auth, storage, sync, sharing
- Developer provides: Schema, UI config, analytics logic
- Reuse: ~80% of code, ~20% use-case specific

**Estimated effort after framework is mature:**
- Simple use case: 1 week
- Medium complexity: 2-3 weeks
- Complex (custom calculations): 3-4 weeks

---

### Framework Plugin System (Future)

**Vision:** Allow third-party developers to create use case plugins

**Plugin Components:**

A plugin package would contain:
- **Name and Version** - Plugin identification
- **Configuration** - Complete use case config (schema, UI, analytics, etc.)
- **Lifecycle Hooks** (optional):
  - Before Write: Transform data before saving
  - After Read: Process data after loading
  - On Sync: Handle sync events

**Usage:**

Developers would install community-created plugins from a plugin repository and activate them in their app.

Example: A community member creates an inventory tracking plugin. Other users can install and use it without building it themselves.

**Benefits:**
- Community can extend framework
- Marketplace of use cases
- Revenue sharing model possible

---

## Business Model & Monetization

### Pricing Strategy (Options to Consider)

**Option 1: Open Source + Donations**
- Framework fully open source
- Accept donations via Open Collective
- Sustainable through community support

**Option 2: Freemium**
- Core framework free
- Premium features paid:
  - Additional cloud provider support
  - Receipt OCR
  - Advanced analytics
  - WhatsApp notifications

**Option 3: Per-Use-Case Licensing**
- Base app: Free
- Expense tracking: Free
- Khata book: $1.99 one-time
- Inventory: $2.99 one-time
- Time tracking: $4.99 one-time

**Option 4: B2B Licensing**
- Consumer use: Free/cheap
- Business use: $49 per organization
- White-label: $499 + rev share

**Recommendation for POC:** Start with Option 1 (open source + donations) to build community trust, evaluate monetization after validation.

---

### Go-to-Market Strategy

**Phase 1: Technical Validation (Months 1-2)**
- Build POC with expense tracking
- Internal testing with team (5-10 people)
- Validate framework architecture works

**Phase 2: Alpha Testing (Month 3)**
- Recruit 20-30 households
- Test multi-user collaboration
- Gather feedback on UI/UX
- Measure performance metrics

**Phase 3: Open Source Launch (Month 4)**
- Release code on GitHub
- Post on Hacker News, Reddit (r/selfhosted, r/privacy)
- Target early adopters: privacy-conscious, tech-savvy
- Collect GitHub stars, contributions

**Phase 4: Second Use Case (Month 5-6)**
- Build khata book mode
- Validate framework extensibility
- Launch in India market
- Partner with micro-entrepreneur networks

**Phase 5: Scale (Month 7+)**
- Add more use cases
- Mobile app store optimization
- Content marketing (blog, videos)
- Community building

---

## Success Metrics

### Framework Validation Metrics
- ✅ POC completed in <8 weeks
- ✅ Second use case built in <2 weeks
- ✅ 80%+ code reuse between use cases
- ✅ No data corruption in multi-user testing
- ✅ Performance targets met

### Product Metrics (Post-Launch)
- **Adoption:** 10,000+ downloads in first 6 months
- **Engagement:** 60%+ MAU/DAU ratio
- **Retention:** 40%+ users active after 3 months
- **Quality:** <5% crash rate, <2% refunds
- **Community:** 100+ GitHub stars, 10+ contributors

### Business Metrics (Year 1)
- **Revenue:** (TBD based on monetization model)
- **CAC:** <$5 per user (organic growth)
- **Support cost:** <$1 per user per year
- **Open source:** 500+ stars, 25+ contributors

---

## Risks & Mitigation Strategies

### Technical Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Spreadsheet API quota limits | High | Medium | Implement aggressive caching, rate limiting, and batching |
| API breaking changes | High | Low | Version detection in schema, graceful degradation, update prompts |
| Multi-user conflict edge cases | Medium | Medium | Comprehensive testing, append-only design prevents most conflicts |
| Poor performance with large datasets | Medium | Low | Virtual scrolling, pagination, query optimization |
| OAuth permission confusion | High | High | Clear onboarding, detailed error messages, video tutorials |

### Business Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Low adoption (framework too complex) | High | Medium | Focus on single use case first, hide framework complexity |
| Feature parity expectations | Medium | High | Clear positioning: "simple by design", not feature competition |
| Unsustainable without revenue | Medium | Medium | Start with donations, explore B2B licensing if needed |
| Cloud provider policy changes | High | Low | Support multiple providers, have local-first backup plan |

### Market Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Existing players add privacy features | Medium | Medium | Open source is hard to compete with, stay ahead on transparency |
| Users uncomfortable with Drive access | Medium | Low | Education on scoped permissions, show what we access |
| Cultural resistance (khata book market) | Medium | Medium | Localization, partnerships with local influencers |

---

## Development Roadmap

### POC Phase (8 weeks)

**Week 1-2: Foundation**
- Framework core architecture
- Authentication module (OAuth with primary provider)
- Storage abstraction layer (spreadsheet API integration)
- Basic transaction engine

**Week 3-4: Expense Tracking Use Case**
- Schema definition for expenses
- Entry form UI
- List view with filters
- Basic analytics dashboard

**Week 5-6: Collaboration Features**
- QR code sharing
- Multi-user sync
- Offline cache
- Conflict resolution testing

**Week 7-8: Polish & Testing**
- Error handling
- Performance optimization
- User testing with 10-15 alpha testers
- Bug fixes

### Post-POC Roadmap

**Months 3-4: Framework Hardening**
- Extract use-case agnostic code into core
- Document extensibility APIs
- Create use case template/generator
- Open source release

**Months 5-6: Second Use Case**
- Build khata book mode
- Validate framework reusability
- Launch in India market
- Gather feedback on framework design

**Months 7-9: Platform Expansion**
- Additional cloud storage provider support
- Web application implementation
- Additional mobile platform optimizations
- Desktop application (if needed)

**Months 10-12: Advanced Features**
- Receipt OCR
- Push notifications
- Accounting software integrations
- Advanced analytics

---

## Open Questions for Discussion

### Technical Decisions
1. Should we support offline editing (with sync) or only offline read?
2. What's the migration strategy when schema versions change?
3. Should we build web app simultaneously or mobile-first?
4. Is a simple local cache sufficient or do we need more robust local-first architecture?

### Product Decisions
1. How do we brand use cases? Separate app listings or unified "CloudSheetSDK" app?
2. Should users be able to create custom use cases in-app (low-code)?
3. Do we need user accounts in addition to OAuth, or is OAuth sufficient?
4. What's the onboarding flow for non-technical users?

### Business Decisions
1. Open source or proprietary framework core?
2. How to handle support if there's no revenue?
3. Should we target businesses or consumers first?
4. Partner with local organizations (India for khata book)?

---


## Use Case Examples

The following use cases demonstrate how CloudSheetSDK can be configured for different domains. These are initial target applications to validate the framework's versatility.

---

### Use Case 1: Expense Tracking

**Description:** Collaborative expense tracking for households and small businesses to maintain transparency over spending.

**Target Users:** Families, roommates, small teams (2-10 people)

**Problem Solved:** Lack of spending visibility, one person tracking everything manually, no easy way to see patterns or share financial transparency.

**Key Features:**
- Record expenses (amount, description, category, date)
- View expense feed with filters (by category, person, date)
- Analytics dashboard (spending by category, by person, trends)
- Export to CSV for accounting
- Soft delete and correction workflow

**Schema:** RecordedBy, Amount, Description, Category, PaymentMethod, Timestamp, Status

**Market:** Global - households, small businesses, roommates (50M+ potential users)

---

### Use Case 2: Khata Book (Credit Ledger)

**Description:** Digital khata book for small shopkeepers to track customer credit and payments, replacing traditional paper ledgers.

**Target Users:** Small shop owners (kirana stores, medical shops, street vendors), service providers

**Problem Solved:** Paper ledgers get lost, customers can't verify balances (trust issues), no backup, manual calculations error-prone.

**Key Features:**
- Add customers and track their credit transactions
- Record credit given and payments received
- Calculate running balance per customer
- Customer-facing view (scan QR to see own balance)
- Analytics: outstanding credit, top debtors, aging report
- Export customer statements

**Schema:** CustomerName, TransactionType (Credit/Payment), Amount, Balance, Description, Timestamp, RecordedBy, Status

**Market:** India/South Asia - 12-15M kirana stores, 80%+ still use paper (20M+ potential users)

**Differentiation vs. Existing Apps:**
- Customer transparency (they can verify their own balance)
- No ads shown to customers
- Data ownership in shopkeeper's cloud storage
- Open source (trust through code visibility)

---

### Future Use Cases (Framework Extensibility)

The same CloudSheetSDK framework can support additional domains with minimal development effort:

1. **Inventory Tracking**
   - Schema: ItemName, Quantity, Type (In/Out), Location, Timestamp, RecordedBy
   - Users: Small retail shops, warehouses, workshop owners

2. **Time & Billable Hours**
   - Schema: ClientName, TaskDescription, Hours, HourlyRate, Date, RecordedBy
   - Users: Freelancers, consultants, agencies

3. **Asset/Tool Management**
   - Schema: AssetName, Action (CheckOut/CheckIn), Person, Location, Timestamp
   - Users: Construction crews, makerspaces, community tool libraries

4. **Project Task Logging**
   - Schema: TaskName, Status, AssignedTo, Priority, DueDate, Timestamp
   - Users: Small teams, community projects, volunteer organizations

5. **Attendance Tracking**
   - Schema: MemberName, Status (Present/Absent), Date, Location, Timestamp
   - Users: Small classes, clubs, volunteer groups

**Development Estimate:** After framework is proven with expense tracking, each additional use case requires 1-2 weeks (vs 6-8 weeks from scratch).

---

## Conclusion

CloudSheetSDK provides a robust, extensible foundation for building collaborative tracking applications across diverse domains. By starting with **Expense Tracking** and **Khata Book** as initial use cases, we validate that a single framework can serve multiple markets while preserving user privacy, data ownership, and simplicity.

**Key Advantages:**
- ✅ One framework → Multiple applications
- ✅ 80%+ code reuse across use cases
- ✅ Zero backend costs
- ✅ Complete user data ownership
- ✅ Privacy-first by design
- ✅ Open source transparency

**Next Steps:**
1. Review and approve this PRD
2. Finalize platform choice (iOS, Android, Web)
3. Begin POC development with Expense Tracking use case
4. Validate framework architecture
5. Build second use case (Khata Book) to prove extensibility

---

**Document End**

*For questions or feedback, contact the product team.*
