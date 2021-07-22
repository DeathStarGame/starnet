(ns starnet.app.kafka-samples.transit-example
  (:require
   [clojure.pprint :as pp]
   [starnet.app.alpha.aux.serdes])
  (:import
   starnet.app.alpha.aux.serdes.TransitJsonSerializer
   starnet.app.alpha.aux.serdes.TransitJsonDeserializer
   starnet.app.alpha.aux.serdes.TransitJsonSerde

   org.apache.kafka.common.serialization.Serdes
   org.apache.kafka.streams.KafkaStreams
   org.apache.kafka.streams.StreamsBuilder
   org.apache.kafka.streams.StreamsConfig
   org.apache.kafka.streams.Topology
   org.apache.kafka.streams.kstream.KStream
   org.apache.kafka.streams.kstream.KTable
   java.util.Properties
   java.util.concurrent.CountDownLatch
   org.apache.kafka.clients.admin.KafkaAdminClient
   org.apache.kafka.clients.admin.NewTopic
   org.apache.kafka.clients.consumer.KafkaConsumer
   org.apache.kafka.clients.producer.KafkaProducer
   org.apache.kafka.clients.producer.ProducerRecord
   org.apache.kafka.streams.kstream.ValueMapper
   org.apache.kafka.streams.kstream.KeyValueMapper
   org.apache.kafka.streams.kstream.Materialized
   org.apache.kafka.streams.kstream.Produced
   java.util.ArrayList
   java.util.Locale
   java.util.Arrays))

(defn create-topic
  [{:keys [conf
           name
           num-partitions
           replication-factor] :as opts}]
  (let [client (KafkaAdminClient/create conf)
        topics (java.util.ArrayList.
                [(NewTopic. name num-partitions (short replication-factor))])]
    (.createTopics client topics)))

(defn delete-topics
  [{:keys [conf
           names] :as opts}]
  (let [client  (KafkaAdminClient/create conf)]
    (.deleteTopics client (java.util.ArrayList. names))))

(defn list-topics
  [{:keys [conf] :as opts}]
  (let [client (KafkaAdminClient/create conf)
        kfu (.listTopics client)]
    (.. kfu (names) (get))))

(defn add-shutdown-hook
  [streams latch]
  (-> (Runtime/getRuntime)
      (.addShutdownHook (proxy
                         [Thread]
                         ["streams-shutdown-hook"]
                          (run []
                            (.println (System/out) "--closing stream")
                            (.close streams)
                            (.countDown latch))))))

(def base-conf {"bootstrap.servers" "broker1:9092"})

(comment

  (create-topic {:conf base-conf
                 :name "transit-input"
                 :num-partitions 1
                 :replication-factor 1})

  (create-topic {:conf base-conf
                 :name "transit-output"
                 :num-partitions 1
                 :replication-factor 1})

  (list-topics {:conf base-conf})

  (delete-topics {:conf base-conf
                  :names ["transit-input" "transit-output"]})

  (def topology
    (let [builder (StreamsBuilder.)]
      (-> builder
          (.stream "transit-input")
          (.to "transit-output"))
      (.build builder)))

  (println (.describe topology))

  (def streams (KafkaStreams.
                topology
                (doto (Properties.)
                  (.putAll {"application.id" "transit-example"
                            "bootstrap.servers" "broker1:9092"
                            "default.key.serde" (.. Serdes String getClass)
                            "default.value.serde" "starnet.app.alpha.aux.serdes.TransitJsonSerde"}))))

  (def latch (CountDownLatch. 1))

  (add-shutdown-hook streams latch)

  (def fu-streams
    (future-call
     (fn []
       (.start streams)
       #_(.await latch) ; halts
       )))

  (future-cancel fu-streams)
  (.close streams)

  (def fu-consumer
    (future-call (fn []
                   (let [consumer (KafkaConsumer.
                                   {"bootstrap.servers" "broker1:9092"
                                    "auto.offset.reset" "earliest"
                                    "auto.commit.enable" "false"
                                    "group.id" (.toString (java.util.UUID/randomUUID))
                                    "consumer.timeout.ms" "5000"
                                    "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
                                    "value.deserializer" "starnet.app.alpha.aux.serdes.TransitJsonDeserializer"})]
                     (.subscribe consumer (Arrays/asList (object-array ["transit-output"])))
                     (while true
                       (let [records (.poll consumer 1000)]
                         (.println System/out (str "polling records:" (java.time.LocalTime/now)))
                         (doseq [rec records]
                           (prn (str (.key rec) " : " (.value rec))))))))))

  (future-cancel fu-consumer)

  (def producer (KafkaProducer.
                 {"bootstrap.servers" "broker1:9092"
                  "auto.commit.enable" "true"
                  "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
                  "value.serializer" "starnet.app.alpha.aux.serdes.TransitJsonSerializer"}))

  (.send producer (ProducerRecord.
                   "transit-input"
                   (.toString (java.util.UUID/randomUUID)) {:a 123}))

  ;
  )

