// 25CE079
package service;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;
import model.annotation.Audit;
import util.AnnotationValidator;
import util.Repository;

import java.util.ArrayList;

public class BankService {

    private final Repository<Account> repository;

    public BankService(Repository<Account> repository) {
        this.repository = repository;
    }

    public Repository<Account> getRepository() {
        return repository;
    }

    @Audit("OPEN_ACCOUNT")
    public Account openAccount(String type, String ownerName, long initialBalance, long extraParam) throws BankException {
        AuditLogger.logCall(BankService.class, "openAccount");

        Account account;
        String t = type.trim().toUpperCase();
        if (t.contains("SAVING")) {
            account = new SavingsAccount(ownerName, initialBalance, extraParam);
        } else if (t.contains("CURRENT")) {
            account = new CurrentAccount(ownerName, initialBalance, extraParam);
        } else if (t.contains("FIXED") || t.contains("FD")) {
            account = new FixedDepositAccount(ownerName, initialBalance);
        } else {
            throw new BankException("Unsupported account type: " + type + ". Available: Savings, Current, Fixed Deposit.");
        }

        // Validate using AnnotationValidator (@Positive, @Id, etc.)
        String[] errors = AnnotationValidator.validate(account);
        if (errors.length > 0) {
            throw new BankException("Validation failed for new account: " + String.join(", ", errors));
        }

        repository.save(account);
        return account;
    }

    @Audit("DEPOSIT")
    public void deposit(String accountNumber, long amount) throws BankException {
        AuditLogger.logCall(BankService.class, "deposit");
        Account acc = repository.findById(accountNumber);
        if (acc == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
        acc.deposit(amount);
    }

    @Audit("WITHDRAW")
    public void withdraw(String accountNumber, long amount) throws BankException {
        AuditLogger.logCall(BankService.class, "withdraw");
        Account acc = repository.findById(accountNumber);
        if (acc == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
        acc.withdraw(amount);
    }

    @Audit("TRANSFER")
    public void transfer(String fromAccNum, String toAccNum, long amount) throws BankException {
        AuditLogger.logCall(BankService.class, "transfer");
        Account from = repository.findById(fromAccNum);
        Account to = repository.findById(toAccNum);
        if (from == null) {
            throw new AccountNotFoundException("Source account not found: " + fromAccNum);
        }
        if (to == null) {
            throw new AccountNotFoundException("Destination account not found: " + toAccNum);
        }
        Account.safeTransfer(from, to, amount);
    }

    @Audit("STATEMENT")
    public String statement(String accountNumber) throws BankException {
        AuditLogger.logCall(BankService.class, "statement");
        Account acc = repository.findById(accountNumber);
        if (acc == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("           ACCOUNT STATEMENT             \n");
        sb.append("=========================================\n");
        sb.append("Account Number : ").append(acc.getAccountNumber()).append("\n");
        sb.append("Owner Name     : ").append(acc.getOwnerName()).append("\n");
        sb.append("Current Balance: Rs.").append(acc.getBalance()).append("\n");
        sb.append("Interest Rate  : ").append(acc.interestRate()).append("%\n");
        sb.append("Account Type   : ").append(acc.getClass().getSimpleName()).append("\n");
        sb.append("Status         : ").append(acc.isActive() ? "Active" : "Inactive").append("\n");
        sb.append("=========================================");
        return sb.toString();
    }

    @Audit("LIST_ACCOUNTS")
    public ArrayList<Account> listAccounts() {
        AuditLogger.logCall(BankService.class, "listAccounts");
        return repository.findAll();
    }
}
