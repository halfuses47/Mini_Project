// 25CE079
import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.SavingsAccount;
import model.CurrentAccount;
import model.FixedDepositAccount;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import util.*;

// A small custom AutoCloseable class to demonstrate try-with-resources
class BankSession implements AutoCloseable {
    private final String sessionName;

    public BankSession(String sessionName) {
        this.sessionName = sessionName;
        System.out.println(">>> [SESSION OPENED]: " + sessionName);
    }

    @Override
    public void close() {
        System.out.println(">>> [SESSION CLOSED]: " + sessionName + " (Resources cleaned up safely)");
    }
}

public class Main {

    public static void main(String[] args) {

        System.out.println("=================================================");
        System.out.println("      MiniBank Fault-Tolerant Operations Demo    ");
        System.out.println("=================================================");

        // 1. Demonstration of try-with-resources with a custom AutoCloseable class
        try (BankSession session = new BankSession("Transaction Audit Session")) {

            Account[] accounts = new Account[3];
            accounts[0] = new SavingsAccount("Ravi Kumar", 5000, 1000);
            accounts[1] = new CurrentAccount("Priya Shah", 2000, 5000);
            accounts[2] = new FixedDepositAccount("Arjun Mehta", 10000);

            // 2. Deposit tests with try-catch
            System.out.println("\n--- Testing Deposits ---");
            try {
                accounts[0].deposit(1000);
                System.out.println("Successfully deposited Rs.1000 to Ravi. New balance: Rs." + accounts[0].getBalance());
            } catch (InvalidAmountException e) {
                System.out.println("Deposit Error: " + e.getMessage());
            }

            try {
                System.out.println("Attempting invalid deposit of -500...");
                accounts[0].deposit(-500);
            } catch (InvalidAmountException e) {
                System.out.println("Friendly Error Caught: " + e.getMessage());
            }

            // 3. Withdrawal tests with try-catch
            System.out.println("\n--- Testing Withdrawals ---");
            try {
                accounts[0].withdraw(2000);
                System.out.println("Successfully withdrew Rs.2000 from Ravi. New balance: Rs." + accounts[0].getBalance());
            } catch (InsufficientFundsException e) {
                System.out.println("Withdrawal Failed: " + e.getMessage() + " (Shortfall: Rs." + e.getShortfall() + ")");
            } catch (InvalidAmountException e) {
                System.out.println("Invalid Amount: " + e.getMessage());
            }

            try {
                accounts[1].withdraw(7000);
                System.out.println("Successfully withdrew Rs.7000 from Priya (using overdraft). New balance: Rs." + accounts[1].getBalance());
            } catch (InsufficientFundsException e) {
                System.out.println("Withdrawal Failed: " + e.getMessage() + " (Shortfall: Rs." + e.getShortfall() + ")");
            } catch (InvalidAmountException e) {
                System.out.println("Invalid Amount: " + e.getMessage());
            }

            try {
                System.out.println("Attempting withdrawal from Fixed Deposit account...");
                accounts[2].withdraw(1000);
            } catch (InsufficientFundsException e) {
                System.out.println("Friendly Error Caught: " + e.getMessage() + " (Shortfall: Rs." + e.getShortfall() + ")");
            } catch (InvalidAmountException e) {
                System.out.println("Invalid Amount: " + e.getMessage());
            }

            // 4. Transfer tests with try-catch
            System.out.println("\n--- Testing Transfers ---");
            try {
                System.out.println("Transferring Rs.1000 from Ravi to Priya...");
                accounts[0].transfer(accounts[1], 1000);
                System.out.println("Transfer completed! Ravi Balance: Rs." + accounts[0].getBalance() + ", Priya Balance: Rs." + accounts[1].getBalance());
            } catch (BankException e) {
                System.out.println("Transfer Error: " + e.getMessage());
            }

            try {
                System.out.println("Attempting transfer with insufficient balance (Rs.50000)...");
                accounts[0].transfer(accounts[1], 50000);
            } catch (InsufficientFundsException e) {
                System.out.println("Friendly Error Caught: " + e.getMessage() + " [Shortfall: Rs." + e.getShortfall() + "]");
            } catch (BankException e) {
                System.out.println("Transfer Failed: " + e.getMessage());
            }

            try {
                System.out.println("Attempting transfer to non-existent (null) account...");
                accounts[0].transfer(null, 500);
            } catch (AccountNotFoundException e) {
                System.out.println("Friendly Error Caught: " + e.getMessage());
            } catch (BankException e) {
                System.out.println("Transfer Failed: " + e.getMessage());
            }

            // 5. Account Summary
            System.out.println("\n===== Account Summary =====");
            for (Account acc : accounts) {
                System.out.println(acc.toString());
                System.out.println("Interest Rate : " + acc.interestRate() + "%");
                if (acc instanceof FixedDepositAccount) {
                    System.out.println("Note          : Fixed Deposit - withdrawals not allowed.");
                }
                System.out.println();
            }

            // =============================================================
            // MULTITHREADING LAB: Concurrency & Synchronization Demo
            // =============================================================
            System.out.println("\n=================================================");
            System.out.println("  Multithreading Demo: AccountWorker & Threads   ");
            System.out.println("=================================================");

            Account sharedAccount = new SavingsAccount("Concurrent User", 0, 0);
            int numThreads = 10;
            int timesPerThread = 1000;
            long depositPerTime = 10;
            long expectedFinal = (long) numThreads * timesPerThread * depositPerTime;

            System.out.println("Initial Shared Account Balance: Rs." + sharedAccount.getBalance());
            System.out.println("Creating " + numThreads + " threads (each depositing Rs." + depositPerTime + " x " + timesPerThread + " times)...");
            System.out.println("Expected Final Balance: Rs." + expectedFinal);

            Thread[] workers = new Thread[numThreads];
            for (int i = 0; i < numThreads; i++) {
                workers[i] = new Thread(new AccountWorker(sharedAccount, timesPerThread, depositPerTime), "WorkerThread-" + (i + 1));
                System.out.println("[LIFECYCLE] Created " + workers[i].getName() + " [State: " + workers[i].getState() + "]");
            }

            // Start all threads
            System.out.println("\n--- Starting all threads ---");
            for (int i = 0; i < numThreads; i++) {
                workers[i].start();
            }

            // Wait for all threads to finish using join()
            System.out.println("\n--- Waiting for threads to join ---");
            for (int i = 0; i < numThreads; i++) {
                try {
                    workers[i].join();
                    System.out.println("[LIFECYCLE] " + workers[i].getName() + " has TERMINATED [State: " + workers[i].getState() + "]");
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted: " + e.getMessage());
                }
            }

            System.out.println("\n--- Concurrency Results ---");
            System.out.println("Final Account Balance : Rs." + sharedAccount.getBalance());
            System.out.println("Expected Balance      : Rs." + expectedFinal);
            if (sharedAccount.getBalance() == expectedFinal) {
                System.out.println("RESULT: Correct! Synchronized methods prevented race conditions.");
            } else {
                System.out.println("RESULT: RACE CONDITION DETECTED! Balance mismatch.");
            }

            // =============================================================
            // TRANSACTION ENGINE LAB (Hour 2)
            // =============================================================

            // 1 & 2. TransactionProcessor with Thread Pool (ExecutorService)
            System.out.println("\n=================================================");
            System.out.println("   TransactionProcessor: Managed Thread Pool     ");
            System.out.println("=================================================");

            TransactionProcessor processor = new TransactionProcessor();
            Account poolAccount = new SavingsAccount("Pool User", 5000, 1000);

            System.out.println("Initial Pool Account Balance: Rs." + poolAccount.getBalance());
            System.out.println("Submitting batch of 6 deposit/withdrawal tasks to pool of 4 threads...");

            for (int i = 1; i <= 6; i++) {
                final int taskId = i;
                final long amt = i * 200;
                if (i % 2 == 1) {
                    processor.submit(() -> {
                        try {
                            poolAccount.deposit(amt);
                            System.out.println("[POOL TASK " + taskId + "] (" + Thread.currentThread().getName() + ") Deposited Rs." + amt + " -> Balance: Rs." + poolAccount.getBalance());
                        } catch (Exception e) {
                            System.out.println("[POOL TASK " + taskId + "] Error: " + e.getMessage());
                        }
                    });
                } else {
                    processor.submit(() -> {
                        try {
                            poolAccount.withdraw(amt);
                            System.out.println("[POOL TASK " + taskId + "] (" + Thread.currentThread().getName() + ") Withdrew Rs." + amt + " -> Balance: Rs." + poolAccount.getBalance());
                        } catch (Exception e) {
                            System.out.println("[POOL TASK " + taskId + "] Error: " + e.getMessage());
                        }
                    });
                }
            }

            System.out.println("Stopping processor and awaiting completion...");
            processor.stop();
            System.out.println("All thread pool tasks completed! Final Pool Account Balance: Rs." + poolAccount.getBalance());

            // 3. Producer-Consumer with wait() and notify()
            System.out.println("\n=================================================");
            System.out.println("  Producer-Consumer: wait() and notify() Buffer   ");
            System.out.println("=================================================");

            TransactionBuffer buffer = new TransactionBuffer(3);
            Account pcAccount = new SavingsAccount("PC User", 1000, 0);

            Thread producer = new Thread(() -> {
                for (int i = 1; i <= 5; i++) {
                    final long depositAmt = i * 100;
                    Runnable task = () -> {
                        try {
                            pcAccount.deposit(depositAmt);
                            System.out.println("  -> Executed buffered deposit of Rs." + depositAmt + ". Balance: Rs." + pcAccount.getBalance());
                        } catch (Exception e) {
                            System.out.println("  -> Error: " + e.getMessage());
                        }
                    };
                    try {
                        buffer.produce(task);
                        Thread.sleep(30); // Simulate time between incoming transactions
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "ProducerThread");

            Thread consumer = new Thread(() -> {
                for (int i = 1; i <= 5; i++) {
                    try {
                        Runnable task = buffer.consume();
                        task.run();
                        Thread.sleep(60); // Simulate processing time
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "ConsumerThread");

            producer.start();
            consumer.start();

            producer.join();
            consumer.join();
            System.out.println("Producer-Consumer coordination completed! Final Balance: Rs." + pcAccount.getBalance());

            // 4. Deadlock Avoidance with Consistent Lock Ordering
            System.out.println("\n=================================================");
            System.out.println("  Deadlock Prevention: Consistent Lock Ordering  ");
            System.out.println("=================================================");

            Account accountA = new SavingsAccount("Alice", 10000, 0);
            Account accountB = new SavingsAccount("Bob", 10000, 0);

            System.out.println("Account A: " + accountA.getAccountNumber() + " (Alice) Balance: Rs." + accountA.getBalance());
            System.out.println("Account B: " + accountB.getAccountNumber() + " (Bob) Balance: Rs." + accountB.getBalance());
            System.out.println("\nRunning 2 concurrent threads transferring A->B and B->A with safe lock ordering...");

            Thread transfer1 = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        Account.safeTransfer(accountA, accountB, 500);
                        Thread.sleep(20);
                    }
                } catch (Exception e) {
                    System.out.println("Transfer 1 Error: " + e.getMessage());
                }
            }, "TransferThread-AtoB");

            Thread transfer2 = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        Account.safeTransfer(accountB, accountA, 300);
                        Thread.sleep(20);
                    }
                } catch (Exception e) {
                    System.out.println("Transfer 2 Error: " + e.getMessage());
                }
            }, "TransferThread-BtoA");

