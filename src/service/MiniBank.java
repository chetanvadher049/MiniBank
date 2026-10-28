package service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

import exception.BankException;
import exception.InsufficientFundsException;
import model.Account;
import model.AccountType;
import model.BankInfo;
import model.Command;
import model.CurrentAccount;
import model.Customer;
import model.FixedDepositAccount;
import model.MenuOption;
import model.SavingsAccount;
import model.WithdrawRule;
import model.TransactionType;
import util.CommandParser;
import util.AnnotationValidator;
import util.ReportGenerator;
import util.StatePersister;
import util.TransactionLog;
import util.Repository;
import exception.InvalidAmountException;
import static util.Validator.*;

public class MiniBank {
    private static final BankInfo BANK_INFO = new BankInfo("MiniBank", "Main Branch");

    public static void main(String[] args) {
        try {
        Account[] accounts = createSampleAccounts();
        BankService bankService = new BankService(
                Path.of("data", "accounts.ser"),
                Path.of("data", "logs"),
                accounts);
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to MiniBank!");

            boolean running = true;
            while (running) {
                printMenu();
                int choice = readMenuChoice(scanner);

                MenuOption selectedOption = switch (choice) {
                    case 1 -> MenuOption.OPEN_ACCOUNT;
                    case 2 -> MenuOption.DEPOSIT;
                    case 3 -> MenuOption.WITHDRAW;
                    case 4 -> MenuOption.TRANSFER;
                    case 5 -> MenuOption.WORKING_HOURS;
                    case 6 -> MenuOption.CHECK_BALANCE;
                    case 7 -> MenuOption.LIST_ACCOUNTS;
                    case 8 -> MenuOption.SEARCH_ACCOUNT;
                    case 9 -> MenuOption.BATCH_PROCESS;
                    case 10 -> MenuOption.REPORT;
                    case 11 -> MenuOption.EXIT;
                    default -> null;
                };

                if (selectedOption == null) {
                    System.out.println("Invalid menu number. Please choose a number from 1 to 11.");
                    continue;
                }

                handleMenuOption(selectedOption, scanner, bankService);
                running = selectedOption != MenuOption.EXIT;
            }
            bankService.save();
        }
        } catch (BankException | IOException | ClassNotFoundException exception) {
            System.out.println("Banking operation failed: " + exception.getMessage());
            if (exception instanceof InsufficientFundsException insufficientFunds) {
                System.out.println("Shortfall: Rs. " + insufficientFunds.getShortfall());
            }
        } finally {
            System.out.println("Banking operation processing finished.");
        }
    }

    private static void printValidationExamples() {
        System.out.println("Validation examples:");
        System.out.println("Mobile 9876543210: " + isValidMobile("9876543210"));
        System.out.println("Mobile 12345: " + isValidMobile("12345"));
        System.out.println("Email user@example.com: " + isValidEmail("user@example.com"));
        System.out.println("Email user@invalid: " + isValidEmail("user@invalid"));
        System.out.println("PAN ABCDE1234F: " + isValidPan("ABCDE1234F"));
        System.out.println("PAN ABC123: " + isValidPan("ABC123"));
        System.out.println("IFSC SBIN0001234: " + isValidIfsc("SBIN0001234"));
        System.out.println("IFSC BANK123: " + isValidIfsc("BANK123"));
        System.out.println("Amount 500: " + isValidAmount("500"));
        System.out.println("Amount -10: " + isValidAmount("-10"));
    }

    private static void demonstrateRepositories(Account[] accounts) {
        Repository<Account> accountRepository = new Repository<>();
        accountRepository.saveAll(accounts);
        Account found = accountRepository.findById(accounts[0].getAccountNumber());
        System.out.println("Repository account lookup: " + found);
        System.out.println("Repository accounts:");
        for (Account account : accountRepository.findAll()) {
            System.out.println(account);
        }

        Customer firstCustomer = new Customer("Anita Sharma", "anita@example.com", "9876543210");
        Customer secondCustomer = new Customer("Rahul Verma", "rahul@example.com", "9876543211");
        Repository<Customer> customerRepository = new Repository<>();
        customerRepository.saveAll(firstCustomer, secondCustomer);
        System.out.println("Repository customer count: " + customerRepository.size());
        System.out.println("Average sample balance: "
                + Repository.average(new Long[] {1_000L, 2_000L, 3_000L}));
    }

    private static void demonstrateCollections(Account[] accounts) {
        AccountStore store = new AccountStore();
        for (Account account : accounts) {
            store.add(account);
        }
        store.recordTransaction("DEPOSIT 500");
        store.recordTransaction("WITHDRAW 200");

        System.out.println("Accounts sorted by balance:");
        for (Account account : store.sortedByBalance()) {
            System.out.println(account);
        }
        System.out.println("Accounts above Rs. 5000: " + store.withBalanceAbove(5_000).size());
        System.out.println("Transaction history entries: " + store.getTransactionHistory().size());

        TransactionProcessor processor = new TransactionProcessor();
        for (Account account : accounts) {
            processor.addAccount(account);
        }
        System.out.println("Thread-safe shared accounts: " + processor.accountCount());
        try {
            processor.stop();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while stopping collection processor.");
        }
    }

    private static void printCommandExample() {
        Command command = CommandParser.parse("DEPOSIT AC0001 500");
        System.out.println("Parsed command:");
        System.out.println("Type: " + command.type());
        System.out.println("Account number: " + command.accountNumber());
        System.out.println("Amount: " + command.amount());
    }

    private static void printAnnotationValidation() {
        Account invalidAccount = new CurrentAccount("Valid Owner", -100, 500);
        System.out.println("Annotation validation for negative balance:");
        for (String error : AnnotationValidator.validate(invalidAccount)) {
            System.out.println(error);
        }

        Account longNameAccount = new CurrentAccount(
                "This owner name is intentionally longer than fifty characters", 100, 500);
        System.out.println("Annotation validation for long owner name:");
        for (String error : AnnotationValidator.validate(longNameAccount)) {
            System.out.println(error);
        }
    }

    private static Account[] createSampleAccounts() throws BankException {
        Account[] accounts = {
            new SavingsAccount("Anita Sharma", 5_000, 1_000),
            new CurrentAccount("Rahul Verma", 0, 5_000),
            new FixedDepositAccount("Meera Patel", 10_000)
        };

        accounts[0].deposit(2_000);
        accounts[0].withdraw(1_000);
        accounts[1].deposit(7_500);
        accounts[1].withdraw(500);
        accounts[1].transfer(accounts[0], 2_500);

        return accounts;
    }

    private static void demonstrateExceptionHandling(Account[] accounts) throws BankException {
        try {
            accounts[0].withdraw(20_000);
        } catch (InsufficientFundsException exception) {
            System.out.println("Withdrawal declined: " + exception.getMessage());
            System.out.println("Recorded shortfall: Rs. " + exception.getShortfall());
        }

        try {
            accounts[0].deposit(0);
        } catch (BankException exception) {
            System.out.println("Deposit declined: " + exception.getMessage());
        }
    }

    private static void demonstrateConcurrency() {
        Account sharedAccount = new CurrentAccount("Threaded Customer", 0, 0);
        int workerCount = 10;
        int depositsPerWorker = 1_000;
        long amountPerDeposit = 10;
        Thread[] workers = new Thread[workerCount];

        System.out.println("Starting synchronized account workers:");
        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Thread(
                    new AccountWorker(sharedAccount, depositsPerWorker, amountPerDeposit),
                    "AccountWorker-" + (i + 1));
            workers[i].start();
        }

        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                System.out.println("Main thread interrupted while waiting for workers.");
                return;
            }
        }

        long expectedBalance = workerCount * depositsPerWorker * amountPerDeposit;
        System.out.println("Expected final balance: Rs. " + expectedBalance);
        System.out.println("Actual final balance: Rs. " + sharedAccount.getBalance());
    }

    private static void demonstrateTransactionProcessor(Account account) {
        TransactionProcessor processor = new TransactionProcessor();
        processor.addAccount(account);
        int taskCount = 20;
        for (int i = 0; i < taskCount; i++) {
            processor.submit(() -> {
                try {
                    account.deposit(25);
                } catch (InvalidAmountException exception) {
                    System.out.println("Batch transaction failed: " + exception.getMessage());
                }
            });
        }
        try {
            processor.stop();
            System.out.println("Thread-pool transactions completed: " + taskCount);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while stopping transaction processor.");
        }
    }

    private static void demonstrateProducerConsumer() {
        TransactionBuffer buffer = new TransactionBuffer(3);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    int transactionNumber = i;
                    buffer.put(() -> System.out.println(
                            "Processed buffered transaction " + transactionNumber));
                    System.out.println("Produced transaction " + transactionNumber);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "TransactionProducer");
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    buffer.take().run();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "TransactionConsumer");

        producer.start();
        consumer.start();
        try {
            producer.join();
            consumer.join();
            System.out.println("Producer-consumer coordination completed.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while waiting for producer-consumer threads.");
        }
    }

    private static void demonstrateOrderedTransfers(Account first, Account second) {
        Thread transferOne = new Thread(
                () -> transferAndReport(first, second, 10), "Transfer-A-to-B");
        Thread transferTwo = new Thread(
                () -> transferAndReport(second, first, 10), "Transfer-B-to-A");
        transferOne.start();
        transferTwo.start();
        try {
            transferOne.join();
            transferTwo.join();
            System.out.println("Ordered-lock transfers completed without deadlock.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while waiting for transfers.");
        }
    }

    private static void demonstratePersistence(Account[] accounts)
            throws IOException, ClassNotFoundException {
        Path stateFile = Path.of("data", "accounts.ser");
        Path logsFolder = Path.of("data", "logs");
        Path logFile = logsFolder.resolve("transactions.log");
        Path reportFile = Path.of("data", "end-of-day-report.txt");

        TransactionLog.append(logFile, "DEPOSIT 500");
        TransactionLog.append(logFile, "WITHDRAW 200");
        StatePersister.save(accounts, stateFile);

        Account[] reloadedAccounts = StatePersister.load(stateFile);
        System.out.println("Reloaded accounts:");
        for (Account account : reloadedAccounts) {
            System.out.println(account);
        }

        ReportGenerator.generate(logsFolder, reportFile);
        System.out.println(Files.readString(reportFile));
    }

    private static void transferAndReport(Account from, Account to, long amount) {
        try {
            from.transfer(to, amount);
        } catch (BankException exception) {
            System.out.println(Thread.currentThread().getName() + " failed: "
                    + exception.getMessage());
        }
    }

    private static void printInterestRates(Account[] accounts) {
        System.out.println("Account interest rates:");
        for (Account account : accounts) {
            System.out.printf("%s: %.1f%% (yearly interest: Rs. %.2f)%n",
                    account.getAccountNumber(), account.interestRate(), account.yearlyInterest());
            if (account instanceof SavingsAccount savingsAccount) {
                System.out.println("Savings minimum balance: Rs. " + savingsAccount.getMinBalance());
            }
        }

    }

    private static void demonstrateWithdrawRules(Account account) {
        WithdrawRule anonymousRule = new WithdrawRule() {
            @Override
            public boolean allow(Account candidate, long amount) {
                return candidate.canWithdraw(amount);
            }
        };
        WithdrawRule lambdaRule = (candidate, amount) -> candidate.canWithdraw(amount);

        System.out.println("Anonymous withdrawal rule allows Rs. 100: "
                + anonymousRule.allow(account, 100));
        System.out.println("Lambda withdrawal rule allows Rs. 100: "
                + lambdaRule.allow(account, 100));
        System.out.println("Savings account is premium: " + (account instanceof model.Premium));
    }

    private static void printAccountDetails(Account[] accounts) {
        System.out.println("Account details:");
        for (Account account : accounts) {
            System.out.println(account);
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1. Open Account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Working Hours");
        System.out.println("6. Check Balance");
        System.out.println("7. List Accounts");
        System.out.println("8. Search Account");
        System.out.println("9. Process Batch");
        System.out.println("10. End-of-day Report");
        System.out.println("11. Exit");
        System.out.print("Choose an option: ");
    }

    private static void handleMenuOption(
            MenuOption option, Scanner scanner, BankService bankService) {
        try {
            switch (option) {
                case OPEN_ACCOUNT -> {
                    AccountType accountType = readAccountType(scanner);
                    System.out.print("Owner name: ");
                    String owner = scanner.nextLine();
                    System.out.print("Email: ");
                    String email = scanner.nextLine();
                    System.out.print("Mobile: ");
                    String mobile = scanner.nextLine();
                    System.out.print("Opening balance: ");
                    long balance = Long.parseLong(scanner.nextLine().trim());
                    System.out.println("Opened: " + bankService.openAccount(
                            owner, email, mobile, balance, accountType));
                }
                case DEPOSIT -> {
                    System.out.print("Account number: ");
                    String account = scanner.nextLine();
                    System.out.print("Amount: ");
                    bankService.deposit(account, Long.parseLong(scanner.nextLine().trim()));
                    System.out.println("Deposit completed.");
                }
                case WITHDRAW -> {
                    System.out.print("Account number: ");
                    String account = scanner.nextLine();
                    System.out.print("Amount: ");
                    bankService.withdraw(account, Long.parseLong(scanner.nextLine().trim()));
                    System.out.println("Withdrawal completed.");
                }
                case TRANSFER -> {
                    System.out.print("From account: ");
                    String from = scanner.nextLine();
                    System.out.print("To account: ");
                    String to = scanner.nextLine();
                    System.out.print("Amount: ");
                    bankService.transfer(from, to, Long.parseLong(scanner.nextLine().trim()));
                    System.out.println("Transfer completed.");
                }
                case CHECK_BALANCE -> {
                    System.out.print("Enter account number or owner name: ");
                    System.out.println(bankService.checkBalance(scanner.nextLine().trim()));
                }
                case LIST_ACCOUNTS -> bankService.listAccounts().forEach(System.out::println);
                case SEARCH_ACCOUNT -> {
                    System.out.print("Owner name: ");
                    bankService.searchByOwner(scanner.nextLine()).forEach(System.out::println);
                }
                case BATCH_PROCESS -> {
                    bankService.processBatch(List.of(
                            new Command(TransactionType.DEPOSIT, "AC0001", 100),
                            new Command(TransactionType.WITHDRAW, "AC0001", 50)));
                    System.out.println("Batch processing completed.");
                }
                case REPORT -> {
                    Path report = Path.of("data", "end-of-day-report.txt");
                    bankService.writeReport(report);
                    System.out.println(Files.readString(report));
                }
                case WORKING_HOURS -> System.out.println(
                        "Working hours: Monday to Friday, 9:00 AM to 5:00 PM.");
                case EXIT -> System.out.println("Thank you for banking with MiniBank.");
            }
        } catch (BankException | IOException | NumberFormatException exception) {
            System.out.println("Operation failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.out.println("Batch operation interrupted.");
        }
    }

    private static int readMenuChoice(Scanner scanner) {
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static AccountType readAccountType(Scanner scanner) {
        while (true) {
            System.out.println("1. Savings Account");
            System.out.println("2. Current Account");
            System.out.println("3. Fixed Deposit Account");
            System.out.print("Choose account type: ");

            int choice = readMenuChoice(scanner);
            AccountType accountType = switch (choice) {
                case 1 -> AccountType.SAVINGS;
                case 2 -> AccountType.CURRENT;
                case 3 -> AccountType.FIXED_DEPOSIT;
                default -> null;
            };

            if (accountType != null) {
                System.out.println("Selected account type: " + accountType);
                return accountType;
            }
            System.out.println("Invalid account type. Please choose 1, 2, or 3.");
        }
    }
}
