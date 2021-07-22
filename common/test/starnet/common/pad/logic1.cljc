(ns starnet.common.pad.logic1
  (:refer-clojure :exclude [==])
  (:require
   [clojure.set :refer [subset?]]
   [clojure.walk :as walk]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :refer [defspec]]

   [clojure.core.logic.nominal :exclude [fresh hash] :as nom]
   [clojure.core.logic :exclude [is] :refer :all :as l]
   [clojure.core.logic.protocols :refer :all]
   [clojure.core.logic.pldb :as pldb :refer [db with-db db-rel db-fact]]
   [clojure.core.logic.fd  :as fd]
   [clojure.core.logic.unifier :as u]
   [clojure.core.logic.arithmetic :as la]
   [clojure.test :as test :refer [is are run-all-tests testing deftest run-tests]])
  #?(:clj (:import clojure.core.logic.fd.IntervalFD)))

(comment
  
  (run-tests)

  ;;
  )

; https://github.com/clojure/core.logic
; https://github.com/clojure/core.logic/wiki
; https://github.com/clojure/core.logic/wiki/Examples
; https://github.com/clojure/core.logic/wiki/Features
; https://github.com/clojure/core.logic/blob/master/src/test/clojure/clojure/core/logic/tests.clj
; https://github.com/clojure/core.logic/blob/master/src/test/cljs/cljs/core/logic/tests.cljs

