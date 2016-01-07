# WeatherService
A Reactive Application that ingests, processes, reformats and stores automated weather station data, and provides for queries (e.g. daily, monthly and annual averages of temperature and precipitation)

* Streams automated weather station data to [Akka](http://http://akka.io/)
* Captures the data and places it in [Kafka](http://kafka.apache.org)
* Buffers the data until it is picked up by [Spark](http://spark.apache.org)
* Reformats the data and stores it in [Cassandra](http://cassandra.apache.org)
* Makes the data queryable via Akka, Spark and Cassandra

ScaledAction pipeline
![ScaledAction pipeline](https://github.com/scaledaction/sentiment-analysis/blob/images/images/WSPipeline1a.png)
