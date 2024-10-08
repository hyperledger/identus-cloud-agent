package org.hyperledger.identus.shared.messaging

import java.time.Duration

case class MessagingServiceConfig(
    connectFlow: ConsumerJobConfig,
    issueFlow: ConsumerJobConfig,
    presentFlow: ConsumerJobConfig,
    didStateSync: ConsumerJobConfig,
    statusListSync: ConsumerJobConfig,
    kafkaEnabled: Boolean,
    kafka: Option[KafkaConfig]
)

final case class ConsumerJobConfig(
    consumerCount: Int,
    retryStrategy: Option[ConsumerRetryStrategy]
)

final case class ConsumerRetryStrategy(
    maxRetries: Int,
    initialDelay: Duration,
    maxDelay: Duration
)

final case class KafkaConfig(
    bootstrapServers: String,
    consumers: KafkaConsumersConfig
)

final case class KafkaConsumersConfig(
    autoCreateTopics: Boolean,
    maxPollRecords: Int,
    maxPollInterval: Duration,
    pollTimeout: Duration,
    rebalanceSafeCommits: Boolean
)
