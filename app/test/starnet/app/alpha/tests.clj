(ns starnet.app.alpha.tests
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test :refer [is run-all-tests testing deftest run-tests] :as t]

   
   [starnet.common.alpha.spec]
   [starnet.app.alpha.spec]

   [starnet.common.sample-tests]
   [starnet.app.sample-tests]
   
   [starnet.common.alpha.tests]
   [starnet.app.alpha.streams-test]
   [starnet.app.alpha.spec-test]
   [starnet.app.alpha.http-test]
   [starnet.common.alpha.core :refer [rand-uuid]]))

(defn -main []
  (run-all-tests #"starnet.app.+tests?$|starnet.common.+tests?$")
  (System/exit 0))

(comment

  (stest/check)
  (tc/quick-check)

  (run-tests)
  (run-tests
   'starnet.common.sample-tests
   'starnet.app.sample-tests)

  ;;
  )


