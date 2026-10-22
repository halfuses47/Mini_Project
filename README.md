# MiniBank — Core Banking System

A robust, fault-tolerant, concurrent, and persistent Core Banking Application developed in pure Java (standard library only) as part of the Advanced Java Course Project (Milestones 1, 2, and 3).

---

## Architecture & Features

### 1. Model & Validation Layer
- **Account Hierarchy**: Base abstract class `Account` with subclasses `SavingsAccount`, `CurrentAccount`, and `FixedDepositAccount`.
- **Annotations**:
  - `@Id`: Marks unique identification fields (`accountNumber`, `customerId`).
  - `@Positive`: Validates numeric fields (e.g., positive balance) with custom failure messages.
  - `@MaxLength`: Validates string length constraints.
  - `@Audit`: Method-level annotation for reflective audit logging.
- **Reflection Validation**: `AnnotationValidator` dynamically inspects and validates fields at runtime across the class hierarchy.

### 2. Fault Tolerance & Custom Exception Hierarchy (`package exception`)
- **`BankException`**: Base checked exception.
- **`InsufficientFundsException`**: Contains `long shortfall` field indicating exact missing funds.
- **`InvalidAmountException`**: Thrown for non-positive or invalid amounts.
- **`AccountNotFoundException`**: Thrown when looking up non-existent accounts.
- **Atomic Operations**: Safe deposit, withdrawal, and transfer with rollback and `try-catch-finally` handling.

### 3. Multithreading, Concurrency, and Deadlock Prevention
- **Race Condition Prevention**: `deposit()` and `withdraw()` are synchronized for thread-safe concurrent modifications.
- **Thread Pool Execution**: `TransactionProcessor` manages a 4-thread pool via `ExecutorService` for high-throughput batch transaction processing.
- **Producer-Consumer Coordination**: `TransactionBuffer` coordinates incoming transactions and processing workers using circular array buffers with `wait()` and `notify()`.
- **Deadlock Avoidance via Consistent Lock Ordering**: `safeTransfer()` consistently acquires locks in alphabetical order of `accountNumber` (smaller account number locked first), preventing circular wait conditions.

### 4. Storage & Collections Framework (`package util`)
- **`AccountStore`**: Uses `HashMap<String, Account>` for $O(1)$ account lookup and `ArrayList<String>` for tracking transaction histories.
- **Sorting**:
  - `Comparable<Account>`: Natural ordering by account number.
  - `Comparator<Account>`: Custom sorting by account balance (`sortedByBalance()`).
- **Thread-Safe Map**: `ConcurrentHashMap` integrated inside `TransactionProcessor` for multi-threaded shared access.

### 5. Generic Repository Layer
- **`Repository<T>`**: Reusable generic data-access repository supporting any entity type.
- **Reflection-Based Key Resolution**: `save()` automatically extracts the `@Id` annotated field as the map key.
- **`@SafeVarargs`**: Method `saveAll(T... items)` allows bulk persistence safely.
- **Bounded Generics**: Generic utility method `<N extends Number> double average(N[] values)`.

### 6. Persistence & NIO Reporting
- **Serialization**: `StatePersister` saves and reloads accounts to `data/accounts.ser` using `ObjectOutputStream` and `ObjectInputStream` with `try-with-resources`.
- **Append-Only Logs**: `TransactionLog` writes transactions using NIO `Files.write` with `CREATE` and `APPEND`.
- **NIO End-of-Day Report**: `ReportGenerator` walks the logs directory with `Files.newDirectoryStream`, inspects file attributes (`Files.readAttributes`), reads streams via `BufferedReader`, aggregates total deposits/withdrawals, and generates formatted reports.

### 7. Service Layer & Interactive Terminal
- **`BankService`**: Central business logic service implementing `openAccount`, `deposit`, `withdraw`, `transfer`, `statement`, and `listAccounts`.
- **`MiniBank` (Main Application)**: Interactive 9-option portal that connects user inputs to `BankService`, automatically reloads state on startup, and saves on exit.

---

## Project Structure

```text
25CE079_Mini-project/
├── exception/
│   ├── BankException.java
│   ├── InsufficientFundsException.java
│   ├── InvalidAmountException.java
│   └── AccountNotFoundException.java
├── model/
│   ├── Account.java
│   ├── SavingsAccount.java
│   ├── CurrentAccount.java
│   ├── FixedDepositAccount.java
│   ├── Customer.java
│   ├── Transactable.java
│   ├── InterestBearing.java
│   ├── TransactionType.java
│   ├── WithdrawRule.java
│   └── annotation/
│       ├── Id.java
│       ├── Positive.java
│       ├── MaxLength.java
│       └── Audit.java
├── service/
│   ├── BankService.java
│   ├── AuditLogger.java
│   └── MiniBank.java           <-- Main Application Entry Point
├── util/
│   ├── Repository.java
│   ├── AccountStore.java
│   ├── AccountWorker.java
│   ├── TransactionProcessor.java
│   ├── TransactionBuffer.java
│   ├── StatePersister.java
│   ├── TransactionLog.java
│   ├── ReportGenerator.java
│   └── AnnotationValidator.java
├── test/
│   ├── Test.java               <-- @Test Annotation
│   └── MiniBankTest.java       <-- Test Suite
├── Main.java                   <-- Milestone Demonstration Driver
├── manifest.txt
├── minibank.jar                <-- Packaged Executable JAR
└── README.md
```

---

## Compilation & Execution

### 1. Compile the Project
```bash
javac -d out exception/*.java model/annotation/*.java model/*.java util/*.java service/*.java test/*.java Main.java
```

### 2. Run the Main Interactive Banking Portal
```bash
java -cp out service.MiniBank
```
*Or run via the packaged JAR:*
```bash
java -jar minibank.jar
```

### 3. Run Milestone Demonstrations
```bash
java -cp out Main
```

### 4. Run the Test Suite
```bash
java -cp out test.MiniBankTest
```
Expected output:
```text
=================================================
            RUNNING MINIBANK TEST SUITE          
=================================================
Running test: testWithdrawalOverLimitThrowsException ... [PASS]
Running test: testConcurrentDepositsKeepBalanceCorrect ... [PASS]
Running test: testDepositIncreasesBalance ... [PASS]
-------------------------------------------------
Results: 3 run, 3 passed, 0 failed.
=================================================
```

---

## Interactive Portal Menu Options

1. **Open New Account**: Prompts for type (Savings/Current/FD), holder name, opening balance, and constraints.
2. **Deposit Money**: Updates balance and writes to transaction log.
3. **Withdraw Money**: Checks funds/limits and handles overdraft/exceptions.
4. **Transfer Funds**: Performs thread-safe atomic transfer between two accounts with deadlock protection.
5. **View Account Statement**: Prints full account status and interest rate.
6. **List All Accounts**: Displays all registered accounts in the system.
7. **Safe Batch Transaction Processing**: Dispatches concurrent batches to a 4-thread pool.
8. **Generate End-of-Day Summary Report**: Aggregates all logs in `logs/` and outputs financial summary report.
9. **Exit Application**: Automatically serializes and saves all accounts to `data/accounts.ser`.
