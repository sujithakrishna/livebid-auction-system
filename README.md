# 🔨 LiveBid --- Concurrent Live Auction System

```{=html}
<p align="center">
```
`<strong>`{=html}A Java + MySQL live auction system focused on
concurrent bidding, optimistic locking, database consistency, and
layered backend design.`</strong>`{=html}
```{=html}
</p>
```
```{=html}
<p align="center">
```
`<img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21"/>`{=html}
`<img src="https://img.shields.io/badge/MySQL-8.0%2B-blue?style=for-the-badge&logo=mysql" alt="MySQL"/>`{=html}
`<img src="https://img.shields.io/badge/JDBC-Database%20Connectivity-green?style=for-the-badge" alt="JDBC"/>`{=html}
`<img src="https://img.shields.io/badge/Maven-Build%20Tool-red?style=for-the-badge&logo=apachemaven" alt="Maven"/>`{=html}
`<img src="https://img.shields.io/badge/Concurrency-Optimistic%20Locking-purple?style=for-the-badge" alt="Concurrency"/>`{=html}
```{=html}
</p>
```

------------------------------------------------------------------------

## 📌 Overview

**LiveBid** is a Java and MySQL based live auction system designed to
model a backend auction workflow where multiple users can participate in
the same auction and place bids concurrently.

The main technical focus of the project is **concurrency control**. When
multiple bidders attempt to update the same auction at nearly the same
time, the system uses **optimistic locking with version checking** to
detect conflicting updates instead of allowing one update to silently
overwrite another.

The project uses a layered structure consisting of models, services,
DAOs, JDBC database connectivity, custom exception handling, and MySQL
persistence.

------------------------------------------------------------------------

## 🎯 Project Objectives

LiveBid was built to demonstrate practical backend concepts beyond basic
CRUD operations:

-   Java object-oriented programming
-   Layered application architecture
-   DAO and service-layer separation
-   JDBC-based MySQL connectivity
-   Auction and bid management
-   Bid validation
-   Highest-bid tracking
-   Auction state management
-   Concurrent bid processing
-   Optimistic locking
-   Version-based conflict detection
-   Retry handling for concurrent updates
-   Notification persistence
-   Exception handling
-   Database consistency

------------------------------------------------------------------------

# ✨ Key Features

## 👤 User Management

The system supports user-related operations required for participating
in auctions.

-   User creation
-   User identification
-   User retrieval
-   Association of users with bids and auction activity

## 🔨 Auction Management

-   Create auctions
-   Store auction details
-   Define starting price
-   Track the current highest bid
-   Maintain auction status
-   Maintain auction version
-   Retrieve auction information

## 💰 Bidding

-   Place bids
-   Validate bid amounts
-   Compare bids against the current highest bid
-   Update the highest bid
-   Persist bid history
-   Associate bids with users and auctions

## ⚡ Concurrent Bidding

Multiple bidders can attempt to place bids against the same auction.

The system is designed to detect conflicting updates using optimistic
locking rather than allowing concurrent requests to overwrite auction
state incorrectly.

## 🔐 Optimistic Locking

Each auction maintains a version value.

Example:

``` text
Auction ID       : 1
Current Bid      : ₹25,000
Version          : 1
Status           : LIVE
```

A bidder attempts to update the auction using the version that was read.

If another bidder has already updated the auction:

``` text
Expected Version : 1
Database Version : 2
```

the update does not silently overwrite the newer state. The application
detects the conflict and handles it through the retry/conflict-handling
flow.

## 🔔 Notifications

The project includes notification persistence associated with users and
auctions.

Notification records can be used as the foundation for future real-time
or external notification mechanisms.

------------------------------------------------------------------------

# 🏗️ Architecture

LiveBid follows a layered backend architecture.

