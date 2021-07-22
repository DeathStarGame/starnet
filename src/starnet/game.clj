(ns starnet.game
  (:require
   [clojure.repl :refer [doc]]
   [clojure.core.async :as a :refer [<! >!  timeout chan alt! go
                                     alts!  take! put! mult tap untap
                                     pub sub sliding-buffer mix admix unmix]]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   
   [starnet.common.alpha.spec]
   [starnet.impl :refer [make-inst with-gen-fmap]]
   [clojure.test :as test :refer [is are run-all-tests testing deftest run-tests]]))


(defn make-state
  ([]
   (make-state {}))
  ([opts]
   (merge {:g/uuid (gen/generate gen/uuid)
           :g/events []}
          (select-keys opts [:g/events :g/uuid]))))

(defn make-channels
  []
  (let [ch-game (chan 10)
        ch-game-events (chan 100)
        ch-inputs (chan 100)
        ch-worker (chan 100)]
    {:ch-game ch-game
     :ch-game-events ch-game-events
     :ch-inputs ch-inputs
     :ch-worker ch-worker}))



