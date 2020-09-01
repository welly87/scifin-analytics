package co.scifin.streams

import co.scifin.engine.sbe.AeronEventEncoder
import co.scifin.engine.sbe.MessageHeaderEncoder
import co.scifin.engine.sbe.SampleEnum
import io.aeron.Aeron
import io.aeron.Publication
import io.aeron.driver.MediaDriver
import org.agrona.concurrent.UnsafeBuffer
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer


class AeronEventPublisher(private val aeron: Aeron)
{
    private var encoder: AeronEventEncoder = AeronEventEncoder()
    private var messageHeaderEncoder: MessageHeaderEncoder = MessageHeaderEncoder()

    private var unsafeBuffer: UnsafeBuffer? = null
    private var currentCountItem = 1
    private val logger = LoggerFactory.getLogger(AeronEventPublisher::class.java)

    fun start()
    {
        val channel = "aeron:ipc"
        val stream = 10

        val publication: Publication = aeron.addPublication(channel, stream)

        if (publication.isConnected)
        {
            createFakeEvent()

            while (true)
            {
                if (publication.offer(unsafeBuffer) > 0)
                {
                    logger.info("sent: {}", currentCountItem)
                    currentCountItem += 1
                }

                Thread.sleep(2000)
            }
        }
    }

    private fun createFakeEvent()
    {
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
    val mediaDriverCtx = MediaDriver.Context().dirDeleteOnStart(true).dirDeleteOnShutdown(true)

    val mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx)

    val aeronCtx = Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName())
    val aeron = Aeron.connect(aeronCtx)

    val publisher = AeronEventPublisher(aeron)
    publisher.start()
}