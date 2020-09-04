package co.scifin.streams

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.api.java.JavaStreamingContext


fun main()
{
    val conf = SparkConf()
            .setMaster("local[*]")
            .setAppName("Aeron Subscriber")
    val ssc = JavaStreamingContext(conf, Duration(1000))

    val events = ssc.receiverStream(AeronReceiver())

    events.foreachRDD { rdd, time ->

        val spark = SparkSessionSingleton.getInstance(rdd.context().conf)!!

        val events = spark.createDataFrame(rdd, AeronEvent::class.java)

        events.createOrReplaceTempView("aerons")

        val df = spark.sql("select * from aerons")

        df.printSchema()

        println("========= $time=========")
        df.show()
    }

    ssc.start()
    ssc.awaitTermination()
}

internal object SparkSessionSingleton
{
    @Transient
    private var instance: SparkSession? = null

    fun getInstance(sparkConf: SparkConf?): SparkSession?
    {
        if (instance == null)
        {
            instance = SparkSession.builder().config(sparkConf).orCreate
        }

        return instance
    }
}