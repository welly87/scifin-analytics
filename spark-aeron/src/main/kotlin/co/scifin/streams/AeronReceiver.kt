package co.scifin.streams

import io.aeron.Aeron
import io.aeron.Subscription
import io.aeron.driver.MediaDriver
import org.agrona.concurrent.AgentRunner
import org.agrona.concurrent.SleepingIdleStrategy
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver
import java.io.Serializable

data class AeronEvent(val lastValue: Long, val enumField: Long, val message: String?): Serializable

class AeronReceiver : Receiver<AeronEvent>(StorageLevel.MEMORY_AND_DISK_2())
{
    override fun onStart()
    {
        val mediaDriverCtx = MediaDriver.Context()
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)

        val mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx)

        val aeronCtx = Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName())
        val aeron = Aeron.connect(aeronCtx)

        val channel = "aeron:udp?endpoint=localhost:20121"
        val stream = 1001
        val subscription: Subscription = aeron.addSubscription(channel, stream)

        val receiveAgentRunner = AgentRunner(SleepingIdleStrategy(), { obj: Throwable -> obj.printStackTrace() }, null, AeronEventSubscriber(this, subscription))

        AgentRunner.startOnThread(receiveAgentRunner)

    }

    override fun onStop()
    {
        TODO("Not yet implemented")
    }
}