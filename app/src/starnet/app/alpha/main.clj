(ns starnet.app.alpha.main
  (:require
   [clojure.core.async :as a :refer [<! >! <!! timeout chan alt! go
                                     >!! <!! alt!! alts! alts!! take! put!
                                     thread pub sub sliding-buffer mix admix unmix]]
   [clojure.set :refer [subset?]]
   [starnet.app.alpha.aux.nrepl :refer [start-nrepl-server]]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]

   [starnet.app.alpha.aux.serdes]

   [starnet.common.alpha.spec]
   [starnet.app.alpha.spec]
   [starnet.common.pad.all]

   [starnet.app.alpha.repl]
   [starnet.app.alpha.tests]
   [starnet.app.alpha.crux]
   [starnet.app.alpha.http]
   [starnet.app.crux-samples.core]

   [starnet.app.alpha.streams :refer [create-topics-async list-topics
                                      delete-topics produce-event create-kvstore
                                      future-call-consumer read-store
                                      send-event create-kstreams-game create-kstreams-access]]
   [starnet.app.alpha.http  :as app-http]
   [starnet.app.alpha.crux :as app-crux]
   [crux.api :as crux])
  (:import
   org.apache.kafka.clients.producer.KafkaProducer))

(defn env-optimized?
  []
  (let [appenv (read-string (System/getenv "appenv"))]
    (:optimized appenv)))

(declare  proc-main proc-http-server proc-nrepl
          proc-derived-1  proc-kstreams proc-log proc-access-store
          proc-cruxdb proc-kproducer proc-nrepl-server )

(def ch-main (chan 1))
(def ch-sys (chan (sliding-buffer 10)))
(def pub-sys (pub ch-sys first (fn [_] (sliding-buffer 10))))
(def a-derived-1 (atom {}))
(def ch-db (chan 10))
(def ch-kproducer (chan 10))
(def ch-access-store (chan 10))
(def ch-kstreams-states (chan (sliding-buffer 10)))
(def pub-kstreams-states (pub ch-kstreams-states first (fn [_] (sliding-buffer 10))))
(def mix-kstreams-states (a/mix ch-kstreams-states))

(defn -main  [& args]
  (when-not (env-optimized?)
    (stest/instrument)
    (s/check-asserts true))
  (when (env-optimized?)
    (alter-var-root #'clojure.test/*load-tests* (fn [_] false)))
  (put! ch-main :start)
  (<!! (proc-main ch-main)))

(defn proc-main
  [ch-main]
  (go (loop []
        (when-let [vl (<! ch-main)]
          (condp = vl
            :start (do
                     (proc-derived-1  pub-sys a-derived-1)
                     (proc-nrepl-server pub-sys)
                     (proc-http-server pub-sys)
                     (proc-cruxdb pub-sys ch-db)
                     (proc-kproducer pub-sys ch-kproducer)
                     (proc-kstreams pub-sys ch-sys mix-kstreams-states)
                     (proc-access-store  pub-sys ch-sys ch-access-store ch-kproducer  pub-kstreams-states)
                     (put! ch-sys [:nrepl-server :start])
                     #_(put! ch-sys [:kstreams [:start {:create-fn
                                                        'starnet.app.alpha.streams/create-kstreams-access
                                                        :repl-only-key :kstreams-access}]])
                     #_(put! ch-sys [:kstreams [:start {:create-fn
                                                        'starnet.app.alpha.streams/create-kstreams-game
                                                        :repl-only-key :kstreams-game}]])
                     #_(put! ch-sys [:kproducer :open])
                     #_(put! ch-sys [:cruxdb :start])
                     #_(put! ch-sys [:http-server :start]))
            :exit (System/exit 0)))
        (recur))
      (println "closing proc-main")))

(comment

  (put! ch-sys [:http-server :start])

  (put! ch-sys [:cruxdb :start])
  (put! ch-sys [:cruxdb :close])

  (stest/unstrument)

  (put! ch-main :exit)
  ;;
  )

(defn proc-nrepl-server
  [pub-sys]
  (let [c (chan 1)]
    (sub pub-sys :nrepl-server c)
    (go (loop [server nil]
          (if-let [[_ v] (<! c)]
            (condp = v
              :start (let [sr (start-nrepl-server "0.0.0.0" 7788)]
                       (recur sr)))
            (recur server)))
        (println "closing proc-nrepl-server"))))



(defn proc-http-server
  [pub-sys]
  (let [c (chan 1)]
    (sub pub-sys :http-server c)
    (go (loop [server nil]
          (when-let [[_ v] (<! c)]
            (condp = v
              :start (let [sr (app-http/start-dev [pub-sys ch-sys])]
                       (recur sr))
              :stop (recur server))))
        (println "closing proc-http-server"))))

(defn proc-log
  [pub-sys]
  (let [c (chan 1)]
    (sub pub-sys :log c)
    (go (loop []
          (if-let [[_ s] (<! c)]
            (println (str "; " s))
            (recur)))
        (println "closing proc-http-server"))))



