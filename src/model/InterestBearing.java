package model;

public interface InterestBearing {
    double interestRate();

    default double yearlyInterest() {
        return getBalanceForInterest() * interestRate() / 100;
    }

    default double monthlyInterest() {
        return yearlyInterest() / 12;
    }

    private long getBalanceForInterest() {
        return ((Account) this).getBalance();
    }
}
