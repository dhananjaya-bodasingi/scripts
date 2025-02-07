#!/bin/bash
# Check if the number of arguments is less than 1
if [ "$#" -lt 1 ]; then
    echo "Error: Missing required argument."
    echo "Usage: $0 <required_argument>"
    exit 1
fi

ENV=$1

while IFS= read -r topic
do
 echo "Creating local rack topic $ENV.$topic"
 /var/lib/kafka/bin/kafka-topics.sh --bootstrap-server platformkafka1:9092,platformkafka2:9092,platformkafka3:9092,platformkafka4:9092,platformkafka5:9092,platformkafka6:9092 --create --topic $ENV.eur1.$topic --config compression.type=snappy --config retention.ms=86400000 --config retention.ms=86400000 --config min.insync.replicas=2 --replica-assignment 1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2

 /var/lib/kafka/bin/kafka-topics.sh --bootstrap-server platformkafka1:9092,platformkafka2:9092,platformkafka3:9092,platformkafka4:9092,platformkafka5:9092,platformkafka6:9092 --create --topic $ENV.eur1.$topic.one --config compression.type=snappy --config retention.ms=86400000 --config retention.ms=86400000 --config min.insync.replicas=2 --replica-assignment 1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2

 /var/lib/kafka/bin/kafka-topics.sh --bootstrap-server platformkafka1:9092,platformkafka2:9092,platformkafka3:9092,platformkafka4:9092,platformkafka5:9092,platformkafka6:9092 --create --topic $ENV.eur1.$topic.e.one --config compression.type=snappy --config retention.ms=86400000 --config retention.ms=86400000 --config min.insync.replicas=2 --replica-assignment 1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2

 /var/lib/kafka/bin/kafka-topics.sh --bootstrap-server platformkafka1:9092,platformkafka2:9092,platformkafka3:9092,platformkafka4:9092,platformkafka5:9092,platformkafka6:9092 --create --topic $ENV.eur2.$topic --config compression.type=snappy --config retention.ms=86400000 --config retention.ms=86400000 --config min.insync.replicas=2 --replica-assignment 4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5

 /var/lib/kafka/bin/kafka-topics.sh --bootstrap-server platformkafka1:9092,platformkafka2:9092,platformkafka3:9092,platformkafka4:9092,platformkafka5:9092,platformkafka6:9092 --create --topic $ENV.eur2.$topic.one --config compression.type=snappy --config retention.ms=86400000 --config retention.ms=86400000 --config min.insync.replicas=2 --replica-assignment 4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5

 /var/lib/kafka/bin/kafka-topics.sh --bootstrap-server platformkafka1:9092,platformkafka2:9092,platformkafka3:9092,platformkafka4:9092,platformkafka5:9092,platformkafka6:9092 --create --topic $ENV.eur2.$topic.e.one --config compression.type=snappy --config retention.ms=86400000 --config retention.ms=86400000 --config min.insync.replicas=2 --replica-assignment 4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5,4:5:6,5:6:4,6:4:5

done < local_rack_topic_list.txt