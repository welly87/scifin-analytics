package co.scifin.streams

import co.scifin.engine.sbe.AeronEventEncoder
import co.scifin.engine.sbe.MessageHeaderEncoder
import co.scifin.engine.sbe.SampleEnum
import io.aeron.Aeron
import io.aeron.Publication
import io.aeron.driver.MediaDriver
import org.agrona.concurrent.UnsafeBuffer
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class AeronEventPublisher
{
    fun start()
    {
        val mediaDriverCtx = MediaDriver.Context()
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)

        val mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx)

        val ctx = Aeron.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName())

        println("trying to connect...")
        val aeron = Aeron.connect(ctx)

        println("connected...")

        val channel = "aeron:udp?endpoint=localhost:20121"
        val stream = 1001

        val publication = aeron.addPublication(channel, stream)

        for (i in 0 until 1000)
        {
            val message = "Hello World! $i"

            val unsafeBuffer = UnsafeBuffer(ByteBuffer.allocateDirect(128))

            val encoder = AeronEventEncoder()
            val messageHeaderEncoder = MessageHeaderEncoder()

            encoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderEncoder)
            encoder.sequence(1)
            encoder.enumField(SampleEnum.VALUE_1)
            encoder.message("New Order")

            val result = publication.offer(unsafeBuffer)

            if (result < 0L)
            {
                if (result == Publication.BACK_PRESSURED)
                {
                    println("Offer failed due to back pressure")
                }
                else if (result == Publication.NOT_CONNECTED)
                {
                    println("Offer failed because publisher is not connected to subscriber")
                }
                else if (result == Publication.ADMIN_ACTION)
                {
                    println("Offer failed because of an administration action in the system")
                }
                else if (result == Publication.CLOSED)
                {
                    println("Offer failed publication is closed")
                    break
                }
                else if (result == Publication.MAX_POSITION_EXCEEDED)
                {
                    println("Offer failed due to publication reaching max position")
                    break
                }
                else
                {
                    println("Offer failed due to unknown reason: $result")
                }
            }
            else
            {
                println("yay!")
            }
            if (!publication.isConnected)
            {
                println("No active subscribers detected")
            }
            Thread.sleep(TimeUnit.SECONDS.toMillis(1))
        }

        println("Done sending.")

        println("Lingering for ")

        Thread.sleep(3000)
    }
}

fun main()
{
    val publisher = AeronEventPublisher()

    publisher.start()
}