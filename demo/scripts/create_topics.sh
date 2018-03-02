cd opt/kafka/bin/
echo "creating topics"
./kafka-topics.sh --zookeeper zookeeper:2181 --create --replication-factor 1 --partitions 1 --topic test
./kafka-topics.sh --zookeeper zookeeper:2181 --create --replication-factor 1 --partitions 1 --topic success
./kafka-topics.sh --zookeeper zookeeper:2181 --create --replication-factor 1 --partitions 1 --topic failure
./kafka-topics.sh --zookeeper zookeeper:2181 --create --replication-factor 1 --partitions 1 --topic shadow_topic
echo "topics created"
./kafka-topics.sh --zookeeper zookeeper:2181 --list