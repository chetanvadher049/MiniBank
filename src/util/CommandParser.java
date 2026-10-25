package util;

import model.Command;
import model.TransactionType;

public final class CommandParser {
    private CommandParser() {
    }

    public static Command parse(String line) {
        if (line == null) {
            throw new IllegalArgumentException("Command cannot be null.");
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Command must contain exactly three parts: TYPE ACCOUNT_NUMBER AMOUNT.");
        }

        TransactionType type;
        try {
            type = TransactionType.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown transaction type: " + parts[0], exception);
        }

        if (!Validator.isValidAmount(parts[2])) {
            throw new IllegalArgumentException("Amount must be a positive whole number.");
        }

        try {
            return new Command(type, parts[1], Long.parseLong(parts[2]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Amount is outside the supported range.", exception);
        }
    }
}
