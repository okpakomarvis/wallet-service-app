package org.fintech.wallet.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {


    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name("transaction-events")
                .partitions(2)
                .replicas(1)
                .compact()
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name("notification-events")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic kycEventsTopic() {
        return TopicBuilder.name("kyc-events")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditLogsTopic() {
        return TopicBuilder.name("audit-logs")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudDetectionTopic() {
        return TopicBuilder.name("fraud-detection")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic walletEventsTopic() {
        return TopicBuilder.name("wallet-events")
                .partitions(2)
                .replicas(1)
                .build();
    }
}
