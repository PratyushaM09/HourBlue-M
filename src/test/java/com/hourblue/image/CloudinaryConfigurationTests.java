package com.hourblue.image;

import com.cloudinary.Cloudinary;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CloudinaryConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CloudinaryConfiguration.class);

    @Test
    void missingCredentialsCreateNoCloudinaryBeans() {
        contextRunner.run(context -> {
            assertNoBean(context, Cloudinary.class);
            assertNoBean(context, CloudinaryImageStorage.class);
        });
    }

    @Test
    void completeCredentialsCreateCloudinaryBeans() {
        contextRunner
                .withPropertyValues(
                        "CLOUDINARY_CLOUD_NAME=test-cloud",
                        "CLOUDINARY_API_KEY=test-key",
                        "CLOUDINARY_API_SECRET=test-secret")
                .run(context -> {
                    context.getBean(Cloudinary.class);
                    context.getBean(CloudinaryImageStorage.class);
                });
    }

    @Test
    void incompleteCredentialsCreateNoCloudinaryBeans() {
        contextRunner
                .withPropertyValues(
                        "CLOUDINARY_CLOUD_NAME=test-cloud",
                        "CLOUDINARY_API_KEY=test-key",
                        "CLOUDINARY_API_SECRET=")
                .run(context -> {
                    assertNoBean(context, Cloudinary.class);
                    assertNoBean(context, CloudinaryImageStorage.class);
                });
    }

    private void assertNoBean(org.springframework.context.ApplicationContext context, Class<?> type) {
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.beans.factory.NoSuchBeanDefinitionException.class,
                () -> context.getBean(type));
    }
}