``` text
                         ┌────────────────────────┐
                         │         Main           │
                         │   Application Entry    │
                         └────────────┬───────────┘
                                      │
                                      ▼
                         ┌────────────────────────┐
                         │       Service Layer    │
                         ├────────────────────────┤
                         │ AuctionService         │
                         │ BidService             │
                         │ UserService            │
                         │ NotificationService    │
                         └────────────┬───────────┘
                                      │
                                      ▼
                         ┌────────────────────────┐
                         │         DAO Layer      │
                         ├────────────────────────┤
                         │ AuctionDAO             │
                         │ BidDAO                 │
                         │ UserDAO                │
                         │ NotificationDAO        │
                         └────────────┬───────────┘
                                      │
                                      ▼
                         ┌────────────────────────┐
                         │          JDBC          │
                         │ Database Connectivity  │
                         └────────────┬───────────┘
                                      │
                                      ▼
                         ┌────────────────────────┐
                         │         MySQL          │
                         │   Persistent Storage    │
                         └────────────────────────┘
```

### Responsibility of Each Layer

**Model Layer**

Represents application entities and domain values.

**Service Layer**

Contains business logic and coordinates operations between models and
DAOs.

**DAO Layer**

Contains database access logic using JDBC.

**Configuration Layer**

Provides database connection configuration.

**Exception Layer**

Contains application-specific exceptions, including optimistic-lock
conflicts.

------------------------------------------------------------------------

# 📂 Project Structure

``` text
livebid/
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── sujitha/
│                   └── livebid/
│
│                       ├── Main.java
│                       │
│                       ├── config/
│                       │   └── DatabaseConnection.java
│                       │
│                       ├── dao/
│                       │   ├── AuctionDAO.java
│                       │   ├── BidDAO.java
│                       │   ├── NotificationDAO.java
│                       │   └── UserDAO.java
│                       │
│                       ├── exception/
│                       │   └── OptimisticLockException.java
│                       │
│                       ├── model/
│                       │   ├── Auction.java
│                       │   ├── AuctionStatus.java
│                       │   ├── Bid.java
│                       │   ├── Notification.java
│                       │   └── User.java
│                       │
│                       └── service/
│                           ├── AuctionService.java
│                           ├── BidService.java
│                           ├── NotificationService.java
│                           └── UserService.java
│
├── pom.xml
├── .gitignore
└── README.md
```

------------------------------------------------------------------------

# 🧩 Core Components

## Model

### `Auction`

Represents an auction and its current state, including:

-   Auction ID
-   Item/product information
-   Starting price
-   Current highest bid
-   Auction status
-   Version

### `Bid`

Represents an individual bid placed by a user for an auction.

### `User`

Represents a participant in the auction system.

### `Notification`

Represents a persisted notification associated with auction activity.

### `AuctionStatus`

Represents the current lifecycle state of an auction.

------------------------------------------------------------------------

# 🗄️ DAO Layer

The DAO layer separates database operations from business logic.

### `AuctionDAO`

Responsible for auction persistence and version-aware auction updates.

### `BidDAO`

Responsible for bid persistence and bid-related database operations.

### `UserDAO`

Responsible for user persistence and retrieval.

### `NotificationDAO`

Responsible for notification persistence and retrieval.

This separation keeps SQL/JDBC operations outside the service layer and
makes the application easier to maintain.

------------------------------------------------------------------------

# ⚙️ Service Layer

## `AuctionService`

Handles auction-related business operations such as:

-   Auction creation
-   Auction retrieval
-   Auction state management

## `BidService`

Contains the core bidding logic:

``` text
Receive Bid
    ↓
Validate Bid
    ↓
Read Current Auction State
    ↓
Check Highest Bid
    ↓
Read Auction Version
    ↓
Attempt Version-Aware Update
    ↓
Update Successful?
   /             \
 YES              NO
  |                |
  ▼                ▼
Accept Bid     Optimistic Lock
                  Conflict
                     |
                     ▼
               Retry / Handle
```

## `UserService`

Handles user-related business operations.

## `NotificationService`

Handles notification-related business operations.

------------------------------------------------------------------------

# ⚡ Concurrency Design

Concurrency is the primary technical focus of LiveBid.

