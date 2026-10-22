// 25CE079
package util;

import model.Command;
import model.TransactionType;

public class CommandParser {

    public static Command parse(String line) {
        String[] parts = line.split(" ");
        TransactionType type = TransactionType.valueOf(parts[0]);
        String accountNumber = parts[1];
        long amount = Long.parseLong(parts[2]);
        return new Command(type, accountNumber, amount);
    }
}
