package model;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import java.io.Serializable;
import model.annotation.Id;
import model.annotation.Positive;

public abstract class Account implements Transactable, InterestBearing, Serializable, Comparable<Account> {

    private static final long serialVersionUID = 1L;

    @Id
    private final String accountNumber;
    private String ownerName;
    @Positive
    private long balance;
    private boolean active;
    private static int accountCounter = 0;

    private static String generateAccountNumber() {
        accountCounter++;
        return "AC" + String.format("%04d", accountCounter);
    }

    public Account(String ownerName, long openingBalance) {
        this.accountNumber = generateAccountNumber();
        this.ownerName = ownerName;
        this.balance = openingBalance;
        this.active = true;
    }

    public Account(String ownerName) {
        this(ownerName, 0);
    }

    public abstract double interestRate();

    public abstract boolean canWithdraw(long amount);

    @Override
    public synchronized void deposit(long amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero. Received: " + amount);
        }
        balance = balance + amount;
    }

    @Override
    public synchronized void withdraw(long amount) throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero. Received: " + amount);
        }
        if (!canWithdraw(amount)) {
            long shortfall = amount - balance;
            if (shortfall <= 0) {
                shortfall = amount;
            }
            throw new InsufficientFundsException("Insufficient balance to withdraw " + amount + ". Shortfall: " + shortfall, shortfall);
        }
        balance = balance - amount;
    }

    public void transfer(Account to, long amount) throws BankException {
        safeTransfer(this, to, amount);
    }

    // Method illustrating the deadlock hazard: locks 'from' then 'to' (opposite order)
    public static void deadlockTransfer(Account from, Account to, long amount) throws BankException {
        if (from == null || to == null) {
            throw new AccountNotFoundException("Accounts cannot be null.");
        }
        // Inverted lock acquisition order causes circular wait (deadlock)
        synchronized (from) {
            System.out.println(Thread.currentThread().getName() + " locked " + from.getAccountNumber() + ", waiting for " + to.getAccountNumber());
            try {
                Thread.sleep(50); // Pause so the other thread locks its 'from' account
            } catch (InterruptedException ignored) {}

            synchronized (to) {
                from.withdraw(amount);
                to.deposit(amount);
                System.out.println(Thread.currentThread().getName() + " transferred Rs." + amount + " from " + from.getAccountNumber() + " to " + to.getAccountNumber());
            }
        }
    }

    // Fix for deadlock: Always locks the account with the smaller accountNumber first
    public static void safeTransfer(Account from, Account to, long amount) throws BankException {
        if (from == null || to == null) {
            throw new AccountNotFoundException("Accounts cannot be null.");
        }
        if (from.equals(to)) {
            throw new BankException("Cannot transfer to the same account.");
        }

        // Consistent Lock Ordering: compare account numbers to always lock in the same sequence
        boolean fromIsSmaller = from.getAccountNumber().compareTo(to.getAccountNumber()) < 0;
        Account firstLock = fromIsSmaller ? from : to;
        Account secondLock = fromIsSmaller ? to : from;

        synchronized (firstLock) {
            synchronized (secondLock) {
                from.withdraw(amount);
                to.deposit(amount);
            }
        }
        System.out.println(Thread.currentThread().getName() + " [SAFE TRANSFER] Rs." + amount + " from " + from.getAccountNumber() + " to " + to.getAccountNumber() + " completed.");
    }

    @Override
    public String toString() {
        return "Account[" + accountNumber + "] Owner: " + ownerName + " Balance: Rs." + balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account a = (Account) o;
        return accountNumber.equals(a.accountNumber);
    }

    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public int compareTo(Account other) {
        if (other == null) return 1;
        return this.accountNumber.compareTo(other.accountNumber);
    }
}
