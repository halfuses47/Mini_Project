// 25CE079
import java.util.Scanner;

public class MiniBank {

    record BankInfo(String name, String branch) {}

    enum MenuOption {
        OPEN_ACCOUNT, DEPOSIT, WITHDRAW, TRANSFER, EXIT
    }

    public static void main(String[] args) {

        BankInfo bank = new BankInfo("MiniBank", "Main Branch");
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to " + bank.name() + " - " + bank.branch());

        int choice;

        do {
            System.out.println();
            System.out.println("1. Open Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

            choice = scanner.nextInt();

            String message = switch (choice) {
                case 1 -> "Open Account - to be implemented in a later lab";
                case 2 -> "Deposit - to be implemented in a later lab";
                case 3 -> "Withdraw - to be implemented in a later lab";
                case 4 -> "Transfer - to be implemented in a later lab";
                case 5 -> "Goodbye!";
                default -> "Invalid choice. Please try again.";
            };

            System.out.println(message);

        } while (choice != 5);

        scanner.close();
    }
}
