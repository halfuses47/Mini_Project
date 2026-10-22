package test;

import util.AccountWorker;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.SavingsAccount;

import java.lang.reflect.Method;

public class MiniBankTest {

    @Test
    public void testDepositIncreasesBalance() throws InvalidAmountException {
        Account account = new SavingsAccount("Test User", 5000, 1000);
        long initialBalance = account.getBalance();
        long depositAmount = 2000;

        account.deposit(depositAmount);

        if (account.getBalance() != initialBalance + depositAmount) {
            throw new AssertionError("Deposit failed! Expected: " + (initialBalance + depositAmount) + ", Got: " + account.getBalance());
        }
    }

    @Test
    public void testWithdrawalOverLimitThrowsException() throws InvalidAmountException {
        Account account = new SavingsAccount("Test User", 5000, 1000);
        boolean exceptionThrown = false;

        try {
            // Balance is 5000, minBalance is 1000 -> max allowed withdrawal is 4000.
            // Withdrawing 4500 should throw InsufficientFundsException
            account.withdraw(4500);
        } catch (InsufficientFundsException e) {
            exceptionThrown = true;
            if (e.getShortfall() <= 0) {
                throw new AssertionError("Shortfall should be greater than 0, but was: " + e.getShortfall());
            }
        }

        if (!exceptionThrown) {
            throw new AssertionError("Expected InsufficientFundsException was not thrown!");
        }
    }

    @Test
    public void testConcurrentDepositsKeepBalanceCorrect() throws InterruptedException {
        Account sharedAccount = new SavingsAccount("Concurrent Test", 0, 0);
        int numThreads = 10;
        int times = 1000;
        long amount = 10;
        long expectedBalance = (long) numThreads * times * amount;

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new AccountWorker(sharedAccount, times, amount));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        if (sharedAccount.getBalance() != expectedBalance) {
            throw new AssertionError("Race condition detected! Expected balance: " + expectedBalance + ", Got: " + sharedAccount.getBalance());
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("            RUNNING MINIBANK TEST SUITE          ");
        System.out.println("=================================================");

        MiniBankTest suite = new MiniBankTest();
        int total = 0;
        int passed = 0;
        int failed = 0;

        for (Method method : MiniBankTest.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Test.class)) {
                total++;
                System.out.print("Running test: " + method.getName() + " ... ");
                try {
                    method.invoke(suite);
                    System.out.println("[PASS]");
                    passed++;
                } catch (Exception e) {
                    System.out.println("[FAIL]");
                    System.out.println("  Cause: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                    failed++;
                }
            }
        }

        System.out.println("-------------------------------------------------");
        System.out.println("Results: " + total + " run, " + passed + " passed, " + failed + " failed.");
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
