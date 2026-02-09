package selfpractice.dayTwo.PaymentProcessingSystem;

import java.time.LocalDate;
import java.time.ZoneId;

public class CardPaymentDetails implements PaymentDetails {
    private String cardNumber;
    private String cvv;
    private LocalDate expiryDate;

    private CardPaymentDetails(String cardNumber, String cvv, LocalDate expiryDate) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getDetails() {
        return String.format("Card Details: %s, %s, %s", this.cardNumber, this.cvv, this.expiryDate);
    }

    public boolean validate() {
        boolean isValid = this.cardNumber != null && this.cvv != null && this.expiryDate != null;
        return isValid;
    }

    public boolean isExpired(long transactionTimestamp) {
        long startOfDay = this.expiryDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return startOfDay < transactionTimestamp;
    }

    @Override
    public String getId() {
        return this.cardNumber;
    }

    public static class Builder {
        private String cardNumber;
        private String cvv;
        private LocalDate expiryDate;

        public Builder cardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
            return this;
        }

        public Builder cvv(String cvv) {
            this.cvv = cvv;
            return this;
        }

        public Builder expiryDate(String expiryDate) {
            LocalDate date = LocalDate.parse(expiryDate);
            this.expiryDate = date;
            return this;
        }

        public CardPaymentDetails build() {
            if (this.cardNumber == null || this.cvv == null || this.expiryDate == null) {
                throw new IllegalArgumentException("Invalid Card Details");
            }
            return new CardPaymentDetails(cardNumber, cvv, expiryDate);
        }
    }

}
