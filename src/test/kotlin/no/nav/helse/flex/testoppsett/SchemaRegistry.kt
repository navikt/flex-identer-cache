package no.nav.helse.flex.testoppsett

import no.nav.helse.flex.kafka.uploadSchema
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

fun startSchemaRegistry(
    network: Network,
    kafkaContainer: KafkaContainer,
) {
    GenericContainer(DockerImageName.parse("confluentinc/cp-schema-registry:7.5.3")).apply {
        withNetwork(network)
        withExposedPorts(8081)
        withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
        withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://${kafkaContainer.networkAliases[0]}:9092")
        start()
        waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
        System.setProperty(
            "KAFKA_SCHEMA_REGISTRY",
            "http://$host:${getMappedPort(8081)}",
        ) // TODO: sett opp mock for å støtte SSL i tester (https)

        // Upload the schema
        val schemaRegistryUrl = System.getProperty("KAFKA_SCHEMA_REGISTRY")
        uploadSchema(schemaRegistryUrl, "Aktor", "avro/aktor.avsc")
    }
}
