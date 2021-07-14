(ns starnet.alpha.tests
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test :refer [is run-all-tests testing deftest run-tests] :as t]

   [starnet.alpha.core.spec]
   
   [starnet.alpha.core.tests]
   [starnet.alpha.core.tmp :refer [rand-uuid]]))

(defn start []
  (run-all-tests #"starnet.ui.+tests?$|starnet.common.+tests?$"))

(defn stop [done]
  (done))

(defn ^:export init []
  (start))

(comment

  (stest/check)
  (tc/quick-check)

  (run-tests)
  (run-tests
   'starnet.common.sample-tests
   'starnet.ui.sample-tests)

  ;;
  )

(deftest common-deps
  (testing "generating random uuid via reader conditionals in .cljc"
    (is (uuid? (rand-uuid)))))