            transfer1.start();
            transfer2.start();

            transfer1.join();
            transfer2.join();

            System.out.println("\nDeadlock successfully avoided!");
            System.out.println("Final Account A Balance: Rs." + accountA.getBalance());
            System.out.println("Final Account B Balance: Rs." + accountB.getBalance());

            // =============================================================
            // PERSISTENCE & REPORTING LAB: Serialization, Logs, & Reports
            // =============================================================
            System.out.println("\n=================================================");
            System.out.println("  Persistence & Reporting: Serialization & NIO   ");
            System.out.println("=================================================");

            // 1. Serialization with StatePersister
            Path accountsFile = Paths.get("data", "accounts.ser");
            System.out.println("Saving accounts state to: " + accountsFile);
            StatePersister.save(accounts, accountsFile);
            System.out.println("Accounts successfully saved.");

            System.out.println("\nReloading accounts from file using StatePersister.load()...");
            Account[] reloadedAccounts = StatePersister.load(accountsFile);
            System.out.println("--- Confirming Data Survived Serialization ---");
            for (Account acc : reloadedAccounts) {
                System.out.println("Reloaded -> " + acc.toString() + " | Active: " + acc.isActive());
            }

            // 2. Transaction Log with append
            Path log1 = Paths.get("logs", "transactions_morning.log");
            Path log2 = Paths.get("logs", "transactions_afternoon.log");

