(ns starnet.alpha.core.game-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test :as test :refer [is testing run-tests deftest]]))

#_(deftest make-state-tests
    (testing "generates valid :g/state"
      (is (s/valid? :g/state
                    (make-state)))))

#_(deftest all-specchecks
  (testing "running spec.test/check via stest/enumerate-namespace"
    (let [summary (-> #?(:clj (stest/enumerate-namespace 'starnet.alpha.core.game)
                         :cljs 'starnet.alpha.core.game)
                      (stest/check {:clojure.spec.test.check/opts {:num-tests 10}})
                      (stest/summarize-results))]
      (is (not (contains? summary :check-failed))))))

#_(deftest make-state-speccheck
  (testing "running spec.test/check"
    (let [summary (-> (stest/check `make-state
                                   {:clojure.spec.test.check/opts {:num-tests 10}})
                      (stest/summarize-results))]
      (is (not (contains? summary :check-failed))))))

#_(deftest next-state-tests
  (testing "event :ev.g.u/create"
    (is (s/valid? :g/state (next-state (s/conform :g/state (make-state))
                                                 (gen/generate gen/uuid)
                                                 {:ev/type :ev.g.u/create
                                                  :u/uuid  (gen/generate gen/uuid)}))))
  (testing "random :g/state and :ev.g.u/create event "
    (is (s/valid? :g/state (next-state (gen/generate (s/gen :g/state))
                                                 (gen/generate gen/uuid)
                                                 (gen/generate (s/gen :ev.g.u/create)))))))
(comment

  (run-tests)
  (all-specchecks)
  (make-state-speccheck)

  (list (reduce #(assoc %1 (keyword (str %2)) %2) {} (range 0 100)))

  (next-state-tests)
  (make-state-tests)

  ;;
  )