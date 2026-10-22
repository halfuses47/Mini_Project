package model;

public interface InterestBearing {
    double interestRate();
    long getBalance();

    default double yearlyInterest() {
        return (interestRate() / 100.0) * getBalance();
    }
}
