package com.mytext.learningassistant.rag;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.support-routing")
public record SupportRoutingProperties(boolean smallEnabled, String smallBaseUrl, String smallApiKey,
                                       String smallModel, Duration smallTimeout) {
    public SupportRoutingProperties {
        smallBaseUrl = smallBaseUrl == null ? "" : smallBaseUrl.trim();
        smallApiKey = smallApiKey == null ? "" : smallApiKey.trim();
        smallModel = smallModel == null ? "" : smallModel.trim();
        smallTimeout = smallTimeout == null ? Duration.ofSeconds(8) : smallTimeout;
    }

    public boolean smallConfigured() {
        return smallEnabled && !smallBaseUrl.isBlank() && !smallApiKey.isBlank() && !smallModel.isBlank();
    }
}
