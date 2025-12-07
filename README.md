# Expense Manager - JavaFX Desktop Application

A comprehensive personal expense tracking application built with JavaFX and SQLite, featuring multi-currency support, visual analytics, and data export capabilities.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage Guide](#usage-guide)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Architecture](#architecture)

## Features

### Core Functionality

- **User Authentication**: Secure registration and login system with user-specific expense tracking
- **Full CRUD Operations**: Create, Read, Update, and Delete expense records
- **Smart Category Normalization**: Fuzzy matching with Levenshtein distance algorithm to standardize expense categories
- **Input Validation**: Robust validation for dates (ISO format), amounts (non-negative numbers), and required fields

### Visualization & Analytics

- **Multiple Chart Types**:

  - Pie Chart (category distribution)
  - Stacked Bar Chart (monthly trends by category)
  - Bar Chart (total by category)
  - Line Chart (expense trends over time)
  - Area Chart (cumulative spending visualization)
  - Scatter Chart (expense distribution)
  - Donut Chart (category breakdown)

- **Interactive Calendar View**:
  - Monthly calendar with per-day spending totals
  - Visual highlighting of top 3 spending days
  - Click any day to view detailed expense breakdown
- **Dashboard Cards**: Quick overview of total spending, top category, and average expense

### Data Management

- **Multi-Currency Support**: Convert and display expenses in USD, EUR, VND, JPY, or GBP
- **CSV Import/Export**: Bulk import expenses from CSV files
- **Multiple Export Formats**:
  - Plain Text (TXT)
  - CSV (structured data)
  - PDF (formatted reports)
  - Excel (XLSX with multiple sheets)
  - JSON (machine-readable format)

### User Experience

- **Dark/Light Theme Toggle**: CSS-based theme switching for comfortable viewing
- **Animated Robot Mascot**: Engaging UI element with animated movement and quotes
- **Responsive Layout**: Three-panel design (sidebar, main content, visualization sidebar)
- **Text Wrapping**: Auto-expanding description fields in table view

## Technology Stack

### Core Technologies

- **Java 17+** (with module system support)
- **JavaFX** (controls, fxml, graphics modules)
- **SQLite 3.42.0.0** (via JDBC)
- **Maven** (build automation and dependency management)

### Key Libraries

- **Apache POI 5.2.3** (Excel file generation)
- **iText/OpenPDF 1.3.30** (PDF generation)
- **JSON-java 20230227** (JSON export)

### Development Tools

- **Maven JavaFX Plugin** (for running JavaFX applications)
- **VS Code / IntelliJ IDEA** (recommended IDEs)

## Prerequisites

Before running the application, ensure you have:

1. **Java Development Kit (JDK) 17 or higher**

   ```bash
   java -version
   ```

2. **Apache Maven 3.6+**

   ```bash
   mvn -version
   ```

3. **JavaFX SDK** (included in project dependencies via Maven)

## Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/saladnga/CS3360Java-ExpenseManager.git
   cd CS3360Java-ExpenseManager
   ```

2. **Install dependencies**

   ```bash
   mvn clean install
   ```

3. **Verify the build**

   ```bash
   mvn compile
   ```

## Running the Application

### Using Maven (Recommended)

```bash
mvn javafx:run
```

### Using Java Command Line

```bash
java --module-path lib --add-modules javafx.controls,javafx.fxml \
     -cp target/classes:lib/sqlite-jdbc-3.42.0.0.jar \
     com.expense.ExpenseManager_MainApp
```

### Running from IDE

1. Open the project in your IDE
2. Set the main class to `com.expense.ExpenseManager_MainApp`
3. Add VM arguments: `--module-path lib --add-modules javafx.controls,javafx.fxml`
4. Run the application

## Usage Guide

### First Launch

1. **Register a new account**

   - Click "Register" on the login screen
   - Enter a unique username and password
   - Click "Create Account"

2. **Login**
   - Enter your credentials
   - Click "Login"

### Creating Expenses

1. Click **"Create Expense"** in the sidebar
2. Fill in the form:
   - **Name**: Required, expense description
   - **Date**: Required, format YYYY-MM-DD (e.g., 2025-12-06)
   - **Amount**: Required, non-negative number
   - **Category**: Optional, auto-normalized (e.g., "food" → "Food & Drinks")
   - **Description**: Optional, detailed notes
3. Click **"Save"**

### Viewing and Managing Expenses

- **Refresh**: Click "Refresh" to reload all expenses
- **Update**: Enter expense ID, click "Fetch", modify fields, click "Save"
- **Delete**: Enter expense ID and click "Delete"
- **Clear All**: Use "Clear All Data" button (caution: irreversible)

### Currency Conversion

1. Select currency from the dropdown (USD, EUR, VND, JPY, GBP)
2. All amounts automatically convert and update

### Generating Reports

1. Click **"Generate Report"** in the sidebar
2. Review the report in the text area
3. Select export format (TXT, CSV, PDF, Excel, JSON)
4. Click **"Export"** and choose save location

### Using the Calendar View

1. Navigate to the right sidebar
2. Select month and year from dropdowns
3. Click any day cell to view detailed expenses for that date
4. Days with spending are highlighted (top 3 days in red)

### Importing CSV Data

1. Click **"Import CSV"** in the sidebar
2. Select a CSV file with columns: Date, Name, Amount, Category, Description
3. Data is automatically validated and imported

## Project Structure

```
project/
├── src/
│   ├── main/
│   │   ├── java/com/expense/
│   │   │   ├── ExpenseManager_MainApp.java    # Main application entry
│   │   │   ├── DatabaseHandler.java           # Database operations (DAO)
│   │   │   ├── Expense.java                   # Expense model/entity
│   │   │   ├── User.java                      # User model
│   │   │   ├── ChartService.java              # Chart generation logic
│   │   │   ├── CalendarPane.java              # Calendar view component
│   │   │   ├── CurrencyConverter.java         # Currency conversion
│   │   │   ├── CSVHandler.java                # CSV import/export
│   │   │   └── ReportGenerator.java           # Report generation
│   │   └── resources/
│   │       ├── app.css                         # Light theme
│   │       ├── dark.css                        # Dark theme
│   │       ├── robot_animation.png             # Animated mascot
│   │       └── *.fxml                          # FXML layouts
│   └── javafx.*/                               # JavaFX source modules
├── lib/                                         # JavaFX JARs and dependencies (need to install JavaFX, SQLite Driver to run)
├── target/                                      # Compiled classes
├── pom.xml                                      # Maven configuration
└── expenses.db                                  # SQLite database (auto-generated)
```

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL
);
```

### Expenses Table

```sql
CREATE TABLE expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,              -- ISO format: YYYY-MM-DD
    name TEXT NOT NULL,
    amount REAL NOT NULL,            -- Stored in USD
    category TEXT,                   -- Normalized categories
    description TEXT,
    user_id INTEGER,
    FOREIGN KEY(user_id) REFERENCES users(id)
);
```

### Normalized Categories

- Food & Drinks
- Utilities
- Personal Care
- Entertainment
- Education
- Health
- Transportation
- Electronics
- Sports
- Other (default for unmatched)

## Architecture

### Three-Layer Architecture

#### 1. UI Layer (Presentation)

- **Components**: JavaFX controls, FXML views, CSS styling
- **Classes**: `ExpenseManager_MainApp`, `CalendarPane`
- **Responsibilities**:
  - Render user interface
  - Handle user input and events
  - Display data from service layer
  - Apply themes and styling

#### 2. Application/Service Layer (Business Logic)

- **Classes**: `ChartService`, `CurrencyConverter`, `ReportGenerator`, `CSVHandler`
- **Responsibilities**:
  - Business logic and data transformation
  - Currency conversion calculations
  - Chart data aggregation
  - Report formatting
  - CSV parsing and generation

#### 3. Data Layer (Persistence)

- **Classes**: `DatabaseHandler` (DAO)
- **Responsibilities**:
  - CRUD operations via JDBC
  - SQL query execution
  - Connection management
  - Data validation at database level
  - Category normalization

### Design Patterns Used

- **Model-View-Controller (MVC)**: Separation of data, UI, and logic
- **Data Access Object (DAO)**: `DatabaseHandler` encapsulates database operations
- **Singleton-like**: Single `DatabaseHandler` connection per session
- **Strategy Pattern**: Multiple chart types via `ChartService` methods
- **Observer Pattern**: JavaFX `ObservableList` for table updates

## Key OOP Concepts Demonstrated

### Encapsulation

- Private fields with public getters/setters in `Expense` and `User` classes
- Encapsulated database connection in `DatabaseHandler`

### Inheritance

- `CalendarPane extends VBox` (JavaFX component inheritance)
- `ExpenseManager_MainApp extends Application` (JavaFX lifecycle)

### Polymorphism

- Multiple chart creation methods with consistent interfaces
- Event handlers using lambda expressions

### Abstraction

- Service layer abstracts business logic from UI
- DAO pattern abstracts SQL details from application logic

## Development Notes

### Building from Source

```bash
# Clean build
mvn clean compile

# Run tests (when implemented)
mvn test

# Package as JAR
mvn package

# Generate site documentation
mvn site
```

## Authors

- **Development Team**: CS-3360 Project Group - [Vu Hoang (saladnga)](https://github.com/saladnga), [Quan Nguyen (NguyenQuan297)](https://github.com/NguyenQuan297)
- **Repository**: [saladnga/CS3360Java-ExpenseManager](https://github.com/saladnga/CS3360Java-ExpenseManager)
- **Course**: CS-3360 - Object-Oriented Programming with Java