Consider an auction with:

``` text
Current Highest Bid = ₹25,000
Version             = 1
```

Two bidders attempt to place bids at approximately the same time:

``` text
Bidder #2 → ₹27,000
Bidder #3 → ₹28,000
```

Both operations may initially read the same auction version.

The system uses the auction version to detect whether the auction has
changed between reading and updating.

Conceptually, the database update follows this pattern:

``` sql
UPDATE auction
SET highest_bid = ?,
    version = version + 1
WHERE id = ?
  AND version = ?;
```

If the expected version no longer matches the database version, the
update is considered a conflict.

This prevents a stale transaction from blindly overwriting a newer
auction state.

------------------------------------------------------------------------

# 🔐 Optimistic Locking Flow

``` text
              Read Auction
                   │
                   ▼
          Read Current Version
                   │
                   ▼
             Validate Bid
                   │
                   ▼
        Attempt Database Update
                   │
             ┌─────┴─────┐
             │           │
          Success      Conflict
             │           │
             ▼           ▼
        Bid Accepted   Version Mismatch
                         │
                         ▼
                 OptimisticLockException
                         │
                         ▼
                   Retry / Handle
```

The project contains a custom:

``` text
OptimisticLockException
```

to represent optimistic-lock conflicts explicitly.

------------------------------------------------------------------------

# 🧪 Concurrency Testing

The system was tested using concurrent bidding scenarios.

Example:

``` text
Auction:
Sony WH-1000XM5 Headphones

Starting Price:
₹25,000

Concurrent Bidders:
Bidder #2
Bidder #3
```

A representative execution flow is:

``` text
Auction Created
      ↓
Version = 1
      ↓
Bidder #2 and Bidder #3 attempt bids
      ↓
One transaction successfully updates the auction
      ↓
Auction version increments
      ↓
The competing transaction detects a version mismatch
      ↓
Optimistic lock conflict is detected
      ↓
Retry / conflict handling is performed
```

This demonstrates how optimistic locking can be used to protect shared
auction state when multiple users compete for the same resource.

------------------------------------------------------------------------

# 🗃️ Database Design

MySQL is used for persistent storage.

The application contains data associated with:

``` text
Users
Auctions
Bids
Notifications
```

The Java application communicates with MySQL through JDBC.

Database connectivity is centralized through:

``` text
src/main/java/com/sujitha/livebid/config/DatabaseConnection.java
```

The DAO classes use the database connection to execute SQL operations.

------------------------------------------------------------------------

# 🛠️ Technology Stack

  Technology      Purpose
  --------------- -------------------------------------
  Java 21         Core application and business logic
  MySQL           Relational database
  JDBC            Database connectivity
  Maven           Build and dependency management
  IntelliJ IDEA   Development environment
  Git             Version control
  GitHub          Source code hosting

------------------------------------------------------------------------

# 📋 Prerequisites

Install the following before running the project:

-   JDK 21 or compatible JDK
-   MySQL Server
-   MySQL Workbench or MySQL client
-   Maven
-   Git
-   IntelliJ IDEA or another Java IDE

------------------------------------------------------------------------

# 🚀 How to Run

## 1. Clone the Repository

``` bash
git clone https://github.com/sujithakrishna/livebid-auction-system.git
```

Then:

``` bash
cd livebid-auction-system
```

## 2. Create the MySQL Database

Create the database and required tables in MySQL according to the schema
expected by the application.

Make sure the MySQL server is running before starting the Java
application.

## 3. Configure Database Credentials

Open:

``` text
src/main/java/com/sujitha/livebid/config/DatabaseConnection.java
```

Configure the connection details for your local MySQL installation:

``` text
Database URL
Username
Password
```

Do not commit real passwords or other credentials to GitHub.

For a production application, these values should be supplied through
environment variables or an external configuration mechanism.

## 4. Build the Project

From the project root:

``` bash
mvn clean package
```

## 5. Run the Application

Run:

``` text
src/main/java/com/sujitha/livebid/Main.java
```

