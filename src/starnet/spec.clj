(ns starnet.spec
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.spec.test.alpha :as stest]))

(derive java.util.Map :isa/map)
(derive java.util.Set :isa/set)
(derive java.util.UUID :isa/uuid)
(derive clojure.lang.Keyword :isa/keyword)