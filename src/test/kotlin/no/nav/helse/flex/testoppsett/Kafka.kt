package no.nav.helse.flex.testoppsett

import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName

fun startKafkaContainer(network: Network): KafkaContainer {
    return KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1")).apply {
        withNetwork(network)
        start()
        System.setProperty("KAFKA_BROKERS", bootstrapServers)
    }
}
