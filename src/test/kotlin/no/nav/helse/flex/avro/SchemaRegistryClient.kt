package no.nav.helse.flex.avro

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import no.nav.helse.flex.kafka.Ident
import no.nav.helse.flex.kafka.IdentType
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.avro.reflect.AvroDoc
import org.apache.avro.reflect.ReflectData
import org.apache.kafka.common.serialization.Serializer
import org.mockito.Mockito

// hey gpt, this is what the code of Ident looks like:
/*@AvroDoc("Identer")
data class Ident(
    val idnummer: String,
    val gjeldende: Boolean,
    val type: IdentType,
) {
    fun toGenericRecord(): GenericRecord {
        val avroSchema = ReflectData.get().getSchema(this::class.java)
        return GenericRecordBuilder(avroSchema).apply {
            set("idnummer", this@Ident.idnummer)
            set("gjeldende", this@Ident.gjeldende)
            set("type", this@Ident.type.name)  // Assuming `IdentType` is an enum
        }.build()
    }
}*/

class SchemaRegistryClient {

    private val mockSchemaRegistryClient = Mockito.mock(SchemaRegistryClient::class.java)
    val identSchema: Schema = ReflectData.get().getSchema(Ident::class.java)
    val avroSchema = AvroSchema(identSchema)
    val serializer = KafkaAvroSerializer(mockSchemaRegistryClient)

    init {
        // Assuming you're registering a schema and fetching it by ID for tests
        val someSchemaId = 123  // Example schema ID
        Mockito.`when`(mockSchemaRegistryClient.register(anyString(), avroSchema)) // anystring is red :( :(
            .thenReturn(someSchemaId)
        Mockito.`when`(mockSchemaRegistryClient.getSchemaById(someSchemaId))
            .thenReturn(avroSchema)  // Now correctly returning a ParsedSchema
    }
}
