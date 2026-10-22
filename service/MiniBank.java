// 25CE079
package service;

import util.Repository;
import util.StatePersister;
import util.ReportGenerator;
import util.TransactionProcessor;
import util.TransactionLog;
import exception.BankException;
import exception.InsufficientFundsException;
import model.Account;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class MiniBank {

    record BankInfo(String name, String branch) {}

    public static void main(String[] args) {
        BankInfo bank = new BankInfo("MiniBank", "Main Branch");
        System.out.println("=================================================");
        System.out.println("     Welcome to " + bank.name() + " - " + bank.branch());
        System.out.println("=================================================");

        Repository<Account> repository = new Repository<>();
        BankService bankService = new BankService(repository);

        // Load saved accounts on startup (Practical 11)
        Path storagePath = Paths.get("data", "accounts.ser");
        if (Files.exists(storagePath)) {
            try {
                Account[] loaded = StatePersister.load(storagePath);
                for (Account acc : loaded) {
                    repository.save(acc);
                }
                System.out.println("[STORAGE] Loaded " + loaded.length + " accounts from persistent storage.");
            } catch (Exception e) {
                System.out.println("[STORAGE] Could not load saved accounts: " + e.getMessage());
            }
        } else {
            System.out.println("[STORAGE] No existing account storage file found. Initialized with fresh repository.");
        }

        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        while (choice != 9) {
            System.out.println("\n-------------------------------------------------");
            System.out.println("              MiniBank Main Menu                 ");
            System.out.println("-------------------------------------------------");
            System.out.println("1. Open New Account");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Transfer Funds");
            System.out.println("5. View Account Statement");
            System.out.println("6. List All Accounts");
            System.out.println("7. Safe Batch Transaction Processing (Pool of 4)");
            System.out.println("8. Generate End-of-Day Summary Report");
            System.out.println("9. Exit Application");
            System.out.print("Enter choice (1-9): ");

            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid numeric choice.");
                scanner.nextLine();
                continue;
            }

            choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1 -> {
                    System.out.print("Account Type (Savings / Current / Fixed Deposit): ");
                    String type = scanner.nextLine();
                    System.out.print("Account Holder Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Opening Balance (Rs.): ");
                    long balance = scanner.nextLong();
                    long extra = 0;
                    if (type.toUpperCase().contains("SAVING")) {
                        System.out.print("Minimum Balance Requirement (Rs.): ");
                        extra = scanner.nextLong();
                    } else if (type.toUpperCase().contains("CURRENT")) {
                        System.out.print("Overdraft Limit (Rs.): ");
                        extra = scanner.nextLong();
                    }
                    scanner.nextLine(); // consume newline
                    try {
                        Account acc = bankService.openAccount(type, name, balance, extra);
                        System.out.println("SUCCESS! Account created: " + acc);
                    } catch (BankException e) {
                        System.out.println("FAILED: " + e.getMessage());
                    }
                }
                case 2 -> {
                    System.out.print("Enter Account Number (e.g. AC0001): ");
                    String accNum = scanner.nextLine().trim();
                    System.out.print("Enter Deposit Amount (Rs.): ");
                    long amount = scanner.nextLong();
                    scanner.nextLine();
                    try {
                        bankService.deposit(accNum, amount);
                        Account acc = repository.findById(accNum);
                        System.out.println("SUCCESS: Deposited Rs." + amount + ". New Balance: Rs." + acc.getBalance());
                        TransactionLog.append(Paths.get("logs", "transactions.log"), "DEPOSIT, " + accNum + ", " + amount);
                    } catch (Exception e) {
                        System.out.println("FAILED: " + e.getMessage());
                    }
                }
                case 3 -> {
                    System.out.print("Enter Account Number (e.g. AC0001): ");
                    String accNum = scanner.nextLine().trim();
                    System.out.print("Enter Withdrawal Amount (Rs.): ");
                    long amount = scanner.nextLong();
                    scanner.nextLine();
                    try {
                        bankService.withdraw(accNum, amount);
                        Account acc = repository.findById(accNum);
                        System.out.println("SUCCESS: Withdrew Rs." + amount + ". New Balance: Rs." + acc.getBalance());
                        TransactionLog.append(Paths.get("logs", "transactions.log"), "WITHDRAW, " + accNum + ", " + amount);
                    } catch (InsufficientFundsException e) {
                        System.out.println("FAILED: " + e.getMessage() + " [Shortfall: Rs." + e.getShortfall() + "]");
                    } catch (BankException e) {
                        System.out.println("FAILED: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                }
                case 4 -> {
                    System.out.print("Enter Source Account Number: ");
                    String from = scanner.nextLine().trim();
                    System.out.print("Enter Destination Account Number: ");
                    String to = scanner.nextLine().trim();
                    System.out.print("Enter Transfer Amount (Rs.): ");
                    long amount = scanner.nextLong();
                    scanner.nextLine();
                    try {
                        bankService.transfer(from, to, amount);
                        System.out.println("SUCCESS: Transferred Rs." + amount + " from " + from + " to " + to);
                        TransactionLog.append(Paths.get("logs", "transactions.log"), "WITHDRAW, " + from + ", " + amount);
                        TransactionLog.append(Paths.get("logs", "transactions.log"), "DEPOSIT, " + to + ", " + amount);
                    } catch (BankException e) {
                        System.out.println("TRANSFER FAILED: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                }
                case 5 -> {
                    System.out.print("Enter Account Number: ");
                    String accNum = scanner.nextLine().trim();
                    try {
                        System.out.println(bankService.statement(accNum));
                    } catch (BankException e) {
                        System.out.println("FAILED: " + e.getMessage());
                    }
                }
                case 6 -> {
                    ArrayList<Account> all = bankService.listAccounts();
                    if (all.isEmpty()) {
                        System.out.println("No accounts currently registered in MiniBank.");
                    } else {
                        System.out.println("=========================================");
                        System.out.println("           CURRENT ALL ACCOUNTS          ");
                        System.out.println("=========================================");
                        for (Account acc : all) {
                            System.out.println(acc + " | Rate: " + acc.interestRate() + "%");
                        }
                    }
                }
                case 7 -> {
                    System.out.println("Executing safe concurrent batch processing using TransactionProcessor (ThreadPool: 4)...");
                    TransactionProcessor processor = new TransactionProcessor();
                    ArrayList<Account> accounts = repository.findAll();
                    if (accounts.isEmpty()) {
                        System.out.println("No accounts available for batch processing. Open an account first.");
                    } else {
                        Account target = accounts.get(0);
                        System.out.println("Submitting 5 concurrent transactions against " + target.getAccountNumber() + " (Current Balance: Rs." + target.getBalance() + ")...");
                        for (int i = 1; i <= 5; i++) {
                            final int idx = i;
                            final long amt = i * 100;
                            processor.submit(() -> {
                                try {
                                    target.deposit(amt);
                                    System.out.println("  -> [BATCH WORKER " + Thread.currentThread().getName() + "] Deposited Rs." + amt + " into " + target.getAccountNumber());
                                } catch (Exception e) {
                                    System.out.println("  -> Error: " + e.getMessage());
                                }
                            });
                        }
                        processor.stop();
                        System.out.println("Batch processing completed! Updated Balance: Rs." + target.getBalance());
                    }
                }
                case 8 -> {
                    System.out.println("Generating End-of-Day report from logs directory...");
                    Path reportFile = Paths.get("reports", "end_of_day_report.txt");
                    try {
                        ReportGenerator.generateReport(Paths.get("logs"), reportFile);
                        System.out.println("\n--- REPORT OUTPUT ---");
                        for (String line : Files.readAllLines(reportFile)) {
                            System.out.println(line);
                        }
                    } catch (Exception e) {
                        System.out.println("Error generating report: " + e.getMessage());
                    }
                }
                case 9 -> {
                    System.out.println("\nExiting MiniBank...");
                    // Save accounts on exit (Practical 11)
                    try {
                        ArrayList<Account> allAccounts = repository.findAll();
                        StatePersister.save(allAccounts.toArray(new Account[0]), storagePath);
                        System.out.println("[STORAGE] Successfully saved " + allAccounts.size() + " accounts to " + storagePath);
                    } catch (Exception e) {
                        System.out.println("[STORAGE] Error saving accounts: " + e.getMessage());
                    }
                    System.out.println("Thank you for banking with MiniBank. Goodbye!");
                }
                default -> System.out.println("Invalid selection. Please choose an option from 1 to 9.");
            }
        }

        scanner.close();
    }
}
