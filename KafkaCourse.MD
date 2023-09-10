# Apache Kafka
# Kafka theory

- Topics: a particular stream of data (similar to a table in DB),
  identified by its name,
  split in Partitions,
  the sequence of messages is called a data stream.
  you cannot query topics, instead, use Kafka Producers to send data and Kafka Consumers to read the data.
- Partitions: ordered and assign when create a Topic.
- Offset: Each message within a partition gets an incremental id.
* Order is guaranteed only within a partition (not across partitions)
* Data is kept only for a limited time (default is one week)
* Once the data is written to a partition, it can't be changed (immutability)
* Data is assigned randomly to a partition unless a key is provided
* Kafka only accepts bytes as an input from producers and sends bytes out as an output consumers.

- Kafka Message Key Hashing:
* Key Hashing is the process of determining the mapping of a key to a partition. In the default Kafka partitioner, the keys are hashed
  using murmur2 algorithm with the formula below:
  targetPartition = Math.abs(Utils.murmur2(keyBytes)) % (numPartitions - 1)


- Brokers: hold Kafka clusters (servers).
* Each broker is identified its ID (int) and contains certain topic partitions.
### We connect to each bootstrap broker, we will connect to the entire cluster.
### 3 brokers are okay (can up to 100 brokers).
* Topics are distributed on every broker because Kafka is distributed.
* Kafka broker also called "bootstrap server".

- Topic replication factor:
* topics should have a replication factor > 1 (usually between 2 and 3).
  at any time only ONE broker can be a leader for a given partition and only the leader can receive and serve data for partition.
  the other brokers will synchronize the data(they called ISR(in-sync replica)).
  Zookeeper decides for leader and replicas (determined).

- Producers: write data to the topics. producers can receive acknowledgment of data writes.
* Three types of acknowledgment exists in Kafka:
  1) acks = 0 : producers won't wait for ack from leader broker (possible data loss but highest throughput)
  2) acks = 1 : producers will wait for only leader broker ack  (limited data loss)
  3) acks = all : producers will wait for leader and all in-sync replicas (ISR) acks (no data loss because first all ISRs get data, then leader)
* producers can choose to send a key with message (string,number,etc...). if key=null data is sent round-robin.
  if a key is sent, then all messages for that key will always go to the same partition.
  it called Key Hashing. a key is basically sent if you need message ordering for a specif field.
* min.insync.replicas = 1: only the broker leader needs to successfully ack
* min.insync.replicas = 2: at least the broker leader and one replica need to ack
* #### NOT_ENOUGH_REPLICAS due to min.insync.replicas setting.

- Consumers: read data from a topic (identified by name) by pull model.
* consumers know which broker to read from
* in case of broker failures, consumers know how to recover
* data is read in order within each partition.
* The serialization/deserialization type must not change during a topic lifecycle(create new topic instead)
- Consumer groups: Consumers read data in consumer groups.
  each consumer within a group reads from exclusive partitions.
* if you have more consumers than partitions, some consumers will be inactive.

- Consumer offsets: Kafka stores the offsets at which a consumer group has been reading.
* The offsets committed live in a Kafka topic named __consumer_offsets.
* When a consumer in a group has processed data received from Kafka, it should be periodically
  committing the offsets.(the Kafka broker will write to __consumer_offsets, not the group itself)
  If a consumer dies, it will be able to read back from where it left off thanks to the committed consumer offsets.
* In Apache Kafka it is acceptable to have multiple consumer groups on the same topic.

- Delivery semantics for consumers: Consumers choose when to commit offsets.
  * There are 3 delivery semantics:
  1) At most once: offsets are committed as soon as the message is received.
     If the process goes wrong, the message will be lost (It won't be read again).
  2) At least once (usually preferred): offsets are committed after the message is processed.
     If the process goes wrong, the message will be read again. This can result in duplicate processing of messages.
     Make sure your processing is idempotent.(i.e. processing again the messages won't impact your systems).
  3) Exactly once: Can be achieved for Kafka => Kafka workflows using Kafka Streams API.
     For Kafka => External Systems workflows, use an idempotent consumer.

