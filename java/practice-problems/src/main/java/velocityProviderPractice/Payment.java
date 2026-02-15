package practice;

import java.time.Instant;

public class Payment {
    /* The payment ID. */
    private final String paymentId;
    /* The timestamp of the payment processing start. */
    private final Instant timestamp;
    /* The hashed card number used for the payment. */
    private final String hashedCardNumber;
    /* Optional merchant identifier for per-merchant configs. */
    private final String merchantId;
    
    /*
     * EXTENSIBILITY NOTE - Merchant-Specific Configuration:
     * To support per-merchant retention/limits, add:
     *   private final String merchantId;
     * Then use Map<String, VelocityProviderConfig> or MerchantConfigService
     * to apply merchant-specific cleanup thresholds while keeping rule
     * durations external (passed as the duration parameter).
     */

    public Payment(String paymentId, Instant timestamp, String hashedCardNumber) {
        this.paymentId = paymentId;
        this.timestamp = timestamp;
        this.hashedCardNumber = hashedCardNumber;
        this.merchantId = null;
    }

    public Payment(String paymentId, Instant timestamp, String hashedCardNumber, String merchantId) {
        this.paymentId = paymentId;
        this.timestamp = timestamp;
        this.hashedCardNumber = hashedCardNumber;
        this.merchantId = merchantId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getHashedCardNumber() {
        return hashedCardNumber;
    }

    public String getMerchantId() {
        return merchantId;
    }
}
