package org.fintech.wallet.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.fintech.wallet.dto.event.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Profile("dev")
@EnableKafka
@Configuration
public class KafkaConsumerConfigDev {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:wallet-service-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, WalletEvent> walletEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(WalletEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WalletEvent> walletEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WalletEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(walletEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, TransactionEvent> transactionEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(TransactionEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> transactionEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transactionEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
    @Bean
    public ConsumerFactory<String, KycEvent> kycEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(KycEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, KycEvent> kycEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KycEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kycEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
    @Bean
    public ConsumerFactory<String, FraudDetectionEvent> fraudDetectionEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(FraudDetectionEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FraudDetectionEvent> fraudDetectionEventConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FraudDetectionEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fraudDetectionEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
    @Bean
    public ConsumerFactory<String, AuditLogEvent> auditLogEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(AuditLogEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuditLogEvent> auditLogEventConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AuditLogEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(auditLogEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId); // will be overridden per consumer if needed
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.fintech.wallet.dto.event");
        return props;
    }

}