- Kafka Broker Discovery: You only need to connect to one broker, and you will be connected to the entire cluster.
  Each broker knows about all brokers,topics and partitions(metadata).

### Zookeeper: Manages brokers, helps in performing leader election for partitions,
sends notifications to Kafka in case of changes(e.g. new topic, broker dies, broker comes up, delete topics, etc...)
Zookeeper by design operates with an odd number of servers (1,3,5,7)
Zookeeper has a leader (handle writes) the rest of the servers are followers (handle reads)
Zookeeper does NOT store consumer offsets with Kafka > v0.10.
* Zookeeper is also less secure tha Kafka, and therefore Zookeeper ports should only be opened to allow traffic from Kafka brokers, and not Kafka clients.
* Kafka 2.x can't work without Zookeeper.
* Kafka 3.x can work without Zookeeper(KIP-500) - using Kafka Raft instead(KRaft).
* Kafka 4.x will not have Zookeeper.

* Kafka Kraft: In 2020, the Apache Kafka project started to work to remove the Zookeeper dependency from it (KIP-500).
  Zookeeper shows scaling issues when Kafka clusters have 100.000 partitions. By removing Zookeeper, Apache Kafka can:
  - Scaling to millions of partitions, and become easier to maintain and set-up.
  - Improve stability, makes it easier to monitor, support and administer.
  - Single security model for the whole system.
  - Single process to start with Kafka
  - Faster controller shutdown and recovery time

### Kafka Guarantees:
* Messages are appended to a topic-partition in the order they sent
* Consumers read messages in the order stored in a topic-partition
* WIth a replication factor of N, producers and consumers can tolerate up to N-1 brokers being down.
* This is why a replication factor of 3 is a good ideal:
  1) Allows for one broker to be taken down for maintenance
  2) Allows for another broker to be taken down unexpectedly
* Also long as number of partitions remains constant for a topic (no new partitions), the same key will always go to the same partition

### Consumer Rebalancing:
1) Eager Rebalance: Stop the world!, then reassign partitions to consumers.
2) Cooperative Rebalance: Reassign a small subset of the partitions from one consumer to another. Other consumers that don't have reassigned partitions can still process uninterrupted.
* Cooperative Rebalance: Has partition.assignment.strategy:
  1) RangeAssignor: assign partition on a per-topic-basis (can lead to imbalanace)
  2) RoundRobin: assign partition across all topics in round-robin fashion, optimal balance
  3) StickyAssignor: Balanced like RoundRobin, and then minimizes partition movements when consumer join/leave the group in order to minimize movements
  4) CooperativeStickyAssignor: Rebalance strategy is identical to StickyAssignor but supports cooperative rebalance and therefore consumers can keep on consuming from the topic
* Default assignor are [RangeAssignor, CooperativeStickyAssignor]
* Kafka Connect already implemented and enabled by default
* Kafka Streams turned on by default using StreamPartitionAssignor
* If a consumer leaves a group and joins back, it will have a new "member ID" and new partition assigned. use group.instance.id to specify a consumer static member.
  Remember session.timeout.ms should not reach else rebalance happened.(specific partition to a specific consumer)

* Offsets are committed when you call .poll() and auto.commit.interval.ms has elapsed.
* ### Make sure messages are all successfully processed before you call poll() again. If you don't, you will not be in at-least-once reading scenario.
* ### In some rare cases, you must disable enable.auto.commit, and most likely most processing to a separate thread and then from time-to-time call .commitSync() or .commitAsync() with the correct offsets manually(advanced)

### Producer Retries:
- retry.backoff.ms setting is by default 100 ms.
  -- max.in.flight.requests.per.connection = how many produce requests can be made in parallel (default 5)
* ### For ensure ordering set max.in.flight.requests.per.connection to 1.

### Idempotent Producer:
* Won't introduce duplicates on network error. Come with:
  1) retries = Integer.MAX_VALUE (2^31 - 1 = 2147483647, retries until delivery.timeout.ms is reached)
  2) max.in.flight.requests = 5 (high performance and keep ordering)
  3) acks = all
  4) delivery.timeout.ms = 120000 (fail after 2 minutes retrying)
