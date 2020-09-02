package co.scifin.streams

import co.scifin.engine.sbe.AeronEventDecoder
import co.scifin.engine.sbe.MessageHeaderDecoder
import io.aeron.Subscription
import io.aeron.logbuffer.Header
import org.agrona.DirectBuffer
import org.agrona.concurrent.Agent
import org.apache.spark.streaming.receiver.Receiver


class AeronEventSubscriber(private val receiver: Receiver<AeronEvent>, private val subscription: Subscription) : Agent
{
    private val headerDecoder: MessageHeaderDecoder = MessageHeaderDecoder()
    private val bodyDecoder: AeronEventDecoder = AeronEventDecoder()

    override fun doWork(): Int
    {
        subscription.poll(this::handler, 1)
        return 0
    }

    override fun roleName(): String
    {
        return "Spark Subscriber"
    }

    private fun handler(buffer: DirectBuffer, offset: Int, length: Int, header: Header)
    {
        var bufferOffset = offset

        headerDecoder.wrap(buffer, bufferOffset)

        val templateId = headerDecoder.templateId()

        println("Template Id: $templateId")

        bufferOffset += headerDecoder.encodedLength()

        println("Message received")

        val aeronEvent = createAeronEvent(buffer, bufferOffset)

        receiver.store(aeronEvent)
    }

    private fun createAeronEvent(buffer: DirectBuffer, bufferOffset: Int): AeronEvent
    {
        val actingBlockLength = headerDecoder.blockLength()
        val actingVersion = headerDecoder.version()

        bodyDecoder.wrap(buffer, bufferOffset, actingBlockLength, actingVersion)

        val lastValue = bodyDecoder.sequence()
        val enumField = bodyDecoder.enumField().value().toLong()
        val message = bodyDecoder.message()

        return AeronEvent(lastValue, enumField, message)
    }
}