            System.out.println("\nAppending transaction logs using TransactionLog.append()...");
            TransactionLog.append(log1, "TXN001, AC0001, DEPOSIT, 5000");
            TransactionLog.append(log1, "TXN002, AC0002, WITHDRAW, 1500");
            TransactionLog.append(log1, "TXN003, AC0001, DEPOSIT, 2500");

            TransactionLog.append(log2, "TXN004, AC0003, DEPOSIT, 10000");
            TransactionLog.append(log2, "TXN005, AC0002, WITHDRAW, 3000");
            TransactionLog.append(log2, "TXN006, AC0001, WITHDRAW, 500");
            System.out.println("Transaction entries written to: " + log1 + " and " + log2);

            // 3. Generate End-of-Day Summary Report using NIO
            Path reportPath = Paths.get("reports", "end_of_day_report.txt");
            System.out.println("\nGenerating End-of-Day report using ReportGenerator...");
            ReportGenerator.generateReport(Paths.get("logs"), reportPath);
            System.out.println("Report written to: " + reportPath);

            System.out.println("\n--- Displaying Generated Report ---");
            for (String line : Files.readAllLines(reportPath)) {
                System.out.println(line);
            }

            // =============================================================
            // MILESTONE 2: Collections Framework & Storage Upgrade
            // =============================================================
            System.out.println("\n=================================================");
            System.out.println("   Milestone 2: Collections Framework Demo       ");
            System.out.println("=================================================");

