package no.nav.helse.flex.kafka
import no.nav.helse.flex.util.OBJECT_MAPPER
import org.apache.avro.Schema
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun uploadSchema(
    schemaRegistryUrl: String,
    topic: String,
    schemaPath: String,
) {
    // Parse the Avro schema
    val schema: Schema = Schema.Parser().parse(Any::class::class.java.classLoader.getResourceAsStream(schemaPath))

    // Convert the schema to JSON string
    val schemaJson = OBJECT_MAPPER.writeValueAsString(mapOf("schema" to schema.toString()))

    // Create HTTP client
    val client = HttpClient.newBuilder().build()

    // Create POST request
    val request =
        HttpRequest.newBuilder()
            .uri(URI.create("$schemaRegistryUrl/subjects/$topic-value/versions"))
            .header("Content-Type", "application/vnd.schemaregistry.v1+json")
            .POST(HttpRequest.BodyPublishers.ofString(schemaJson))
            .build()

    // Execute the request
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

    // Check the response
    if (response.statusCode() in 200..299) {
        println("Schema uploaded successfully. Response: ${response.body()}")
    } else {
        println("Failed to upload schema. Status: ${response.statusCode()}, Response: ${response.body()}")
    }
}
