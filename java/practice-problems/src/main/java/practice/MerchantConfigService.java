package practice;

import java.util.HashMap;
import java.util.Map;

import practice.Solution.VelocityProviderConfig;

public class MerchantConfigService {

    public static final String DEFAULT_MERCHANT_ID = "default";

    private final Map<String, VelocityProviderConfig> configs = new HashMap<>();
    private final VelocityProviderConfig defaultConfig;

    public MerchantConfigService() {
        this(VelocityProviderConfig.defaultConfig());
    }

    public MerchantConfigService(VelocityProviderConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public void addConfig(String merchantId, VelocityProviderConfig config) {
        configs.put(merchantId, config);
    }

    public VelocityProviderConfig getConfig(String merchantId) {
        if (merchantId == null || merchantId.isEmpty()) {
            return defaultConfig;
        }
        return configs.getOrDefault(merchantId, defaultConfig);
    }
}