* ### Since Kafka 3.0 the producer is "safe" by default: 	1) acks = all(-1) 	2) enable.idempotence = true , but in Kafka 2.8 and lower: 1) acks = 1	2) enable.idempotence = false

### Message Compression at the Producer level: enabled at the Producer level. compression.type can be none(default), gzip, lz4, snappy and zstd(old version). It can speed up batch produce.
* ### Compression can set at the broker level or topic-level(compression.type = producer)(compression.type=lz4)
* #### If your compression. type is equals to topic-level there is not another compression but if they are different, batches are decompressed by the broker and then recompressed.
* ### Warning: If you enable broker-side compression, it will consume extra CPU cycles.

### Producer Default Partitioner:
* When key is null two approach exist: 1) Round Robin: for Kafka 2.3 and below  2) Sticky Partitioner: for Kafka 2.4 and above.
* Sticky Partitioner improves the performance of the producer especially when high throughput when the key is null

* If the producer produces faster than the broker can take, the records will be buffered in memory. buffer.memory = 32MB
* If the buffer is full, then the .send() method will start to block. max.block.ms=60000 the time the .send() will block until throwing an exception.

### Delivery Semantics
#### 1) At Most Once : offsets are committed as soon as the message batch is received. If the processing goes wrong, the message will be lost(Never Twice but sometimes zero).
#### 2) At Least Once: committed after the message is processed. This can result in duplicate processing of messages. Make sure your processing is idempotent.
#### 3) Exactly Once: Can be achieved for Kafka -> Kafka workflows using the Transactional API(easy with Kafka Stream API). For Kafka => Sink workflows, use an idempotent consumer.

### Consumer Offset Commit Strategies:
#### 1) enable.auto.commit: true and synchronous processing of batches(easy)
#### 2) enable.auto.commit: false and manual commit of offsets(use commitSync() and commitAsync() with the correct offsets manually(advanced)).  For this you need to launch manually using .seek() API and model, store your offset in a database table for example and handle the cases where rebalances happen(ConsumerRebalancerListener interface)

* ## Make sure messages are all successfully processed before you call poll() again.

### Consumer Offset Reset Behavior:
* #### auto.offset.reset = 
  * latest (will read from the end of log)
  * earliest (will read from the start of the log)
  * none (will throw an exception if no offset is found)
* ### If a consumer hasn't read new data in retention time(7 days) offset can be lost(offset.retention.minutes)

### Consumer Groups
* #### Consumers in a group talk to a consumer groups coordinator. To detect consumers that are "down", there is a "heartbeat" mechanism and a "poll" mechanism.
* #### heartbeat.interval.ms = 3 seconds, usually set to  1/3rd of session.timeout.ms
* #### max.poll.interval.ms = 5 minutes, Maximum amount of time between two .poll() calls before declaring the consumer dead.
* #### max.poll.records = 500, Controls how many records to receive per poll request. Increase if your messages are very small and have a lot of available RAM. Lower it if it takes you too much time to process records.
* #### fetch.min.bytes = 1, Controls how much data you want to pull at least on each request. Help improving throughput and decreasing request number.Cost latency.
* #### fetch.max.wait.ms = 500, The Maximum amount of time the Kafka broker will block before answering the fetch request if there isn't sufficient data to immediately satisfy the requirements given by fetch.min.bytes you have up to 500 ms latency before the fetch returns data to the consumer.
* #### max.partition.fetch.bytes = 1MB, the maximum amount of data per partition the server will return. If you read from 100 partitions, you'll need a lot of memory(RAM).
* #### max.fetch.bytes = 55MB, Maximum data returned for each request. If you have available memory. try increasing it to allow the consumer to read more data in each request.

### Kafka Consumer Replica Fetching:
* #### It is possible to configure consumers to read from the closest replica(for latency and network cost). it called Rack Awareness. 
* #### For Broker setting: rack.id config must be set to the data center ID. replica.selector.class must be set(org.apache.kafka.common.replica.RackAwareReplicaSelector).
* #### For Consumer client setting: Set client.rack to the data center ID the consumer is launched on.

