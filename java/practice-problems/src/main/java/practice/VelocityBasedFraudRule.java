package practice;

import practice.Solution.VelocityProvider;
import practice.Solution.VelocityProviderImpl;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityBasedFraudRule implements MerchantRule {

    private final MerchantConfigService configService;
    private final Map<String, VelocityProvider> providersByMerchant = new ConcurrentHashMap<>();

    public VelocityBasedFraudRule(MerchantConfigService configService) {
        this.configService = configService;
    }

    // Legacy/test constructor: single shared provider for all merchants.
    public VelocityBasedFraudRule(VelocityProvider velocityProvider) {
        this.configService = null;
        providersByMerchant.put(MerchantConfigService.DEFAULT_MERCHANT_ID, velocityProvider);
    }

    @Override
    public RuleCheckResult checkRule(Payment payment) {
        String merchantId = payment.getMerchantId() == null
                ? MerchantConfigService.DEFAULT_MERCHANT_ID
                : payment.getMerchantId();

        VelocityProvider provider;
        if (configService == null) {
            provider = providersByMerchant.get(MerchantConfigService.DEFAULT_MERCHANT_ID);
        } else {
            provider = providersByMerchant.computeIfAbsent(
                    merchantId,
                    id -> new VelocityProviderImpl(configService.getConfig(id)));
        }

        return provider.getCardUsageCount(payment, Duration.ofHours(1)) > 5
                ? RuleCheckResult.builder().allowed(false).build()
                : RuleCheckResult.builder().allowed(true).build();
    }

}
