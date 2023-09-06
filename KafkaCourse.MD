# Apache Kafka
# Kafka theory

- Topics: a particular stream of data (similar to a table in DB),
  identified by its name,
  splited in Partitions,
  the sequence of messages is called a data stream.
  you cannot query topics, instead, use Kafka Producers to send data and Kafkaa Consumers to read the data.
- Partitions: ordered and assign when create a Topic.
- Offset: Each message within a partition gets an incremental id.
* Order is guaranteed only within a partition (not across partitions)
* Data is kept only for a limited time (default is one week)
* Once the data is written to a partition, it can't be changed (immutability)
* Data is assigned randomly to a partition unless a key is provided
* Kafka only accepts bytes as an input from producers and sends bytes out as an ouput consumers.

- Kafka Message Key Hashing:
* Key Hashing is the process of determining the mapping of a key to a partition. In the default Kafka partitioner, the keys are hashed
  using mumur2 algorithm with the formula below:
  targetPartition = Math.abs(Utils.murmur2(keyBytes)) % (numPartitions - 1)


- Brokers: hold Kafka clusters (servers).
* Each broker is identified its ID (int) and contains certain topic partitions.
### We connect to each bootstrap broker, we will connected to the entire cluster.
### 3 brokers are okay (can up to 100 brokers).
* Topics are distributed on every brokers because Kafka is distributed.
* Kafka broker also called "bootstrap server".

- Topic replication factor:
* topics should have a replication factor > 1 (usually between 2 and 3).
  at any time only ONE broker can be a leader for a given partition and only the leader can receive and serve data for patition.
  the other brokers will synchronize the data(they called ISR(in-sync replica)).
  Zookeeper decides for leader and replicas (determined).

- Producers: write data to the topics. producers can receive acknowledgment of data writes.
* Three types of acknowledgment exists in Kafka:
  1) acks = 0 : producers won't wait for ack (possible data loss)
  2) acks = 1 : producers will wait for leader ack (limited data loss)
  3) acks = all : producers will wait for leader and all replicas acks (no data loss)
* producers can choose to send a key with message (string,number,etc...). if key=null data is sent round robin.
  if a key is sent, then all messages for that key will always go to the same partition.
  it called Key Hashing. a key is basically sent if you need message ordering for a speicif field.

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
  commiting the offsets.(the Kafka broker will write to __consumer_offsets, not the group itself)
  If a consumer dies, it will be able to read back from where it left off thanks to the commited consumer offsets.
* In Apache Kafka it is acceptable to have multiple consumer groups on the same topic.

- Delivery semantics for consumers: Consumers choose when to commit offsets.
  * There are 3 delivery semantics:
  1) At most once: offsets are commited as soon as the message is received.
     If the process goes wrong, the message will be lost (It won't be read again).
  2) At least once (usually prefered): offsets are commited after the message is processed.
     If the process goes wrong, the message will be read again. This can result in duplicate processing of messages.
     Make sure your processing is idempotent.(i.e. processing again the messages won't impact your systems).
  3) Exactly once: Can be achieved for Kafka => Kafka workflows using Kafka Streams API.
     For Kafka => External Systems workflows, use an idempotent consumer.

- Kafka Broker Discovery: You only need to connect to one broker and you will be connected to the entire cluster.
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
  - Scaling to milions of partitions, and become easier to maintain and set-up.
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
* Alson long as number of partitions remains constant for a topic (no new partitions), the same key will always go to the same partition

### Consumer Rebalancing:
1) Eager Rebalance: Stop the world!, then reassign partitions to consumers.
2) Cooperative Rebalance: Reassign a small subset of the partitions from one consumer to another. Other consumers that don't have reassigned partitions can still process uninterrupted.
* Cooperative Rebalance: Has partition.assignment.strategy:
  1) RangeAssignor: assign partition on a per-topic-basis (can lead to imbalanace)
  2) RoundRobin: assign partition across all topics in round-robin fashion, optimal balance
  3) StickyAssignor: Balanced like RoundRobin, and then minimizes partition movements when consumer join/leave the group in ordeer to minimize movements
  4) CooperativeStickyAssignor: Relabance strategy is identical to StickyAssignor but supports cooperative relabances and therefore consumers can keep on consuming from the topic
* Default assignor are [RangeAssignor, CooperativeStickyAssignor]
* Kafka Connect already implemented and enabled by default
* Kafka Streams turend on by default using StreamPartitionAssignor
* If a consumer leaves a group and joins back, it will have a new "member ID" and new partition assigned. use group.instance.id to sepcifiy a consumer static member.
  Remember session.timeout.ms should not reach else relabance happened.(specific partition to a specific consumer)

* Offsets are commited when you call .poll() and auto.commit.interval.ms has elapsed.
* #### Make sure messages are all successfully processed before you call poll() again. If you don't, you will not be in at-least-once reading scenario. In some rare cases, you must disable enable.auto.commit, and most likely most processing to a separate thread and then from time-to-time call .commitSync() or .commitAsync() with the correct offsets manually(advanced)