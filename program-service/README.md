# Program Service 🚀

> **Core Engine for the Smokefree Cessation Platform**
>
> This service manages the user's entire journey: from personalized program creation and quiz scheduling to habit tracking (streaks) and content delivery.

---

## 📋 Table of Contents
- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [System Architecture](#-system-architecture)
- [Key Business Logic](#-key-business-logic)
- [Database Schema](#-database-schema)
- [Getting Started](#-getting-started)
- [API Authentication](#-api-authentication)

---

## 📖 Overview

The **Program Service** is a state-managed microservice designed to help users quit smoking through a structured, scientifically backed roadmap. It moves beyond simple CRUD operations to handle complex domain logic such as **Streak Recovery**, **Trial Enforcement**, and **Dynamic Quiz Scheduling**.

### Key Features
*   **Personalized Roadmap:** Automatically assigns a 30/45/60-day plan based on user severity (calculated from baseline assessments).
*   **Smart Quiz Engine:** High-performance scheduling for Daily/Weekly quizzes with "Lazy Loading" due date calculation.
*   **Gamification & Tracking:** Tracks smoke-free streaks, manages "slip-ups" (breaks), and allows streak recovery via educational interventions.
*   **Content Management:** Version-controlled content delivery (`ContentModule`) for educational materials.
*   **Trial Enforcement:** "Hard Stop" logic blocks access to paid features immediately upon trial expiration.

---

## 🛠 Tech Stack

*   **Language:** Java 25 (Bleeding Edge / Preview Features Enabled)
*   **Framework:** Spring Boot 3.5.7
*   **Database:** PostgreSQL 14+ (Schema: `program`)
*   **Migration:** Flyway (Current Version: V42)
*   **Build Tool:** Maven (Wrapper included)
*   **Containerization:** Docker (Eclipse Temurin 25 Alpine)
*   **Utilities:** Lombok 1.18.42, Hibernate Types 60

---

## 🏗 System Architecture

The project follows a **Layered Architecture** with strict separation of concerns:

```
src/main/java/com/smokefree/program
├── auth/           # Security filters (HeaderUserContextFilter)
├── config/         # App configurations (Security, CORS, Props)
├── domain/         
│   ├── model/      # JPA Entities (Program, QuizAssignment, Streak...)
│   ├── repo/       # Spring Data Repositories
│   └── service/    # Business Logic Interfaces & Implementations
└── web/            
    ├── controller/ # REST Endpoints
    ├── dto/        # Data Transfer Objects
    └── error/      # Global Exception Handling
```

---

## 🧠 Key Business Logic

### 1. Program Creation & Auto-Assignment
When a user starts a program (`ProgramServiceImpl`), the system:
1.  Reads the `UserBaselineResult` to determine addiction severity.
2.  Selects a `PlanTemplate` (e.g., `L1_30D`).
3.  **Auto-Assigns Quizzes:** Reads `PlanQuizSchedule` and generates `QuizAssignment` records for the user. These act as "future promises" for content delivery.

### 2. The "Hard Stop" Trial Logic
The system enforces subscription limits passively but strictly.
*   **Logic:** In `ProgramService.getActive()`, the system checks `Instant.now() > trialEndExpected`.
*   **Outcome:** If expired, a `SubscriptionRequiredException` is thrown immediately, blocking the request before any data is returned.

### 3. Streak Recovery (The "Killer Feature")
We treat relapse as part of the journey, not the end.
*   **Flow:** User breaks a streak -> `StreakBreak` record created.
*   **Recovery:** User completes a specific "Recovery Quiz".
*   **Magic:** Upon submission (`QuizFlowServiceImpl.submit`), the system locates the latest `StreakBreak` and "heals" the historical `Streak` record by removing its `endedAt` timestamp.

### 4. Quiz Engine Optimization
To prevent **N+1 queries** when listing due quizzes:
*   The system fetches all Assignments, Results, and Templates in parallel batches.
*   Due dates (`ONCE` vs `RECURRING` logic) are calculated in-memory, ensuring O(1) database round-trips for the dashboard view.

---

## 🗄 Database Schema

The service uses a dedicated schema named **`program`**. Key tables include:

*   **`programs`**: The root aggregate. Stores status, start date, and current streak counters.
*   **`quiz_templates`**: Master data for questions and answers.
*   **`quiz_assignments`**: Links a User/Program to a Template with scheduling rules.
*   **`quiz_attempts`** & **`quiz_answers`**: Stores user submissions. Note: `quiz_answers` uses a composite key `(attempt_id, question_no)`.
*   **`streaks`** & **`streak_breaks`**: Tracks continuous smoke-free periods.
*   **`smoke_events`**: Logs of individual smoking incidents.

*Migrations are managed via Flyway in `src/main/resources/db/migration`.*

---

## 🚀 Getting Started

### Prerequisites
*   **JDK 25** (Required for compilation)
*   Docker & Docker Compose (for PostgreSQL)

### Run Locally

1.  **Start Database:**
    ```bash
    docker-compose up -d postgres
    ```

2.  **Build Project:**
    ```bash
    ./mvnw clean install
    ```

3.  **Run Application:**
    ```bash
    ./mvnw spring-boot:run
    ```
    *The app will start on port `8080` (default).*

---

## 🔐 API Authentication

This service is designed to run behind an API Gateway. It is **Stateless**.

*   **Mechanism:** It trusts the upstream gateway to perform authentication.
*   **Identification:** It relies on the **`X-User-Id`** HTTP Header.
*   **Context:** The `HeaderUserContextFilter` extracts this ID and creates a `UserPrincipal` for the security context.

**Example Request:**
```http
GET /v1/programs/active
X-User-Id: a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
X-User-Tier: PREMIUM  <-- Optional, for entitlement checks
```

---
*© 2024 Smokefree Project. Internal Use Only.*
