package no.nav.helse.flex.avro
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.common.serialization.Serializer
import org.mockito.Mockito

class SchemaRegistryClient {
    val mockSchemaRegistryClient = Mockito.mock(SchemaRegistryClient::class.java)
    val someSchemaId = 123  // Example schema ID

// Assuming you're registering a schema
    Mockito.`when`(mockSchemaRegistryClient.register(anyString(), any(Schema::class.java)))
    .thenReturn(someSchemaId)

    // Assuming you need to fetch a schema
    val mockSchema = AvroSchema(YourAvroClass.getClassSchema())  // Your Avro class schema
    Mockito.`when`(mockSchemaRegistryClient.getSchemaById(someSchemaId))
    .thenReturn(mockSchema)

    // Set up your serializer with the mocked client
    val serializer = KafkaAvroSerializer(mockSchemaRegistryClient)

}
