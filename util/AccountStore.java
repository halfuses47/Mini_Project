package util;

import model.Account;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class AccountStore {

    private final HashMap<String, Account> accounts = new HashMap<>();
    private final ArrayList<String> transactionHistory = new ArrayList<>();

    public void add(Account account) {
        if (account != null) {
            accounts.put(account.getAccountNumber(), account);
            transactionHistory.add("STORE_ADD: Registered account " + account.getAccountNumber() + " for " + account.getOwnerName() + " (Opening Balance: Rs." + account.getBalance() + ")");
        }
    }

    public Account get(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public ArrayList<Account> getAll() {
        return new ArrayList<>(accounts.values());
    }

    public void logTransaction(String entry) {
        transactionHistory.add(entry);
    }

    public ArrayList<String> getTransactionHistory() {
        return transactionHistory;
    }

    public ArrayList<Account> sortedByBalance() {
        ArrayList<Account> sortedList = new ArrayList<>(accounts.values());
        sortedList.sort(Comparator.comparingLong(Account::getBalance));
        return sortedList;
    }
}