(deftest logic-tests
  (testing "mixed examples"
    (are [x y] (= x y)
      (run* [q] (== q true)) '(true)

      (run* [q]
            (== q {:a 1 :b 2})) '({:a 1 :b 2})
      (run* [q]
            (== {:a q :b 2} {:a 1 :b 2})) '(1)
      (run* [q]
            (== 1 q)) '(1)
      (read-string (pr-str (run* [q]
                                 (== 1 q)))) '(1)
      (run* [q]
            (== q '(1 2 3))) '((1 2 3))
      (run* [q]
            (== q 1)
            (== q 2)) '()))
  (testing "conde examples"
    (are [x y] (= x y)
      (run* [q]
            (conde
             [succeed]))  '(_0)
      (run* [q]
            (conde
             [succeed (== q 1)])) '(1)
      (run* [q]
            (conde
             [(== q 1)]
             [(== q 2)])) '(1 2)))
  (testing "conso examples"
    (are [x y] (= x y)
      (run* [q]
            (conso 1 [2 3] q)) '((1 2 3))
      (run* [q]
            (conso 1 q [1 2 3])) '((2 3))

      (run* [q]
            (conso q [2 3] [1 2 3])) '(1)
      (run* [q]
            (conso 1 [2 q] [1 2 3])) '(3)
      (run* [q]
            (resto [1 2 3 4] q)) '((2 3 4))))
  (testing "membero examples"
    (are [x y] (= x y)
      (run* [q]
            (membero 7 [1 3 8 q])) '(7)
      (run* [q] (membero q '(:cat :dog :bird))) '(:cat :dog :bird)
      (run* [q]
            (and*
             [(membero q [:t1 :t2 :t3])
              (membero q [:t2 :t3])
              (membero q [:t1 :t2])])) '(:t2)
      (let [a {:type :some-entity
               :tags #{:t1 :t2 :t3}
               :logic-alias :a}
            b {:type :some-entity
               :tags #{:t2 :t3}
               :logic-alias :a}
            c {:type :some-entity
               :tags #{:t1 :t2}
               :logic-alias :a}]
        (run* [q]
              (->> [a b c]
                   (map :tags)
                   (map vec)
                   (map (fn [xs]
                          xs
                          (membero q xs)))
                   (and*)))) '(:t2)
      (run* [q]
            (fresh [a]
                   (membero a [1 2 3])
                   (membero q [3 4 5])
                   (== a q))) '(3)))
  (testing "pldb examples"
    (let [_ (db-rel person p)
          _ (db-rel fruit f)
          _ (db-rel enjoys ^:index p1 ^:index f1)
          _ (db-rel  citrus f)
          _ (db-rel  sweet f)

          facts1 (db
                  [person 'A]
                  [person 'B]
                  [person 'C]

                  [fruit 'mango]
                  [fruit 'banana]
                  [fruit 'persimmon]

                  [enjoys 'A 'mango]
                  [enjoys 'A 'banana]
                  [enjoys 'B 'banana]
                  [enjoys 'C 'persimmon])

          facts2 (-> facts1
                     (db-fact citrus 'mango)
                     (db-fact sweet 'banana)
                     (db-fact sweet 'persimmon))]
      (are [x y] (= x y)
        (with-db facts2
          (run* [q]
                (fresh [x y]
                       (citrus y)
                       (enjoys x y)
                       (== q [x y])))) '([A mango])
        (with-db facts2
          (run* [q]
                (fresh [x y]
                       (sweet y)
                       (enjoys x y)
                       (== q [x y])))) '([A banana] [B banana] [C persimmon]))))
  (testing "logic.unifier/unify"
    (is (= (u/unify ['(?x ?y ?z) '(1 2 ?y)]) '(1 2 2))))
  (testing "contraint logic programming (CLP tree)"
    (are [x y] (= x y)
      (run* [q]
            (!= q 1)) '((_0 :- (!= (_0 1))))
      (run* [q]
            (fresh [x y]
                   (!= [1 x] [y 2])
                   (== q [x y]))) '(([_0 _1] :- (!= (_1 1) (_0 2))))))
  (testing "contraint logic programming (CLP finite domains)"
    (are [x y] (= x y)
      (run* [q]
            (fd/in q (fd/interval 1 5))) '(1 2 3 4 5)
      (run* [q]
            (fresh [x y]
                   (fd/in x y (fd/interval 1 10))
                   (fd/+ x y 10)
                   (== q [x y]))) '([1 9] [2 8] [3 7] [4 6] [5 5] [6 4] [7 3] [8 2] [9 1])
      (run* [q]
            (fresh [x y]
                   (fd/in x y (fd/interval 0 9))
                   (fd/eq
                    (= (+ x y) 9)
                    (= (+ (* x 2) (* y 4)) 24))
                   (== q [x y]))) '([6 3])
      (run* [q]
            (fresh [x y]
                   (fd/in x y (fd/interval 1 10))
                   (fd/+ x y 10)
                   (== q [x y]))) '([1 9] [2 8] [3 7] [4 6] [5 5] [6 4] [7 3] [8 2] [9 1])
      (run* [q]
            (fresh [x y]
                   (fd/in x y (fd/interval 1 10))
                   (fd/+ x y 10)
                   (fd/distinct [x y])
                   (== q [x y]))) '([1 9] [2 8] [3 7] [4 6] [6 4] [7 3] [8 2] [9 1])
      (fd/-intersection (fd/interval 0 20) (fd/interval 10 30)) (fd/interval 10 20)))

  (testing "tabling"
    (let [_ (defne arco [x y]
              ([:a :b])
              ([:b :a])
              ([:b :d]))
          _ (def patho
              (tabled [x y]
                      (conde
                       [(arco x y)]
                       [(fresh [z]
                               (arco x z)
                               (patho z y))])))]
      (are [x y] (= x y)
        (run* [q] (patho :a q)) '(:b :a :d))))
  
  
  )


(comment

  ; https://github.com/clojure/core.logic/wiki/Examples#a-classic-ai-program

  (defne moveo [before action after]
    ([[:middle :onbox :middle :hasnot]
      :grasp
      [:middle :onbox :middle :has]])
    ([[pos :onfloor pos has?]
      :climb
      [pos :onbox pos has?]])
    ([[pos1 :onfloor pos1 has?]
      :push
      [pos2 :onfloor pos2 has?]])
    ([[pos1 :onfloor box has?]
      :walk
      [pos2 :onfloor box has?]]))

  (def start-state [:atdoor :onfloor :atwindow :hasnot])
  (def end-state [:atwindow :onbox :atwindow :has])

  (defne cangeto [state out]
    ([[_ _ _ :has] out]
     (== out true))
    ([_ _]
     (fresh [action next]
            (moveo state action next)
            (cangeto next out))))

  (run 1 [q]
       (cangeto start-state q)) ; (true)

  ; https://github.com/clojure/core.logic/wiki/Examples#sudoku

  (defn get-square [rows x y]
    (for [x (range x (+ x 3))
          y (range y (+ y 3))]
      (get-in rows [x y])))

  (defn bind [var hint]
    (if-not (zero? hint)
      (== var hint)
      succeed))

  (defn bind-all [vars hints]
    (and* (map bind vars hints)))

  (defn sudokufd [hints]
    (let [vars (repeatedly 81 lvar)
          rows (->> vars (partition 9) (map vec) (into []))
          cols (apply map vector rows)
          sqs  (for [x (range 0 9 3)
                     y (range 0 9 3)]
                 (get-square rows x y))]
      (run 1 [q]
           (== q vars)
           (everyg #(fd/in % (fd/domain 1 2 3 4 5 6 7 8 9)) vars)
           (bind-all vars hints)
           (everyg fd/distinct rows)
           (everyg fd/distinct cols)
           (everyg fd/distinct sqs))))

  (def hints
    [2 0 7 0 1 0 5 0 8
     0 0 0 6 7 8 0 0 0
     8 0 0 0 0 0 0 0 6
     0 7 0 9 0 6 0 5 0
     4 9 0 0 0 0 0 1 3
     0 3 0 4 0 1 0 2 0
     5 0 0 0 0 0 0 0 1
     0 0 0 2 9 4 0 0 0
     3 0 6 0 8 0 4 0 9])

  (doseq [x (partition 9 (first (sudokufd hints)))]
    (println x))

  ;=>
; ((2 6 7 3 1 9 5 4 8
;   9 5 4 6 7 8 1 3 2 
;   8 1 3 5 4 2 7 9 6 
;   1 7 2 9 3 6 8 5 4
;   4 9 5 8 2 7 6 1 3
;   6 3 8 4 5 1 9 2 7
;   5 4 9 7 6 3 2 8 1
;   7 8 1 2 9 4 3 6 5
;   3 2 6 1 8 5 4 7 9))

  ; https://github.com/clojure/core.logic/wiki/Examples#a-type-inferencer-for-the-simply-typed-lambda-calculus


  (defna findo [x l o]
    ([_ [[y :- o] . _] _]
     (project [x y] (== (= x y) true)))
    ([_ [_ . c] _] (findo x c o)))

  (defn typedo [c x t]
    (conda
     [(lvaro x) (findo x c t)]
     [(matche [c x t]
              ([_ [[y] :>> a] [s :> t]]
               (fresh [l]
                      (conso [y :- s] c l)
                      (typedo l a t)))
              ([_ [:apply a b] _]
               (fresh [s]
                      (typedo c a [s :> t])
                      (typedo c b s))))]))


  ;; ([_.0 :> _.1])
  (run* [q]
        (fresh [f g a b t]
               (typedo [[f :- a] [g :- b]] [:apply f g] t)
               (== q a)))

  ;; ([:int :> _.0])
  (run* [q]
        (fresh [f g a t]
               (typedo [[f :- a] [g :- :int]] [:apply f g] t)
               (== q a)))

  ;; (:int)
  (run* [q]
        (fresh [f g a t]
               (typedo [[f :- [:int :> :float]] [g :- a]]
                       [:apply f g] t)
               (== q a)))

  ;; ()
  (run* [t]
        (fresh [f a b]
               (typedo [f :- a] [:apply f f] t)))

  ;; ([_.0 :> [[_.0 :> _.1] :> _.1]])
  (run* [t]
        (fresh [x y]
               (typedo []
                       [[x] :>> [[y] :>> [:apply y x]]] t)))


  ;;
  )


(comment

  fd/domain
  fd/interval
  fd/dom
  fd/in

  (fd/-intersection (fd/interval 1 6) 1)
  (fd/to-vals (fd/-intersection (fd/interval 1 6) (fd/interval 5 9)))

  ; https://github.com/clojure/core.logic/blob/master/src/test/clojure/clojure/core/logic/tests.clj#L2205
  (run* [q]
        (fd/dom q (fd/interval 1 100))
        (fd/dom q (fd/interval 30 60))
        (fd/dom q (fd/interval 50 55))
        #_(== q 51))

  ; https://github.com/clojure/core.logic/blob/master/src/test/clojure/clojure/core/logic/tests.clj#L2604
  (deftest test-distinct
    (is (= (into #{}
                 (run* [q]
                       (fresh [x y z]
                              (fd/in x y z (fd/interval 1 3))
                              (fd/distinct [x y z])
                              (== q [x y z]))))
           (into #{} '([1 2 3] [1 3 2] [2 1 3] [2 3 1] [3 1 2] [3 2 1])))))

  (deftest test-fd-<-1
    (is (= (into #{}
                 (run* [q]
                       (fresh [a b c]
                              (fd/in a b c (fd/interval 1 3))
                              (fd/< a b) (fd/< b c)
                              (== q [a b c]))))
           (into #{} '([1 2 3])))))

  (time (->
         (run* [q]
               (fresh [a b c]
                      (fd/in a b c (fd/interval 1 100))
                      (fd/< a b) (fd/< b c)
                      (== q [a b c])))
         (count)))


  (is (= (run* [q]
               (== q 1)
               (predc q number? `number?))
         '(1)))
  (is (= (run* [q]
               (predc q number? `number?)
               (== q "foo"))
         ()))

  (is (= (run* [q]
               (fresh [x]
                      (featurec x {:foo q})
                      (== x {:foo 1})))
         '(1)))

  (is (= (run* [q]
               (fresh [x]
                      (featurec x {:foo {:bar q}})
                      (== x {:foo {:bar 1}})))
         '(1)))

  (to-s [[:a 1] [:b 1]])

  (let [[a b c :as v] [1 2 3]]
    [[a b c] v])

  (= (let [[x y z c b a :as s] (map lvar '[x y z c b a])
           ss (to-s [[x 5] [y x] [z y] [c z] [b c] [a b]])]
       (walk ss a))
     5)

  (= (let [x  (lvar 'x)
           y  (lvar 'y)]
       (walk* (to-s [[x 5] [y x]]) `(~x ~y)))
     '(5 5))

  s#

  (= (u/unify ['(?x 2 . ?y) '(1 2 3 4 5)])
     '(1 2 3 4 5))

  (= (u/unify ['{:a [?b (?c [?d {:e ?e}])]} {:a [:b '(:c [:d {:e :e}])]}])
     {:a [:b '(:c [:d {:e :e}])]})

  (defnc evenc [x]
    (even? x))

  (defnc div3c [x]
    (zero? (mod x 3)))

  (deftest test-unifier-constraints-4
    (is (= (u/unify {:when {'#{?a ?b} [evenc div3c]}} ['{:a ?a :b ?b} {:a 6 :b 12}])
           {:a 6 :b 12}))
    (is (= (u/unify {:when {'#{?a ?b} [evenc div3c]}} ['{:a ?a :b ?b} {:a 2 :b 6}])
           nil)))

  (defnc complexc [a b]
    (and (even? a) (zero? (mod b 3))))

  (deftest test-unifier-constraints-5
    (is (= (u/unify {:when {'[?a ?b] complexc}} ['{:a ?a :b ?b} {:a 2 :b 3}])
           {:a 2 :b 3}))
    (is (= (u/unify {:when {'[?a ?b] complexc}} ['{:a ?a :b ?b} {:a 2 :b 4}])
           nil)))

  (u/unify {:as '{?x (?y ?z)}} ['?x '(1 2)])
  (u/unify {:as '{?x (?y ?z)}} ['(?x) '((1 2))])


  ;;Anonymous constraints
  (deftest test-unifier-anon-constraints-3 ;;One var
    (is (= (u/unify {:when {'?a (fnc [x] (even? x))}} ['{:a ?a} {:a 2}])
           {:a 2}))
    (is (= (u/unify {:when {'?a (fnc [x] (even? x))}} ['{:a ?a} {:a 1}])
           nil)))

  (deftest test-binding-map-constraints-1
    (is (= (u/unifier {:when {'?x evenc '?y div3c}} ['(?x ?y) '(2 6)])
           '{?x 2 ?y 6}))
    (is (= (u/unifier {:when {'?x div3c '? evenc}} ['(?x ?y) '(2 6)])
           nil))
    (is (= (u/unifier {:when {'[?x ?y] complexc}} ['(?x ?y) '(2 6)])
           '{?x 2 ?y 6}))
    (is (= (u/unifier {:when {'#{?x ?y} [evenc div3c]}} ['(?x ?y) '(6 12)])
           '{?x 6 ?y 12}))
    (is (= (u/unifier {:when {'#{?x ?y} [evenc div3c]}} ['(?x ?y) '(14 12)])
           nil)))

  (deftest test-binding-map-as-1
    (is (= (u/unifier {:as {'?x '(?y ?z)}} '[(?x) ((1 2))])
           '{?x (1 2) ?y 1 ?z 2})))

  (defnc dayc [day x]
    (if (= day :tuesday)
      (even? x)
      (odd? x)))

  (defnc greaterc
    [x v]
    (> x v))

  (run* [q]
        (fresh [a b c d]
               (fd/in a b c d (fd/interval 1 4))
               (fd/distinct [a b c d])
               (fd/< a b)
               (greaterc c 1)
               (dayc :tuesday d)
               (== q {:path [a b c d]
                      :entities {}})))

  predc
  conda
  project
  permuteo
  featurec
  nafc

  (defnc evenc
    [x]
    (> x 5))

  (defnc c-2
    [x]
    (< x 15))

  (defn rule-1
    [state dom]
    (fresh [a b]
           (fd/in a b dom)
           (c-1 a)
           (fd/< a b)
           (== state {:a a
                      :b b})))

  (defn rule-2
    [state dom]
    (fresh [a b]
           (fd/in a b dom)
           (c-2 b)
           (== state {:a a
                      :b b})))

  (defn rule-3
    [state dom]
    (fresh [a b]
           (fd/in a b dom)
           (fd/eq
            (= (* a 2) b))
           (== state {:a a
                      :b b})))

  (let [dom (fd/interval 0 10)]
    (run 10 [q]
         (rule-1 q dom)
         (rule-2 q dom)
         (rule-3 q dom)))


  (deftest test-naf-1
    (is (= (into #{}
                 (run* [q]
                       (membero q '(a b c))
                       (nafc == q 'b)))
           '#{a c}))
    (is (= (into #{}
                 (run* [q]
                       (nafc == q 'b)
                       (membero q '(a b c))))
           '#{a c})))

  (into #{}
        (run* [x y]
              (fd/in x y (fd/interval 1 5))
              (fd/< x y)
              (nafc fd/+ x y 5)))

  (deftest test-naf-5
    (is (= (run* [q]
                 (membero q '(:a :b :c :d))
                 (nafc membero q '(:a :b :c)))
           '(:d)))
    (is (= (run* [q]
                 (nafc membero q '(:a :b :c))
                 (membero q '(:a :b :c :d)))
           '(:d))))

  (run* [q]
        (fresh [a b]
               (== a 1)
               (== b 5)
               (project [a b]
                        (fd/in q (fd/interval a b)))))


  (def a (first (run* [q]
                      (fresh [a b]
                             (== a 1)
                             (== b 5)
                             (project [a b]
                                      (== q `(fd/interval ~a ~b)))))))
  (def b (eval a))


  ; #interval [[1 2] [5] [10 100]]

  (def x (doall (run* [q]
                      (fd/in q (fd/interval 1 1000000)))))

  (def y (doall (range 0 1000000)))

  ; possible for intervals to be first class primitives in logic?
  ; so the query above would return <interval 0 100000> instead of unifying/walking

  (type (fd/bounds (fd/interval 1 5)))

  (run* [q]
        (fresh [x]
               (l/or*
                [(fd/in x (fd/interval 1 10))
                 (fd/in x (fd/interval  20 30))])
               (== q x)))

  ;;
  )
  
(comment

  (= (fd/-intersection (fd/interval 0 20) (fd/interval 10 30)) (fd/interval 10 20))

  (= (into #{}
           (run* [q]
                 (fresh [x y z]
                        (fd/in x y z (fd/interval 1 10))
                        (fd/+ x y z)
                        (fd/< x y)
                        (== z 10)
                        (== q [x y z]))))
     (into #{} '([1 9 10] [2 8 10] [3 7 10] [4 6 10])))

  (= (into #{}
           (run* [q]
                 (fresh [x y z]
                        (fd/in x y z (fd/interval 1 3))
                        (fd/distinct [x y z])
                        (== q [x y z]))))
     (into #{} '([1 2 3] [1 3 2] [2 1 3] [2 3 1] [3 1 2] [3 2 1])))

  (run* [q]
        (fresh [x y z]
               (fd/in x y z (fd/interval 1 20))
               (fd/+ x y z)
               (fd/distinct [x y z])
               (== z 15)
               (== q [x y z])))

  (->>
   (run* [q]
         (fresh [x y]
                (fd/in x y (fd/interval 1 64))
                (== q [x y])))
   (count))

  (= (fd/-drop-before (fd/multi-interval 2 4) (fd/-lb 3))
     4)

  (= (fd/-keep-before (fd/multi-interval 2 4) (fd/-lb 3))
     2)

  (fd/-keep-before (fd/interval 0 100) (fd/-lb 50))
  (fd/-drop-before (fd/interval 0 100) (fd/-lb 50))
  (fd/-difference 1 2)

  (fd/multi-interval  (fd/interval 0 100) (fd/interval 10 30))
  (= (fd/-difference (fd/interval 0 100) (fd/interval 10 30))
     (fd/multi-interval  (fd/interval 0 9) (fd/interval 31 100)))
  (= (fd/-intersection (fd/interval 0 100) (fd/interval 10 30))
     (fd/interval 10 30))

  (def i (fd/interval 1 5))
  (.-lb i)
  (.-ub i)
  (fd/-lb i)
  (fd/-ub i)

  (def entities
    {:database {:energy {:op '- :min 30 :max 50}
                :knowledge {:op '+ :min 20 :max 30}
                :available [even?]}
     :fruit-tree {:energy {:op '+ :min 60 :max 80}
                  :vision {:op '+ :min 30 :max 50}
                  :available [#(not= 2)]}
     :update-center {:resolve {:op '+ :min 20 :max 50}
                     :vision {:op '+ :min 10 :max 50}
                     :interface {:op '+ :min 20 :max 60}
                     :energy {:op '- :min 20 :max 60}
                     :available [odd?]}
     :nanite-lab {:range {:op '+ :min 20 :max 80}
                  :fields {:op '+ :min 10 :max 30}
                  :energy {:op '- :min 20 :max 40}
                  :available [#{4 5 6 7} 'after-update-center]}
     :missiondroid {:range {:min 0 :max 100}
                    :fields {:min 0 :max 100}
                    :interface {:min 0 :max 100}}
     :cape {:energy {:min 0 :max 100 :min-start 50 :max-start 70}
            :resolve {:min 0 :max 100 :min-start 80 :max-start 90}
            :vision {:min 0 :max 100  :min-start 80 :max-start 100}
            :knowledge {:min 0 :max 100 :min-start 20 :max-start 70}}})

  (defn gen-cape
    []
    {:energy (gen/generate (gen/choose (get-in entities [:cape :energy :min-start])
                                       (get-in entities [:cape :energy :max-start])))
     :resolve (gen/generate (gen/choose (get-in entities [:cape :resolve :min-start])
                                        (get-in entities [:cape :resolve :max-start])))
     :vision (gen/generate (gen/choose (get-in entities [:cape :vision :min-start])
                                       (get-in entities [:cape :vision :max-start])))
     :knowledge (gen/generate (gen/choose (get-in entities [:cape :knowledge :min-start])
                                          (get-in entities [:cape :knowledge :max-start])))})

  (def c1 (gen-cape))

  (run 5 [q]
       (fresh [a b c d]
              (?== a 5)
              (fd/in a b c d (fd/interval 1 100))
              (fd/in b (fd/interval 20 80))
              (fd/in c (fd/interval 50 60))
              (fd/in d (fd/interval 10 30))
              #_(fd/eq
                 (= (- b c) 20)
                 (= (+ (* c 2) (* d 4)) 24))
              (?== q {:a a
                      :b b
                      :c c
                      :d d})))

  (run 10 [q]
       (fresh [a]
              (fd/in a (fd/interval 1 5))
              (?== q {:a a})))

    ;;
  )
  
  
  (defn dot-prod
    "Retruns the sum of products of corresponding els"
    [a b]
    (reduce + 0 (map * a b)))
  
  (defn vec-norm
    "Returns the  Euclidean norm (length or magnitude) of a vector.
   L2 norm"
    [a]
  ; (bigdec (Math/sqrt (dot-prod a a)))
    (Math/sqrt (dot-prod a a)))

  (defn elwise-subtract
    "Returns a vector (mx), subtracts a,b element-wise "
    [a b]
    (mapv - a b))

  (defn vec-dist
    "Returns the distance between two vecs (points in n dimension)"
    [a b]
    (vec-norm (elwise-subtract a b)))

(comment

  (def positions (->
                  (mapcat (fn [x]
                            (map (fn [y]
                                   [y x]) (range 8))) (range 8))))
  (doseq [p (partition 8 positions)]
    (println p))

  (def radius 1.5)
  (def center [2 3])

  (vec-dist [2 3] [3 4])

  (defnc within-radius
    [x y c r]
    (< (vec-dist [x y] c) radius))

  (->
   (run* [q]
         (fresh [x y]
                (fd/in x y (fd/interval 0 7))
                (within-radius x y center radius)
                (== q [x y])))
   (count))

  (run* [q]
        (fresh [a b]
               (conde
                [(== q [0 0]) (== q [2 2])]
                [(== a 1) (== b 2) (la/> b a) (== q [a b])])))

  (run* [q]
        (fresh [a b]
               (conda
                [(== q [0 0]) (== q [2 2])]
                [(== a 1) (== b 2) (la/> b a) (== q [a b])])))

  (run* [q]
        (fresh [a b]
               (condu
                [(== q [0 0]) (== q [2 2])]
                [(== a 1) (== b 2) (la/> b a) (== q [a b])])))
  ;;
  )