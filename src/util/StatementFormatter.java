package util;

import model.Account;

public final class StatementFormatter {
    private StatementFormatter() {
    }

    public static String buildStatement(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        StringBuilder statement = new StringBuilder();
        statement.append("Account Statement\n");
        statement.append("Account Number: ").append(account.getAccountNumber()).append('\n');
        statement.append("Owner: ").append(account.getOwnerName()).append('\n');
        statement.append("Balance: Rs. ").append(account.getBalance()).append('\n');
        statement.append("Status: ").append(account.isActive() ? "Active" : "Inactive");
        return statement.toString();
    }
}
