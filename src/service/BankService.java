package service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InvalidAmountException;
import model.Account;
import model.Command;
import model.Customer;
import model.SavingsAccount;
import util.ReportGenerator;
import util.Repository;
import util.StatePersister;
import util.StatementFormatter;
import util.TransactionLog;
import util.Validator;

public final class BankService {
    private final Repository<Account> accounts = new Repository<>();
    private final Path stateFile;
    private final Path logsFolder;

    public BankService(Path stateFile, Path logsFolder, Account[] defaults)
            throws IOException, ClassNotFoundException {
        this.stateFile = stateFile;
        this.logsFolder = logsFolder;
        Account[] saved = java.nio.file.Files.exists(stateFile)
                ? StatePersister.load(stateFile)
                : defaults;
        accounts.saveAll(saved);
    }

    public Account openAccount(String ownerName, String email, String mobile, long openingBalance)
            throws InvalidAmountException {
        if (ownerName == null || ownerName.isBlank()
                || !Validator.isValidEmail(email) || !Validator.isValidMobile(mobile)) {
            throw new IllegalArgumentException("Invalid owner name, email, or mobile.");
        }
        if (openingBalance < 0) {
            throw new InvalidAmountException(openingBalance);
        }
        Account account = new SavingsAccount(ownerName, openingBalance, 0);
        accounts.save(account);
        return account;
    }

    public void deposit(String accountNumber, long amount) throws BankException, IOException {
        find(accountNumber).deposit(amount);
        log("DEPOSIT " + amount + " " + accountNumber);
    }

    public void withdraw(String accountNumber, long amount) throws BankException, IOException {
        find(accountNumber).withdraw(amount);
        log("WITHDRAW " + amount + " " + accountNumber);
    }

    public void transfer(String from, String to, long amount) throws BankException, IOException {
        Account source = find(from);
        Account destination = find(to);
        source.transfer(destination, amount);
        log("TRANSFER " + amount + " " + from + " " + to);
    }

    public String statement(String accountNumber) throws AccountNotFoundException {
        return StatementFormatter.buildStatement(find(accountNumber));
    }

    public ArrayList<Account> listAccounts() {
        return accounts.findAll();
    }

    public ArrayList<Account> searchByOwner(String ownerName) {
        ArrayList<Account> matches = new ArrayList<>();
        for (Account account : accounts.findAll()) {
            if (account.getOwnerName().equalsIgnoreCase(ownerName)) {
                matches.add(account);
            }
        }
        return matches;
    }

    public String checkBalance(String identifier) throws AccountNotFoundException {
        Account account = accounts.findById(identifier);
        if (account != null) {
            return "Account " + account.getAccountNumber()
                    + " balance: Rs. " + account.getBalance();
        }

        ArrayList<Account> matches = searchByOwner(identifier);
        if (matches.isEmpty()) {
            throw new AccountNotFoundException(identifier);
        }

        long totalBalance = 0;
        StringBuilder result = new StringBuilder();
        result.append("Balances for ").append(identifier).append(":\n");
        for (Account ownerAccount : matches) {
            totalBalance += ownerAccount.getBalance();
            result.append(ownerAccount.getAccountNumber())
                    .append(": Rs. ").append(ownerAccount.getBalance()).append('\n');
        }
        result.append("Total balance: Rs. ").append(totalBalance);
        return result.toString();
    }

    public void processBatch(List<Command> commands) throws InterruptedException {
        TransactionProcessor processor = new TransactionProcessor();
        for (Account account : accounts.findAll()) {
            processor.addAccount(account);
        }
        for (Command command : commands) {
            processor.submit(() -> {
                try {
                    if (command.type().name().equals("DEPOSIT")) {
                        find(command.accountNumber()).deposit(command.amount());
                    } else {
                        find(command.accountNumber()).withdraw(command.amount());
                    }
                } catch (BankException exception) {
                    System.out.println("Batch transaction failed: " + exception.getMessage());
                }
            });
        }
        processor.stop();
    }

    public void save() throws IOException {
        StatePersister.save(accounts.findAll().toArray(Account[]::new), stateFile);
    }

    public void writeReport(Path reportFile) throws IOException {
        ReportGenerator.generate(logsFolder, reportFile);
    }

    private Account find(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.findById(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    private void log(String line) throws IOException {
        TransactionLog.append(logsFolder.resolve("transactions.log"), line);
    }
}