### Kafka Ecosystem: 
* #### Consumer and Producer considered low-level but Kafka and Ecosystem introduced over time some new API that are higher level that solves specific problems like Kafka Stream, Kafka Connect and Schema Registry.

## Kafka Connect
* ### A tool used for receive and send data with databases and sink data. A connect cluster made from workers. Get data from Common data source and sink to Common data stores.
* ### https://www.confluent.io/hub/confluentinc/kafka-connect-elasticsearch
* ### https://github.com/conduktor/kafka-connect-wikimedia/releases

## Kafka Streams
* ### Easy data processing and transformation library, process on a stream Kafka data like count, analyze and numbers of something in a Kafka topic, highly scalable, elastic and fault tolerant, Exactly-Once capabilities, One record at a time processing(no batching)

## Schema Registry
* ### Data verification for bad data, field renamed, change data format for not Consumer break. Data need to be self describable. the Kafka Brokers verify the messages when they received. It needs no CPU usage and zero-copy. The Schema Registry must be a separate components and Consumers and Producers need to be able to talk to it. Apache Avro as the data format(Protobuf, JSON Schema also supported). Producers send schema and Consumers retrieve schema. It must be high available and set up well.
* ### The purpose of Schema Registry: 1) Store and retrieve schemas for Producers/Consumers 2) Enforce Backward/Forward/Full compatibility on topics 3) Decrease the size of the payload of data sent to Kafka

### Partition Count & Replication Factor
* ### Small cluster(< 6 brokers): 3x # of brokers, Big cluster(> 12 brokers): 2x # of brokers
* ## Test! Every Kafka cluster will have different performance.
* ### Replication Factor should be at least 2, usually 3, maximum 4. Higher replication factor (N) --> Better durability of you system (N-1  brokers can fail) but more replication factor cause higher latency if acks=all.
* ## Set it to 3 to get started(you must have at least 3 brokers for that)
* ## Recommended  maximum of 4000 partitions per broker.
* ## Follow the Netflix model and create more Kafka clusters.

### Topic Naming Convention:
* ### |message type|dataset name|data name|data format|
* ### Message type: logging, queuing, tracking, etl/db, streaming, push, user
* ### Dataset name: analogous to a database name in traditional RDBMS systems. used as a category to group topics together.
* ### Data name: analogous to a table name in traditional RDBMS systems.
* ### Data format: .avro, .json, .text, .csv, .log
* ### User snake_case.

### Kafka for Admin:
* ### It's not easy to setup a cluster because you have to isolate each Zookeeper and Broker on separate servers, monitoring need to be implemented, operations must be mastered, need ad excellent Kafka Admin.

### Kafka Monitoring and Operations:
* ### Expose metrics through JMX, common places to host Kafka metrics are ELK, Datadog or NewRelic.
* ### Some Metrics: 
* #### 1) Under Replicated Partitions: Number of partitions are have problems with the ISR. May indicate a high load on system.
* #### 2) Request Handler: Utilization of threads for IO, network, etc... overall utilization of an Apache Kafka broker.
* #### 3) Request Timing: How long it takes to reply requests. Lower is better, as latency will be improved.
* ### Kafka Operations team must be able to perform the following tasks: 1) Rolling Restart of Brokers  2) Updating Configurations   3) Rebalancing Partitions  4) Increasing replication factor  5) Adding a broker  6) Replacing a Broker  7) Removing a Broker  8) Upgrading a Kafka Cluster with zero downtime

### Apache Kafka Security:
* ### Authentication: SSL: clients must use SSL certificates, SASL/PLAINTEXT: weak but easy setup and need username and password; SASL/SCRAM: username and password with a challenge and more secure; SASL/GSSAPI(Kerberos): such as Microsoft Active Directory, strong but hard to setup; SASL/OAUTHBEARER: leverage oauth token for authentication.
* ### Authorisation: use ACL(Access Control List)
* ### Encryption: ensures that the data exchanged between clients and brokers is sector to routers on the way.