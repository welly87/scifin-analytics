package co.scifin.streams

import io.aeron.Aeron
import io.aeron.Subscription
import io.aeron.driver.MediaDriver
import org.agrona.concurrent.AgentRunner
import org.agrona.concurrent.SleepingIdleStrategy
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver

class AeronEvent(lastValue: Long, enumField: Long, message: String?)

class CustomReceiver : Receiver<AeronEvent>(StorageLevel.MEMORY_AND_DISK_2())
{
    override fun onStart()
    {
        val mediaDriverCtx: MediaDriver.Context = MediaDriver.Context().dirDeleteOnStart(true).dirDeleteOnShutdown(true)

        val mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx)

        val aeronCtx = Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName())
        val aeron = Aeron.connect(aeronCtx)

        val channel = "aeron:ipc"
        val stream = 10
        val subscription: Subscription = aeron.addSubscription(channel, stream)

        val receiveAgentRunner = AgentRunner(SleepingIdleStrategy(), { obj: Throwable -> obj.printStackTrace() }, null, AeronEventSubscriber(this, subscription))

        AgentRunner.startOnThread(receiveAgentRunner)
    }


    override fun onStop()
    {
        TODO("Not yet implemented")
    }

}