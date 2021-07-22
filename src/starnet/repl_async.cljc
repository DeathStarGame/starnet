(ns starnet.common.pad.async1
  (:require
   [clojure.core.async :as a :refer [<! >! <!! timeout chan alt! go
                                     >!! <!! alt!! alts! alts!! take! put!
                                     thread pub sub]]
   ))

; https://github.com/clojure/core.async/wiki/Pub-Sub