            AccountStore store = new AccountStore();

            Account a1 = new SavingsAccount("Vikram Patel", 12000, 1000);
            Account a2 = new CurrentAccount("Ananya Sen", 45000, 10000);
            Account a3 = new FixedDepositAccount("Kavita Nair", 8000);
            Account a4 = new SavingsAccount("Dev Sharma", 25000, 500);
            Account a5 = new CurrentAccount("Meera Joshi", 1500, 2000);

            // 1. Add several accounts to AccountStore (HashMap)
            store.add(a1);
            store.add(a2);
            store.add(a3);
            store.add(a4);
            store.add(a5);

            System.out.println("Added 5 accounts to AccountStore (backed by HashMap<String, Account>).");
            System.out.println("Fetching specific account via store.get(\"" + a1.getAccountNumber() + "\"): " + store.get(a1.getAccountNumber()));

            // 2. Transaction history in ArrayList<String>
            System.out.println("\n--- AccountStore Transaction History (ArrayList<String>) ---");
            for (String entry : store.getTransactionHistory()) {
                System.out.println("  " + entry);
            }

            // 3. Print accounts sorted by balance (using Comparator)
            System.out.println("\n--- Accounts Sorted by Balance (Ascending via Comparator) ---");
            ArrayList<Account> sortedByBal = store.sortedByBalance();
            for (Account acc : sortedByBal) {
                System.out.println(String.format("  %-10s | Owner: %-15s | Balance: Rs.%d",
                        acc.getAccountNumber(), acc.getOwnerName(), acc.getBalance()));
            }

