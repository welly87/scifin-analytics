package co.scifin.streams

import co.scifin.engine.sbe.AeronEventEncoder
import co.scifin.engine.sbe.MessageHeaderEncoder
import co.scifin.engine.sbe.SampleEnum
import io.aeron.Aeron
import io.aeron.driver.MediaDriver
import org.agrona.concurrent.SleepingIdleStrategy
import org.agrona.concurrent.UnsafeBuffer
import java.nio.ByteBuffer


class AeronEventPublisher(private val aeron: Aeron)
{
    private var encoder: AeronEventEncoder = AeronEventEncoder()
    private var messageHeaderEncoder: MessageHeaderEncoder = MessageHeaderEncoder()

    private var unsafeBuffer: UnsafeBuffer? = null
    private var currentCountItem = 1

    fun start()
    {
        val channel = "aeron:ipc"
        val stream = 10

        val pub = aeron.addPublication(channel, stream)
        val idle = SleepingIdleStrategy()

        println("publication created")

        while (true)
        {
            createFakeEvent()

            if (pub.offer(unsafeBuffer) > 0)
            {
                println("sent: $currentCountItem")
                currentCountItem += 1
            }

            Thread.sleep(2000)
        }
    }

    private fun createFakeEvent()
    {
        println("fake event created")

        // TODO should reuse buffer, but this one is only fake/test, ignore this
        unsafeBuffer = UnsafeBuffer(ByteBuffer.allocateDirect(128))

        encoder = AeronEventEncoder()
        messageHeaderEncoder = MessageHeaderEncoder()

        encoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderEncoder)
        encoder.sequence(1)
        encoder.enumField(SampleEnum.VALUE_1)
        encoder.message("New Order")
    }
}


fun main()
{
    val mediaDriverCtx = MediaDriver.Context()
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true)

    val mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx)

    val aeronCtx = Aeron.Context()
            .aeronDirectoryName(mediaDriver.aeronDirectoryName())

    println("trying to connect...")
    val aeron = Aeron.connect(aeronCtx)

    println("connected...")
    val publisher = AeronEventPublisher(aeron)

    publisher.start()
}