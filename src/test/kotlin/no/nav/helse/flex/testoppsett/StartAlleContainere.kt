package no.nav.helse.flex.testoppsett

import org.testcontainers.containers.Network

fun startAlleContainere() {
    val network = Network.newNetwork()
    startRedisContainer()
    val kafkaContainer = startKafkaContainer(network)
    startSchemaRegistry(network, kafkaContainer)
    startPostgresContainer()
}
