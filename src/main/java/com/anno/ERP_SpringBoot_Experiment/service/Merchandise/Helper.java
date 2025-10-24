package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component("featureMerchandiseHelper")
@RequiredArgsConstructor
public class Helper {

    private static final String ALPHANUMERIC_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    UUID convertStringToUUID(String id) {
        if (id == null || id.length() != 32) {
            throw new IllegalArgumentException("Invalid ID format. Expected 32 characters.");
        }

        String formattedId = String.format("%s-%s-%s-%s-%s",
                id.substring(0, 8),
                id.substring(8, 12),
                id.substring(12, 16),
                id.substring(16, 20),
                id.substring(20, 32)
        );

        return UUID.fromString(formattedId);
    }

    public String generateKey() {
        StringBuilder sb = new StringBuilder(5);

        for (int i = 0; i < 5; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(ALPHANUMERIC_CHARACTERS.length());
            sb.append(ALPHANUMERIC_CHARACTERS.charAt(randomIndex));
        }

        return sb.toString();
    }
}