(def crux-conf {:crux.node/topology '[crux.kafka/topology
                                      crux.kv.rocksdb/kv-store]
                :crux.kafka/bootstrap-servers "broker1:9092"
                :crux.kafka/tx-topic "crux-transaction-log"
                :crux.kafka/doc-topic "crux-docs"
                :crux.kafka/create-topics true
                :crux.kafka/doc-partitions 1
                :crux.kafka/replication-factor (short 1)
                :crux.kv/db-dir "/ctx/data/crux"
                :crux.kv/sync? false
                :crux.kv/check-and-store-index-version true})

(defn proc-cruxdb
  [pub-sys ch-db]
  (let [c (chan 1)]
    (sub pub-sys :cruxdb c)
    (go (loop [node nil]
          (if-let [[vl port] (alts! (if node [c ch-db] [c]))] ; add check if node is valid
            (condp = port
              c (condp = (second vl)
                  :start (let [n (crux/start-node crux-conf)]
                           (alter-var-root #'app-crux/node (constantly n)) ; for dev purposes
                           (println "; crux node started")
                           (recur n))
                  :close (do
                           (.close node)
                           (alter-var-root #'app-crux/node (constantly nil)) ; for dev purposes
                           (println "; crux node closed")
                           (recur nil)))
              ch-db (let [[f args cout] vl]
                      (go
                        (let [x (f args)] ; db call here
                          (>! cout x) ; convey data
                          ))
                      (recur node))
              )))
        (println "closing proc-cruxdb"))))

(def kprops-producer {"bootstrap.servers" "broker1:9092"
                      "auto.commit.enable" "true"
                      "key.serializer" "starnet.app.alpha.aux.serdes.TransitJsonSerializer"
                      "value.serializer" "starnet.app.alpha.aux.serdes.TransitJsonSerializer"})

(defn proc-kproducer
  [pub-sys ch-kproducer]
  (let [c (chan 1)]
    (sub pub-sys :kproducer c)
    (go (loop [kproducer nil]
          (if-let [[vl port] (alts! (if kproducer [c ch-kproducer] [c]))]
            (condp = port
              c (condp = (second vl)
                  :open (let [kp (KafkaProducer. kprops-producer)]
                          (println "; kprodcuer created")
                          (recur kp))
                  :close (do
                           (.close kproducer)
                           (println "; kproducer closed")
                           (recur nil)))
              ch-kproducer (let [[args cout] vl]
                             (>! cout (apply send-event kproducer args)) ; may deref future
                             (recur kproducer))))
          ))))

(defn proc-access-store
  [pub-sys ch-sys ch-access-store ch-kproducer pub-kstreams-states]
  (let [csys (chan 1)
        cstates (chan 1)
        store-name "alpha.access.streams"]
    (sub pub-sys :kstreams csys)
    (sub pub-kstreams-states store-name cstates)
    (go (loop [store nil]
          (if-let [[vl port] (alts! (if store [cstates ch-access-store] [cstates]))]
            (condp = port
              cstates (let [[appid [running? nw old kstreams]] vl]
                        (cond
                          (some? running?) (let [s (create-kvstore kstreams store-name)]
                                             (recur s))
                          (not running?) (when store
                                           (do (.close store)
                                               (recur nil)))
                          :else (recur nil)))
              ch-access-store (let [[op token cout] vl]
                                (condp = op
                                  :get (do (>! cout (.get token store))
                                           (recur store))
                                  (recur store)))))))))

(def kprops {"bootstrap.servers" "broker1:9092"})

(def ktopics ["alpha.token"
              "alpha.access.changes"
              "alpha.game"
              "alpha.game.changes"])

(comment

  (list-topics {:props kprops})
  (delete-topics {:props kprops :names ktopics})
  
  ;;
  )

; not used in the system, for repl purposes only
(def ^:private a-kstreams (atom {}))

(defn proc-kstreams
  [pub-sys ch-sys mix-kstreams-states]
  (let [c (chan 1)]
    (sub pub-sys :kstreams c)
    (go (loop [app nil]
          (if-let [[t [k args]] (<! c)]
            (do
              (if-not (subset? (set ktopics) (list-topics {:props kprops}))
                (<! (create-topics-async kprops ktopics)))
              (condp = k
                :start (let [{:keys [create-fn repl-only-key]} args
                             a ((resolve create-fn))]
                         (swap! a-kstreams assoc repl-only-key a) ; for repl purposes
                         (.start (:kstreams app))
                         (a/admix mix-kstreams-states (:ch-state a))
                         (a/admix mix-kstreams-states (:ch-running a))
                         (recur a))
                :close (do (when app
                             (.close (:kstreams app))
                             (a/unmix mix-kstreams-states (:ch-state app))
                             (a/unmix mix-kstreams-states (:ch-running app)))
                           (recur app))
                :cleanup (do (.cleanUp (:kstreams app))
                             (recur app))
                (recur app)))
            ))
        (println (str "proc-kstreams exiting")))
    c))

(defn proc-derived-1
  [pub-sys derived]
  (let [c (chan 1)]
    (sub pub-sys :kv c)
    (go (loop []
          (when-let [[t [k v]] (<! c)]
            (do
              (swap! derived assoc k v)))
          (recur))
        (println "proc-view exiting"))
    c))