            // 4. Comparable<Account> demonstration (compareTo on accountNumber)
            System.out.println("\n--- Accounts Naturally Sorted by Account Number (Comparable<Account>) ---");
            ArrayList<Account> naturalOrderList = store.getAll();
            java.util.Collections.sort(naturalOrderList);
            for (Account acc : naturalOrderList) {
                System.out.println(String.format("  %-10s | Owner: %-15s | Balance: Rs.%d",
                        acc.getAccountNumber(), acc.getOwnerName(), acc.getBalance()));
            }

            // 5. ConcurrentHashMap in TransactionProcessor demo
            System.out.println("\n--- Thread-Safe Storage in TransactionProcessor (ConcurrentHashMap) ---");
            TransactionProcessor concurrentProcessor = new TransactionProcessor();
            for (Account acc : store.getAll()) {
                concurrentProcessor.addAccount(acc);
            }
            System.out.println("Registered " + concurrentProcessor.getSharedAccounts().size() + " accounts in ConcurrentHashMap for concurrent transactions.");
            concurrentProcessor.stop();

            // =============================================================
            // GENERIC REPOSITORY LAB: Reflection @Id & Bounded Generics
            // =============================================================
            System.out.println("\n=================================================");
            System.out.println("  Generic Repository<T> & @Id Reflection Demo    ");
            System.out.println("=================================================");

            Repository<Account> accountRepo = new Repository<>();

            Account r1 = new SavingsAccount("Siddharth Roy", 30000, 1000);
            Account r2 = new CurrentAccount("Neha Gupta", 60000, 15000);
            Account r3 = new FixedDepositAccount("Rajesh Verma", 150000);

            // 1. Save individual account using save() with @Id reflection
            accountRepo.save(r1);
            System.out.println("Saved into Repository via save(): " + r1.getAccountNumber() + " (" + r1.getOwnerName() + ")");

            // 2. Save multiple accounts using @SafeVarargs saveAll()
            accountRepo.saveAll(r2, r3);
            System.out.println("Saved multiple accounts into Repository via @SafeVarargs saveAll().");

            // 3. Fetch one by id
            String targetId = r2.getAccountNumber();
            Account fetched = accountRepo.findById(targetId);
            System.out.println("\nFetched by ID (" + targetId + "): " + fetched);

            // 4. Print all accounts from repository
            System.out.println("\n--- All Accounts in Repository<Account> (findAll()) ---");
            for (Account acc : accountRepo.findAll()) {
                System.out.println("  " + acc);
            }

            // 5. Test generic method with bounded type parameter <N extends Number>
            System.out.println("\n--- Bounded Generic Method: Repository.<N extends Number> average(...) ---");
            Long[] balances = new Long[] { r1.getBalance(), r2.getBalance(), r3.getBalance() };
            double avgBalance = Repository.average(balances);
            System.out.println("Balances: [Rs." + balances[0] + ", Rs." + balances[1] + ", Rs." + balances[2] + "]");
            System.out.println("Calculated Average Balance: Rs." + String.format("%.2f", avgBalance));

            Double[] interestRates = new Double[] { r1.interestRate(), r2.interestRate(), r3.interestRate() };
            double avgRate = Repository.average(interestRates);
            System.out.println("Interest Rates: [" + interestRates[0] + "%, " + interestRates[1] + "%, " + interestRates[2] + "%]");
            System.out.println("Calculated Average Interest Rate: " + String.format("%.2f", avgRate) + "%");

        } catch (Exception e) {
            System.out.println("General Application Error: " + e.getMessage());
        } finally {
            // Demonstration of finally block
            System.out.println("\n=== [FINALLY BLOCK]: All banking operations finished. Audit trail written. ===");
        }
    }
}
