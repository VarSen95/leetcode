package practice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class VelocityProviderTest {
    
    private Solution.VelocityProvider provider;
    
    @BeforeEach
    public void setUp() {
        provider = Solution.VelocityProvider.getProvider();
    }
    
    @Test
    public void testNoPaymentsRegistered() {
        Solution.Payment payment = new Solution.Payment(
            UUID.randomUUID().toString(),
            Instant.now(),
            "card123"
        );
        
        int count = provider.getCardUsageCount(payment, Duration.ofMinutes(10));
        assertEquals(0, count, "Should return 0 when no payments registered");
    }
    
    @Test
    public void testSinglePaymentWithinWindow() {
        Instant now = Instant.now();
        String cardHash = "card123";
        
        // Register a payment
        Solution.Payment payment1 = new Solution.Payment(
            UUID.randomUUID().toString(),
            now,
            cardHash
        );
        provider.registerPayment(payment1);
        
        // Query 5 minutes later with 10 minute window
        Solution.Payment queryPayment = new Solution.Payment(
            UUID.randomUUID().toString(),
            now.plus(Duration.ofMinutes(5)),
            cardHash
        );
        
        int count = provider.getCardUsageCount(queryPayment, Duration.ofMinutes(10));
        assertEquals(1, count, "Should find 1 payment within 10 minute window");
    }
    
    @Test
    public void testMultiplePaymentsWithinWindow() {
        Instant baseTime = Instant.now();
        String cardHash = "card456";
        
        // Register 5 payments over 8 minutes
        for (int i = 0; i < 5; i++) {
            Solution.Payment payment = new Solution.Payment(
                UUID.randomUUID().toString(),
                baseTime.plus(Duration.ofMinutes(i * 2)),
                cardHash
            );
            provider.registerPayment(payment);
        }
        
        // Query at 10 minutes with 10 minute window - should see all 5
        Solution.Payment queryPayment = new Solution.Payment(
            UUID.randomUUID().toString(),
            baseTime.plus(Duration.ofMinutes(10)),
            cardHash
        );
        
        int count = provider.getCardUsageCount(queryPayment, Duration.ofMinutes(10));
        assertEquals(5, count, "Should find all 5 payments within 10 minute window");
    }
    
    @Test
    public void testPaymentsOutsideWindow() {
        Instant baseTime = Instant.now();
        String cardHash = "card789";
        
        // Register payment at time 0
        Solution.Payment payment1 = new Solution.Payment(
            UUID.randomUUID().toString(),
            baseTime,
            cardHash
        );
        provider.registerPayment(payment1);
        
        // Query 15 minutes later with 10 minute window - should not see old payment
        Solution.Payment queryPayment = new Solution.Payment(
            UUID.randomUUID().toString(),
            baseTime.plus(Duration.ofMinutes(15)),
            cardHash
        );
        
        int count = provider.getCardUsageCount(queryPayment, Duration.ofMinutes(10));
        assertEquals(0, count, "Should not find payments outside window");
    }
    
    @Test
    public void testMixedPaymentsInsideAndOutsideWindow() {
        Instant baseTime = Instant.now();
        String cardHash = "cardABC";
        
        // Register payments at 0, 5, 15, 20 minutes
        int[] minuteOffsets = {0, 5, 15, 20};
        for (int offset : minuteOffsets) {
            Solution.Payment payment = new Solution.Payment(
                UUID.randomUUID().toString(),
                baseTime.plus(Duration.ofMinutes(offset)),
                cardHash
            );
            provider.registerPayment(payment);
        }
        
        // Query at 18 minutes with 10 minute window - should see payments at 15 only
        Solution.Payment queryPayment = new Solution.Payment(
            UUID.randomUUID().toString(),
            baseTime.plus(Duration.ofMinutes(18)),
            cardHash
        );
        
        int count = provider.getCardUsageCount(queryPayment, Duration.ofMinutes(10));
        assertEquals(1, count, "Should find only 1 payment within window");
    }
    
    @Test
    public void testDifferentCards() {
        Instant now = Instant.now();
        
        // Register payments for different cards
        Solution.Payment payment1 = new Solution.Payment(
            UUID.randomUUID().toString(),
            now,
            "card001"
        );
        provider.registerPayment(payment1);
        
        Solution.Payment payment2 = new Solution.Payment(
            UUID.randomUUID().toString(),
            now,
            "card002"
        );
        provider.registerPayment(payment2);
        
        // Query for card001 - should only see its own payment
        Solution.Payment queryPayment = new Solution.Payment(
            UUID.randomUUID().toString(),
            now.plus(Duration.ofMinutes(1)),
            "card001"
        );
        
        int count = provider.getCardUsageCount(queryPayment, Duration.ofMinutes(10));
        assertEquals(1, count, "Should only count payments for the specific card");
    }
    
    @Test
    public void testEdgeCaseExactWindowBoundary() {
        Instant baseTime = Instant.now();
        String cardHash = "cardEdge";
        
        // Register payment at time 0
        Solution.Payment payment1 = new Solution.Payment(
            UUID.randomUUID().toString(),
            baseTime,
            cardHash
        );
        provider.registerPayment(payment1);
        
        // Query exactly 10 minutes later with 10 minute window
        Solution.Payment queryPayment = new Solution.Payment(
            UUID.randomUUID().toString(),
            baseTime.plus(Duration.ofMinutes(10)),
            cardHash
        );
        
        int count = provider.getCardUsageCount(queryPayment, Duration.ofMinutes(10));
        assertEquals(1, count, "Payment at exact window boundary should be included");
    }
}
