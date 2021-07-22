(ns starnet.main
  (:require
   [clojure.core.async :as a :refer [<! >! <!! timeout chan alt! go
                                     >!! <!! alt!! alts! alts!! take! put! mult tap untap
                                     thread pub sub sliding-buffer mix admix unmix]]
   [clojure.set :refer [subset?]]
   [starnet.nrepl :refer [start-nrepl-server]]
   [clojure.spec.alpha :as s]

   [starnet.common.alpha.spec]
   [starnet.spec]
   [starnet.common.pad.all]

   [starnet.repl]
   #_[starnet.app.pad.all]
   [starnet.common.pad.async2]
   [starnet.common.pad.datascript1]
   [starnet.game]
   [starnet.common.pad.transducers1]

   [starnet.core :as appcore]
   [clojure.java.shell :refer [sh]]
   [clojure.java.io :as io]))


(declare  proc-main
          proc-derived-1 proc-log
          proc-nrepl-server)

(def channels (let [ch-proc-main (chan 1)
                    ch-sys (chan (sliding-buffer 10))
                    pb-sys (pub ch-sys :ch/topic (fn [_] (sliding-buffer 10)))]
                {:ch-proc-main ch-proc-main
                 :ch-sys ch-sys
                 :pb-sys pb-sys}))

(defn -main  [& args]
  (put! (channels :ch-proc-main) {:proc/op :start})
  (<!! (proc-main (select-keys channels [:ch-proc-main :ch-sys]))))

(defn proc-main
  [{:keys [ch-proc-main ch-sys]}]
  (go
    (loop []
      (when-let [{op :proc/op} (<! ch-proc-main)]
        (condp = op
          :start
          (do
            (<! (proc-keys channels))
            (proc-nrepl-server (select-keys channels [:pb-sys]))
            (put! ch-sys {:ch/topic :nrepl-server :proc/op :start})
            (recur))
          :exit (System/exit 0))))
    (println "closing proc-main")))

(comment

  (put! (channels :ch-main) {:proc/op :start})

  ;;
  )

(defn proc-nrepl-server
  [{:keys [pb-sys]}]
  (let [c (chan 1)]
    (sub pb-sys :nrepl-server c)
    (go (loop [server nil]
          (if-let [{op :proc/op} (<! c)]
            (condp = op
              :start (let [sr (start-nrepl-server "0.0.0.0" 7788)]
                       (recur sr)))))
        (println "closing proc-nrepl-server"))))

(defn proc-log
  [{:keys [pb-sys]}]
  (let [c (chan 1)]
    (sub pb-sys :log c)
    (go (loop []
          (if-let [{s :log/str} (<! c)]
            (println (str "; " s))
            (recur)))
        (println "closing proc-log"))))

(defn proc-derived-1
  [{:keys [pb-sys]} derived]
  (let [c (chan 1)]
    (sub pb-sys :kv c)
    (go (loop []
          (when-let [{:keys [k v]} (<! c)]
            (do
              (swap! derived assoc k v))
            (recur)))
        (println "proc-view exiting"))
    c))