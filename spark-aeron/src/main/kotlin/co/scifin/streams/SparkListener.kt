package co.scifin.streams

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.api.java.JavaStreamingContext


fun main()
{
    val sparkConf = SparkConf().setAppName("Aeron Subscriber")
    val ssc = JavaStreamingContext(sparkConf, Duration(1000))

    val lines = ssc.receiverStream(CustomReceiver())

    val spark = SparkSession.builder().appName("Aeron Subscriber").orCreate


}