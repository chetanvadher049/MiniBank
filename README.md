# MiniBank

A Java 17+ console banking application with an encapsulated customer and
polymorphic account model.

## Run with exactly two commands

Open PowerShell in the project root:

```powershell
cd E:\Minibank
```

Then execute these two commands:

```powershell
javac -d out (Get-ChildItem src -Filter *.java -Recurse).FullName
java -cp out service.MiniBank
```

The first command compiles every source file under `src` and places compiled
classes in `out`. The second command starts the `service.MiniBank` main class.
Start the application from `E:\Minibank` so runtime files are stored under
`data\`. Select option `11` to save state and exit.

No cleanup command, build script, Maven, XML file, JUnit, or external library
is required. The application reads and writes runtime data under `data\`.

Working hours and input validation are included as supplementary features.
`Customer` generates
read-only customer IDs, while `Account` protects its balance so it can only be
changed through `deposit` and `withdraw`.

`Account` is an abstract base class with `SavingsAccount`, `CurrentAccount`,
and `FixedDepositAccount` implementations. Each account type supplies its own
interest rate and withdrawal policy.

The source is organized into `model`, `util`, and `service` packages. Account
operations use `Transactable`, interest calculations use `InterestBearing`,
and withdrawal policies can be supplied with `WithdrawRule` implementations or
lambdas.

Runtime validation annotations are defined in `model.annotation`. `Account`
uses `@Id`, `@Positive`, and `@MaxLength`, and `AnnotationValidator` reads
these annotations through reflection.

Checked banking failures are defined in the `exception` package. Deposits and
withdrawals report invalid amounts and insufficient funds explicitly, while
account transfers propagate `BankException` safely.

`Account` deposit, withdrawal, and balance access are synchronized. The
`AccountWorker` runnable demonstrates multiple named threads updating one
shared account and joining before the final balance is checked.

`TransactionProcessor` runs batch work on a four-thread executor and waits for
shutdown completion. `TransactionBuffer` demonstrates producer-consumer
coordination with `wait()`/`notifyAll()`. Transfers acquire account locks in
account-number order so opposite-direction transfers cannot deadlock.

Account state is serializable through `StatePersister`. `TransactionLog`
appends audit entries under `data\logs`, and `ReportGenerator` scans `.log`
files with NIO to produce `data\end-of-day-report.txt`, including deposit and
withdrawal totals and file metadata.

`AccountStore` provides `HashMap` lookup by account number, an
`ArrayList` transaction history, balance sorting with a `Comparator`, and
balance filtering. `Account` is also `Comparable` by account number, while
`TransactionProcessor` keeps shared accounts in a `ConcurrentHashMap`.

`Repository<T>` is a reusable reflective repository that extracts keys from
`@Id` fields. It supports single and bulk saves, lookup, deletion, listing,
item counts, and the bounded generic `average` method. It works unchanged for
both `Account` and `Customer`.

`BankService` is the application service layer. It validates account creation,
coordinates repository lookups and checked banking exceptions, processes safe
batches through `TransactionProcessor`, writes transaction reports, and
persists state on startup and shutdown. The menu now provides real open,
deposit, withdraw, transfer, list, search, batch, report, and exit operations.
When opening an account, the user selects Savings, Current, or Fixed Deposit
before entering the account details.

The final menu is a working service-backed flow:

1. Open account
2. Deposit
3. Withdraw
4. Transfer
5. Working hours
6. Check balance by account number or owner name
7. List accounts
8. Search by owner
9. Process a safe transaction batch
10. Generate the end-of-day report
11. Exit and persist state
11. Exit and persist state
