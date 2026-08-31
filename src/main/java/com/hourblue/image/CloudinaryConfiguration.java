package com.hourblue.image;

import java.util.Map;

import com.cloudinary.Cloudinary;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.unit.DataSize;

@Configuration
class CloudinaryConfiguration {

    @Bean
    @Conditional(CompleteCloudinaryCredentialsCondition.class)
    Cloudinary cloudinary(
            @Value("${CLOUDINARY_CLOUD_NAME}") String cloudName,
            @Value("${CLOUDINARY_API_KEY}") String apiKey,
            @Value("${CLOUDINARY_API_SECRET}") String apiSecret) {
        return new Cloudinary(Map.of(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    @Bean
    @ConditionalOnBean(Cloudinary.class)
    CloudinaryImageStorage cloudinaryImageStorage(
            Cloudinary cloudinary,
            @Value("${CLOUDINARY_FOLDER:hourblue/posts}") String folder,
            @Value("${MAX_IMAGE_SIZE:5MB}") String maxImageSize) {
        return new CloudinaryImageStorage(cloudinary, folder, DataSize.parse(maxImageSize).toBytes());
    }

    static class CompleteCloudinaryCredentialsCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return hasText(environment, "CLOUDINARY_CLOUD_NAME")
                    && hasText(environment, "CLOUDINARY_API_KEY")
                    && hasText(environment, "CLOUDINARY_API_SECRET");
        }

        private boolean hasText(Environment environment, String name) {
            String value = environment.getProperty(name);
            return value != null && !value.isBlank();
        }
    }
}
