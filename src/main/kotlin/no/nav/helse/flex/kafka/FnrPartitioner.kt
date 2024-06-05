package no.nav.helse.flex.kafka

import no.nav.helse.flex.repository.Aktor
import org.apache.kafka.clients.producer.Partitioner
import org.apache.kafka.common.Cluster
import org.apache.kafka.common.InvalidRecordException
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.utils.Utils

abstract class FnrPartitioner : Partitioner {
    companion object {
        fun kalkulerPartisjon(
            keyBytes: ByteArray,
            numPartitions: Int,
        ): Int = Utils.toPositive(Utils.murmur2(keyBytes)) % (numPartitions)
    }

    override fun configure(configs: MutableMap<String, *>?) {}

    override fun close() {}

    override fun partition(
        topic: String?,
        key: Any?,
        keyBytes: ByteArray?,
        value: Any?,
        valueBytes: ByteArray?,
        cluster: Cluster?,
    ): Int {
        val partitions: List<PartitionInfo> = cluster!!.partitionsForTopic(topic)
        val numPartitions: Int = partitions.size

        if (keyBytes == null || key !is String) {
            throw InvalidRecordException("All messages should have a valid key.")
        }

        return kalkulerPartisjon(keyBytes, numPartitions)
    }
}

class AktorPartitioner : FnrPartitioner() {
    override fun partition(
        topic: String?,
        key: Any?,
        keyBytes: ByteArray?,
        value: Any?,
        valueBytes: ByteArray?,
        cluster: Cluster?,
    ): Int {
        val aktor = value as Aktor
        val actualKey: String = aktor.aktorId
        return super.partition(topic, actualKey, actualKey.toByteArray(), value, valueBytes, cluster)
    }
}