from IntelliJ IDEA or your preferred Java environment.

------------------------------------------------------------------------

# 🔄 Application Workflow

The overall auction workflow is:

``` text
User
  │
  ▼
Create / Access Auction
  │
  ▼
Auction Status = LIVE
  │
  ▼
Place Bid
  │
  ▼
Validate Bid
  │
  ▼
Read Current Auction State
  │
  ▼
Validate Version
  │
  ├───────────────┐
  │               │
  ▼               ▼
Valid           Conflict
  │               │
  ▼               ▼
Update        Retry / Handle
Auction
  │
  ▼
Increment Version
  │
  ▼
Persist Bid
  │
  ▼
Create Notification
```

------------------------------------------------------------------------

# 📊 Example Scenario

An auction starts with:

``` text
Product          : Sony WH-1000XM5 Headphones
Starting Price   : ₹25,000
Status           : LIVE
Version          : 1
```

A bidder places:

``` text
Bid = ₹27,000
```

If the version is still valid:

``` text
Highest Bid → ₹27,000
Version     → 2
```

If another bidder had already updated the auction:

``` text
Expected Version → 1
Actual Version   → 2
```

the stale update is rejected as a concurrency conflict rather than
overwriting the latest auction state.

------------------------------------------------------------------------

# 💡 Why Optimistic Locking?

Optimistic locking is useful when conflicts are possible but continuous
database locking is undesirable.

Instead of locking the auction row for the entire bidding operation, the
application assumes that conflicts are relatively infrequent and
verifies the version at update time.

This approach provides:

-   Conflict detection
-   Protection against lost updates
-   Better concurrency
-   Explicit handling of stale data
-   A foundation for scalable concurrent operations

------------------------------------------------------------------------

# 📈 Future Enhancements

The current Java + MySQL implementation can be extended into a larger
production-style auction platform.

Potential enhancements include:

-   REST APIs
-   WebSocket-based real-time bidding
-   Authentication and authorization
-   JWT security
-   Redis caching
-   Message queues
-   Real-time notifications
-   Auction scheduling
-   Automatic auction closing
-   Bid history dashboards
-   Email notifications
-   Distributed locking
-   Docker deployment
-   Cloud deployment
-   Centralized logging
-   Monitoring and observability
-   Automated tests

These are future extensions and are not part of the current Java + MySQL
implementation.

------------------------------------------------------------------------

# 🎓 What This Project Demonstrates

LiveBid demonstrates practical understanding of:

-   Core Java
-   Object-Oriented Programming
-   Java Collections and exception handling
-   JDBC
-   MySQL
-   SQL-based persistence
-   DAO pattern
-   Service-layer architecture
-   Separation of concerns
-   Concurrent programming concepts
-   Optimistic locking
-   Version-based conflict detection
-   Retry mechanisms
-   Transaction consistency
-   Shared-state concurrency
-   Git and GitHub

------------------------------------------------------------------------

# ⭐ Project Highlights

  Area                Implementation
  ------------------- -------------------------------
  Language            Java
  Database            MySQL
  Connectivity        JDBC
  Architecture        Model + Service + DAO
  Concurrency         Optimistic locking
  Conflict handling   Version checking + retry flow
  Persistence         MySQL
  Build               Maven
  Version Control     Git
  Repository          GitHub

------------------------------------------------------------------------

# 👩‍💻 Author

**Sujitha V K**

Java • SQL • Backend Development

GitHub:\
https://github.com/sujithakrishna

LinkedIn:\
https://www.linkedin.com/in/sujitha-v-k-77a924258/

------------------------------------------------------------------------

# 🔗 Repository

**LiveBid --- Concurrent Live Auction System**

https://github.com/sujithakrishna/livebid-auction-system

------------------------------------------------------------------------

```{=html}
<p align="center">
```
`<strong>`{=html}Built with Java ☕ • MySQL 🗄️ • JDBC 🔌 • Concurrency
⚡`</strong>`{=html}
```{=html}
</p>
```
