(ns starnet.impl
  (:require
   [clojure.repl :refer [doc]]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   (:import
    java.util.Date))

(comment
  
  (type (make-inst))
  (inst? (make-inst))
  
  ;;
  )

(defn str->int [s]
  (java.lang.Integer/parseInt s))

(defn rand-uuid []
  (java.util.UUID/randomUUID))

(defn with-gen-cyclic
  "Same as s/with-gen, but gen-fn takes  [spec] as argument.
   Caution: may be wrong, naive and design-breaking."
  [spec gen-fn]
  (let [s spec]
    (s/with-gen s (fn [] (gen-fn s)))))

(defn with-gen-fmap
  "Similar to s/with-gen. 
   Passes fmap-fn to gen/fmap.
   fmap-fn is a mapper, takes [generated-value]."
  [spec fmap-fn]
  (let [s spec]
    (with-gen-cyclic s
      #(gen/fmap fmap-fn (s/gen %)))))

(defn make-inst
  "Returns a damn inst"
  []
  (java.util.Date.))
