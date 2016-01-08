# Weather Service
A Reactive Application that ingests, processes, reformats and stores automated weather station data, and provides for queries (e.g. daily, monthly and annual averages of temperature and precipitation)

* Streams automated weather station data to [Akka](http://http://akka.io/)
* Captures the data and places it in [Kafka](http://kafka.apache.org)
* Buffers the data until it is picked up by [Spark](http://spark.apache.org)
* Reformats the data and stores it in [Cassandra](http://cassandra.apache.org)
* Makes the data queryable via Akka, Spark and Cassandra

ScaledAction pipeline
![ScaledAction pipeline](https://github.com/scaledaction/sentiment-analysis/blob/images/images/WSPipeline1a.png)

# 1 Deploymenet via DCOS

# 1.1 Create a DCOS cluster and install the CLI

Use the AWS "Cloud Formation" service to bring up a cluster.

- You'll need enough capacity to run all the services (we recommended a Master of M3Large, a Public Slave of M3Large and 7 Slaves of M3Large)
- SSH access to the cluster
- Internet access from inside the cluster

When you open the dashboard, follow the instructions to install the DCOS CLI.

# 1.2 Install Cassandra and Kafka

You can either execute `./bin/base-install.sh <your DCOS cluster base URL>` or run the following commands yourself.

## Configure the DCOS CLI

Use `dcos config set core.dcos_url <your DCOS core URL>`, e.g.
`dcos config set core.dcos_url "http://peter-22f-elasticl-1ejv8oa4oyqw8-626125644.us-west-2.elb.amazonaws.com"`.


## Sequence of commands to run with the DCOS CLI

```console
# Start DCOS services:
dcos package install cassandra
dcos package install kafka

# When Kafka is healthy, add brokers
dcos kafka broker add 0..2
dcos kafka broker update 0..2 --options num.io.threads=16,num.partitions=6,default.replication.factor=2
dcos kafka broker start 0..2
# Show Kafka cluster status
dcos kafka broker list
```

# 1.3 Adjust the configuration

* Copy `etc/config_template.yml` to `etc/config.yml`
* Create a Twitter account with API keys ([see here for details](https://dev.twitter.com/oauth/overview/application-owner-access-tokens))
* Insert your credentials into the configuration file

#2 Execute SQL queries

#2.1 Set up a CQL shell and establish Cassandra tables

Run the scripts:
```console
 cqlsh> source 'create-timeseries.cql';
 cqlsh> source 'load-timeseries.cql';
```

From the Command Line

1.Start `weatherservice` cd /path/to/weatherservice sbt app/run

As the `weatherservice` app initializes, you will see Akka Cluster start, Zookeeper and the Kafka servers start.

2.Start the Kafka data feed app In a second shell run:

`sbt clients/run`

3.Open a third shell and again enter this but select WeatherServiceClientApp:

`sbt clients/run`

This api client runs queries against the Cassandra data (examples might be as follows):

* current temperature
* daily average temperature
* monthly average temperature
* monthly high and low temperatures
* daily and monthly precipitation
