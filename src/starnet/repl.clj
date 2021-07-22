(ns starnet.repl
  (:require
   [clojure.pprint :as pp]
   [clojure.spec.alpha :as s]
   [clojure.core.async :as a :refer [<! >! <!! timeout chan alt! go
                                     >!! <!! alt!! alts! alts!! take! put!
                                     thread pub sub]])
  )

(comment

  create-user
  delete-account
  change-username
  change-email
  list-users
  list-user-account
  list-user-ongoing-games
  list-user-game-history
  create-event
  :event.type/single-elemination-bracket
  :event/start-ts
  cancel-event
  signin-event
  signout-event
  list-events
  list-event-signedup-users
  create-game
  cancel-game
  start-game
  end-game
  list-games
  join-game
  invite-into-game
  connect-to-game
  disconnect-from-game
  ingame-event
  list-ingame-events-for-game
  
  ;;
  )

(def games {0 #uuid "15108e92-959d-4089-98fe-b92bb7c571db"
            1 #uuid "461b65a8-0f24-46c9-8248-4bf6d7e1aa1a"})

(def users {0 #uuid "5ada3765-0393-4d48-bad9-fac992d00e62"
              1 #uuid "179c265a-7f72-4225-a785-2d048d575854"})

(def observers {0 #uuid "46855899-838a-45fd-98b4-c76c08954645"
                1 #uuid "ea1162e3-fe45-4652-9fa9-4f8dc6c78f71"
                2 #uuid "4cd4b905-6859-4c22-bae7-ad5ec51dc3f8"})



(comment

  ; https://clojure.org/reference/transients

  (defn vrange [n]
    (loop [i 0 v []]
      (if (< i n)
        (recur (inc i) (conj v i))
        v)))

  (defn vrange2 [n]
    (loop [i 0 v (transient [])]
      (if (< i n)
        (recur (inc i) (conj! v i))
        (persistent! v))))

  ;; benchmarked (Java 1.8, Clojure 1.7)
  (time (count (vrange 1000000)))    ;; 73.7 ms
  (time (count (vrange2 1000000)))   ;; 19.7 ms


  ;;
  )



(comment

  ; https://vimeo.com/45561411

  (def v (into [] (range 10000000)))
  (time (reduce + (map inc (filter even? v))))
  (time (reduce + (r/map inc (r/filter even? v))))
  (time (r/fold + (r/map inc (r/filter even? v))))


  ;;
  )


(comment

  ; https://blog.cognitect.com/blog/2014/8/6/transducers-are-coming

  (def xform (comp (map inc) (filter even?)))
  (->> [1 2 3 4] (map inc) (filter even?))



  ; Once you've got a transducer, what can you do with it?

  (def data [1 2 3 4])

  ;lazily transform the data (one lazy sequence, not three as with composed sequence functions)
  (sequence xform data)

  ; reduce with a transformation (no laziness, just a loop)
  (transduce xform + 0 data)

  ; build one collection from a transformation of another, again no laziness
  (into [] xform data)

  ; create a recipe for a transformation, which can be subsequently sequenced, iterated or reduced
  (iteration xform data)

  ; or use the same transducer to transform everything that goes through a channel
  (chan 1 xform)




  (def xf (map inc))
  ((xf (constantly 1)))


  (defn inc-with-print [x]
    (println x)
    (inc x))

  (type (eduction (map inc-with-print) (map inc-with-print) (range 3)))

  ;;
  )

(comment
  ; https://clojure.org/reference/multimethods

  (isa? String Object)
  (isa? (class {}) java.util.Map)
  (isa? (class []) java.util.Collection)

  (derive java.util.Map ::collection)
  (derive java.util.Collection ::collection)
  (isa? java.util.HashMap ::collection)

  (defmulti foo class)
  (defmethod foo ::collection [c] :a-collection)
  (defmethod foo String [s] :a-string)
  (foo [])
  ; :a-collection
  (foo (java.util.HashMap.))
  ; :a-collection
  (foo "bar")
  ; :a-string



  (defmulti area :Shape)
  (defn rect [wd ht] {:Shape :Rect :wd wd :ht ht})
  (defn circle [radius] {:Shape :Circle :radius radius})
  (defmethod area :Rect [r]
    (* (:wd r) (:ht r)))
  (defmethod area :Circle [c]
    (* (. Math PI) (* (:radius c) (:radius c))))
  (defmethod area :default [x] :oops)
  (def r (rect 4 13))
  (def c (circle 12))
  (area r)
  (area c)
  (area {})

  ;;
  )

(comment

  (def part {:app.part/key :app.part.key/user-data
             :app.part/state {:some :state}
             :app.part/depends-on [:app.part.key/another-part]
             :app.part/mount (fn [ctx part a b]
                               (part/get-state ctx :app.part.key/another-part)
                               (part/update-state part {:some :other-state}))
             :app.part/unmount (fn [ctx part a b])
             :app.part/status (fn [ctx part])
             :app.part/update-state (fn [ctx part])
             :app.part/get-state (fn [ctx part])})

  (derive java.util.Map ::map)
  (derive java.util.Collection ::coll)
  (derive (class :a-keyword) ::key)
  (isa? (class :asd) ::key)

  (isa? (class (seq [1])) java.util.Collection)
  (isa? (class {}) java.util.Collection)
  (ancestors java.util.Collection)


  (defmulti get-state class)
  (defmethod get-state ::map [x] :a-map)
  (defmethod get-state java.util.Collection [x] 'java.util.Collection)
  (defmethod get-state :default [x] :oops)

  (get-state {})
  (get-state [])

  (ns-unmap *ns* 'get-state)

  ;;
  )

(comment

  (derive java.lang.Object ::object)
  (derive java.util.Map ::map)
  (derive java.util.Collection ::coll)
  (derive (class :a-keyword) ::key)

  (defmulti get-state (fn [ctx part] [(class ctx) (class part)]))
  (defmethod get-state [::map ::map] [ctx part] :map-map)
  (defmethod get-state [::map ::key] [ctx part] :map-key)
  (defmethod get-state [::object ::object] [ctx part] :objects)
  (defmethod get-state [::object java.lang.Comparable] [ctx part] :object-comparable)
  (prefer-method get-state [::object java.lang.Comparable] [::object ::object])
  (prefer-method get-state  [::map ::map] [::object ::object])
  (defmethod get-state :default [ctx part] :oops)

  (get-state {} {})
  (get-state {} :a-keyword)
  (get-state {} "as")

  (ancestors (class []))
  (ancestors java.lang.String)
  (isa? java.lang.String ::object)

  ; on arity
  ; https://stackoverflow.com/questions/10313657/is-it-possible-to-overload-clojure-multi-methods-on-arity

  ;;
  )


(comment

  (defmulti variadic (fn [& args] (mapv class args)))
  (defmethod variadic [String] [& args] [:string])
  (defmethod variadic [String String]  [& args] [:string :string])
  (defmethod variadic [String java.util.Map] [& args] [:string :map])
  (defmethod variadic [Number java.util.Map]  [& args] [:number :map])
  (ancestors (class {}))
  (ns-unmap *ns* 'variadic)

  (variadic "asd")
  (variadic "asd" "asd")
  (variadic "asd" {})
  (isa? (class {}) java.util.Map)
  (variadic 1 {})


  (ns-unmap *ns* 'send-event)
  (stest/unstrument `send-event)
  (stest/instrument `send-event)

  (defmulti send-event
    "send event to kafka"
    {:arglists '([] [topic] [topic a-num] [a-num topic])}
    (fn [& args] [(count args) (mapv class args)]))
  (defmethod send-event [0 []] [& args] [])
  (defmethod send-event [1  [Number]]  [& args] [:number])
  (defmethod send-event [1  [String]]  [& args] [:string])
  (defmethod send-event [2  [String Number]] [& args] [:string :number])
  (defmethod send-event [2  [Number String]] [& args]  [:number :string])
  (defmethod send-event [2  [Number Number]] [& args]  [:number :number])
  (defmethod send-event [2  [java.util.Map Number]] [& args] [:map :number])
  (defmethod send-event [3  [java.util.Map Number String]] [& args] [:map :number :string])

  (s/fdef send-event
    :args (s/alt :0 (s/cat)
                 :number (s/cat :a number?)
                 :string (s/cat :a string?)
                 :string-number (s/cat :a string? :b number?)
                 :number-string (s/cat :a number? :b string?)
                 :number-number (s/cat :a number? :b number?)
                 :map-number (s/cat :a map? :b number?)
                 :map-number-string (s/cat :a map? :b number? :c string?)))

  (send-event)
  (send-event 1)
  (send-event "asd")
  (send-event "asd" 1)
  (send-event  1 "asd")
  (send-event  1 1)
  (send-event  {} 1)
  (send-event  {} 1 "asd")

  (send-event :a)
  (send-event :a :b)





  ;;
  )


(comment
  ; https://github.com/clojure/spec.alpha
  ; https://clojure.org/about/spec
  ; https://clojure.org/guides/spec
  ; https://clojure.github.io/test.check/intro.html
  ; https://clojure.github.io/test.check/generator-examples.html

  (s/conform even? 1000)
  (s/valid? even? 1000)

  (s/valid? nil? nil)
  (s/valid? string? "abc")
  (s/valid? #(> % 5) 10)
  (s/valid? inst? (Date.))
  (s/valid? #{42} 42)

  (s/def ::inst inst?)
  (s/valid? ::inst (Date.))
  (doc ::inst)
  (s/def ::big-even (s/and int? #(> % 1000)))
  (doc ::big-even)
  (s/valid? ::big-even 10000)
  (s/def ::string-or-int (s/or :string string?
                               :int int?))
  (s/valid? ::string-or-int "abc")
  (s/valid? ::string-or-int 1000)
  (s/valid? ::string-or-int :foo)
  (s/conform ::string-or-int "abc")
  (s/conform ::string-or-int 1000)

  (s/valid? string? nil)
  (s/valid? (s/nilable string?) nil)

  (s/explain ::big-even 10)
  (s/explain ::string-or-int :foo)
  (s/explain-str ::string-or-int :foo)
  (s/explain-data ::string-or-int :foo)

  (def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
  (s/def ::email-type (s/and string? #(re-matches email-regex %)))

  (s/def ::acctid int?)
  (s/def ::username string?)
  (s/def ::email ::email-type)
  (s/def ::phone string?)

  (s/def ::user (s/keys :req [::username ::email]
                        :opt [::phone]))
  (s/valid? ::user
            {::username "user1"
             ::email "user1@gmail.com"})
  (s/explain ::user
             {::username "user1"})
  (s/explain-str ::user
                 {::username "user1"
                  ::email "n/a"})

  (s/def :unq/user
    (s/keys :req-un [::username ::email]
            :opt-un [::phone]))
  (s/valid? :unq/user
            {:username "user1"
             :email "user1@gmail.com"})
  (s/conform  :unq/user
              {:username "user1"
               :email "user1@gmail.com"})
  (s/explain :unq/user
             {:username "user1"})
  (s/explain :unq/user
             {:username "user1"
              :email "n/a"})

  (s/def ::port number?)
  (s/def ::host string?)
  (s/def ::id keyword?)
  (s/def ::server (s/keys* :req [::id ::host] :opt [::port]))
  (s/conform ::server [::id :s1 ::host "localhost" ::port 8080])

  (s/def :animal/kind string?)
  (s/def :animal/says string?)
  (s/def :animal/common (s/keys :req [:animal/kind :animal/says]))
  (s/def :dog/tail? boolean?)
  (s/def :dog/breed string?)
  (s/def :animal/dog (s/merge :animal/common
                              (s/keys :req [:dog/tail? :dog/breed])))
  (s/valid? :animal/dog
            {:animal/kind "dog"
             :animal/says "woof"
             :dog/tail? true
             :dog/breed "retriever"})

  ; https://clojure.org/guides/spec#_multi_spec

  (s/def :event/timestamp int?)
  (s/def :search/url string?)
  (s/def :error/message string?)
  (s/def :error/code int?)

  (defmulti event-type :event/type)
  (defmethod event-type :event/search [_]
    (s/keys :req [:event/type :event/timestamp :search/url]))
  (defmethod event-type :event/error [_]
    (s/keys :req [:event/type :event/timestamp :error/message :error/code]))

  (s/def :event/event (s/multi-spec event-type :event/type))

  (s/valid? :event/event
            {:event/type :event/search
             :event/timestamp 1463970123000
             :search/url "https://clojure.org"})

  (s/valid? :event/event
            {:event/type :event/error
             :event/timestamp 1463970123000
             :error/message "Invalid host"
             :error/code 500})

  (s/explain :event/event
             {:event/type :event/restart})

  (s/explain :event/event
             {:event/type :event/search
              :search/url 200})

  ; collection

  (s/conform (s/coll-of keyword?) [:a :b :c])
  (s/conform (s/coll-of number?) #{5 10 2})

  (s/def ::vnum3 (s/coll-of number? :kind vector? :count 3 :distinct true :into #{}))
  (s/conform ::vnum3 [1 2 3])
  (s/explain ::vnum3 #{1 2 3})
  (s/explain ::vnum3 [1 1 1])
  (s/explain ::vnum3 [1 2 :a])

  ; tuple

  (s/def ::point (s/tuple double? double? double?))
  (s/conform ::point [1.5 2.5 -0.5])
  (doc s/cat)

  ; map

  (s/def ::scores (s/map-of string? int?))
  (s/conform ::scores {"Sally" 1000, "Joe" 500})

  ;  sequences

  (s/def ::ingredient (s/cat :quantity number? :unit keyword?))
  (s/conform ::ingredient [2 :teaspoon])
  (s/explain ::ingredient [11 "peaches"])
  (s/explain ::ingredient [2])

  (s/def ::seq-of-keywords (s/* keyword?))
  (s/conform ::seq-of-keywords [:a :b :c])
  (s/explain ::seq-of-keywords [10 20])

  (s/def ::odds-then-maybe-even (s/cat :odds (s/+ odd?)
                                       :even (s/? even?)))
  (s/conform ::odds-then-maybe-even [1 3 5 100])
  (s/conform ::odds-then-maybe-even [1])
  (s/explain ::odds-then-maybe-even [100])

  (s/def ::opts (s/* (s/cat :opt keyword? :val boolean?)))
  (s/conform ::opts [:silent? false :verbose true])

  (s/def ::config (s/*
                   (s/cat :prop string?
                          :val  (s/alt :s string? :b boolean?))))
  (s/conform ::config ["-server" "foo" "-verbose" true "-user" "joe"])

  (s/describe ::seq-of-keywords)
  (s/describe ::odds-then-maybe-even)
  (s/describe ::opts)
  (s/describe ::config)

  (s/def ::even-strings (s/& (s/* string?) #(even? (count %))))
  (s/valid? ::even-strings ["a"])  ;; false
  (s/valid? ::even-strings ["a" "b"])  ;; true
  (s/valid? ::even-strings ["a" "b" "c"])  ;; false
  (s/valid? ::even-strings ["a" "b" "c" "d"])  ;; true

  (s/def ::nested
    (s/cat :names-kw #{:names}
           :names (s/spec (s/* string?))
           :nums-kw #{:nums}
           :nums (s/spec (s/* number?))))
  (s/conform ::nested [:names ["a" "b"] :nums [1 2 3]])

  (s/def ::unnested
    (s/cat :names-kw #{:names}
           :names (s/* string?)
           :nums-kw #{:nums}
           :nums (s/* number?)))
  (s/conform ::unnested [:names "a" "b" :nums 1 2 3])

  ; using spec for validation

  (defn user
    [user]
    {:pre [(s/valid? ::user user)]
     :post [(s/valid? string? %)]}
    (str (::username user) " " (::email user)))
  (user 42)
  (user {::username "user1" ::email "user1_gmail.com"})
  (user {::username "user1" ::email "user1@gmail.com"})

  (defn user2
    [user]
    (let [u (s/assert ::user user)]
      (str (::username u) " " (::email u))))
  (s/check-asserts true)
  (user2 42)
  (user2 {::username "user1" ::email "user1_gmail.com"})
  (user2 {::username "user1" ::email "user1@gmail.com"})
  (doc s/assert)


  (defn- set-config [prop val]
  ;; dummy fn
    (println "set" prop val))

  (defn configure [input]
    (let [parsed (s/conform ::config input)]
      (if (= parsed ::s/invalid)
        (throw (ex-info "Invalid input" (s/explain-data ::config input)))
        (for [{prop :prop [_ val] :val} parsed]
          (set-config (subs prop 1) val)))))

  (configure ["-server" "foo" "-verbose" true "-user" "joe"])


  (defn ranged-rand
    "Returns random int in range start <= rand < end"
    [start end]
    (+ start (long (rand (- end start)))))

  (s/fdef ranged-rand
    :args (s/and (s/cat :start int? :end int?)
                 #(< (:start %) (:end %)))
    :ret int?
    :fn (s/and #(>= (:ret %) (-> % :args :start))
               #(< (:ret %) (-> % :args :end))))
  (doc ranged-rand)
  (ranged-rand 1 3)
  (ranged-rand 5 1)
  (ranged-rand 1 5)
  (stest/instrument `ranged-rand)
  (ranged-rand 5 1)
  (stest/unstrument `ranged-rand)

  (stest/check `ranged-rand)

  (defn adder [x] #(+ x %))
  (s/fdef adder
    :args (s/cat :x number?)
    :ret (s/fspec :args (s/cat :y number?)
                  :ret number?)
    :fn #(= (-> % :args :x) ((:ret %) 0)))


  (s/fdef clojure.core/declare
    :args (s/cat :names (s/* simple-symbol?))
    :ret any?)
  (declare 100)

  (defn hello
    [name]
    (str "hello " name))
  (s/fdef hello
    :args string?
    :ret string?)
  (hello "asd")
  (hello :asd)
  (stest/instrument `hello)
  (hello :asd)
  (stest/unstrument `hello)

  (stest/abbrev-result (first (stest/check `ranged-rand)))
  (-> (stest/enumerate-namespace 'starnet.samples.kafka.spec-example) stest/check)
  (stest/check)


  (defn invoke-service [service request]
  ;; invokes remote service
    )

  (defn run-query [service query]
    (let [{::keys [result error]} (invoke-service service {::query query})]
      (or result error)))

  (s/def ::query string?)
  (s/def ::request (s/keys :req [::query]))
  (s/def ::result (s/coll-of string? :gen-max 3))
  (s/def ::error int?)
  (s/def ::response (s/or :ok (s/keys :req [::result])
                          :err (s/keys :req [::error])))

  (s/fdef invoke-service
    :args (s/cat :service any? :request ::request)
    :ret ::response)

  (s/fdef run-query
    :args (s/cat :service any? :query string?)
    :ret (s/or :ok ::result :err ::error))

  (stest/instrument `invoke-service {:stub #{`invoke-service}})
  (invoke-service nil {::query "test"})
  (invoke-service nil {::query "test"})
  (stest/summarize-results (stest/check `run-query))


  ; generators

  (gen/generate (s/gen int?))
  (gen/generate (s/gen nil?))
  (gen/sample (s/gen string?))
  (gen/sample (s/gen #{:club :diamond :heart :spade}))
  (gen/sample (s/gen (s/cat :k keyword? :ns (s/+ number?))))

  (s/exercise (s/cat :k keyword? :ns (s/+ number?)) 5)
  (s/exercise (s/or :k keyword? :s string? :n number?) 5)

  (s/exercise-fn `ranged-rand)


  (gen/generate (s/gen even?))
  (gen/generate (s/gen (s/and int? even?)))

  (defn divisible-by [n] #(zero? (mod % n)))
  (gen/sample (s/gen (s/and int?
                            #(> % 0)
                            (divisible-by 3))))

  (gen/sample (s/gen (s/and string? #(clojure.string/includes? % "hello"))))


  ; custom generators

  (s/def ::kws (s/and keyword? #(= (namespace %) "my.domain")))
  (s/valid? ::kws :my.domain/name) ;; true
  (gen/sample (s/gen ::kws)) ;; unlikely we'll generate useful keywords this way

  (def kw-gen (s/gen #{:my.domain/name :my.domain/occupation :my.domain/id}))
  (gen/sample kw-gen 5)

  (s/def ::kws (s/with-gen (s/and keyword? #(= (namespace %) "my.domain"))
                 #(s/gen #{:my.domain/name :my.domain/occupation :my.domain/id})))
  (s/valid? ::kws :my.domain/name)  ;; true
  (gen/sample (s/gen ::kws))
  ;;=> (:my.domain/occupation :my.domain/occupation :my.domain/name  ...)

  (def kw-gen-2 (gen/fmap #(keyword "my.domain" %) (gen/string-alphanumeric)))
  (gen/sample kw-gen-2 5)

  (def kw-gen-3 (gen/fmap #(keyword "my.domain" %)
                          (gen/such-that #(not= % "")
                                         (gen/string-alphanumeric))))
  (gen/sample kw-gen-3 5)


  (s/def ::hello
    (s/with-gen #(clojure.string/includes? % "hello")
      #(gen/fmap (fn [[s1 s2]] (str s1 "hello" s2))
                 (gen/tuple (gen/string-alphanumeric) (gen/string-alphanumeric)))))
  (gen/sample (s/gen ::hello))

  (s/def ::roll (s/int-in 0 11))
  (gen/sample (s/gen ::roll))

  (s/def ::the-aughts (s/inst-in #inst "2000" #inst "2010"))
  (drop 50 (gen/sample (s/gen ::the-aughts) 55))

  (s/def ::dubs (s/double-in :min -100.0 :max 100.0 :NaN? false :infinite? false))
  (s/valid? ::dubs 2.9)
  (s/valid? ::dubs Double/POSITIVE_INFINITY)
  (gen/sample (s/gen ::dubs))



  ;; 
  )

(comment

  (ns-unmap *ns* 'ev-type)

  (defmulti ev-type (fn [kw] kw))
  (defmethod ev-type :ev.u/create [_] #{:ev.u/create})
  (defmethod ev-type :ev.u/delete [_] #{:ev.u/delete})
  (s/def :ev/type1 (s/multi-spec ev-type (fn [gened-val dispatch-tag]
                                           (prn "gened-val " gened-val)
                                           (prn "dispatch-tag " dispatch-tag)
                                           dispatch-tag)))
  (ev-type :ev.u/create)
  (s/valid? :ev/type1 :ev.u/create)
  (s/valid? :ev/type1 :ev.u/delete)
  (s/valid? :ev/type1 :ev.u/delete1)
  (gen/generate (s/gen :ev/type1))

  ;;
  )


(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")

(defn spec-email
  []
  (let []
    (s/with-gen
      (s/and string? #(re-matches email-regex %))
      #(sgen/fmap (fn [s]
                    (str s "@gmail.com"))
                  (gen/such-that (fn [s] (not= s ""))
                                 gen/string-alphanumeric)))))

(defn spec-string-in-range
  [min max & {:keys [gen-char] :or {gen-char gen/char-alphanumeric}}]
  (s/with-gen
    string?
    #(gen/fmap (fn [v] (apply str v)) (gen/vector gen-char min max))))

(comment

  (gen/generate gen/string)
  (gen/generate gen/string-ascii)
  (gen/generate gen/string-alphanumeric)

  (gen/generate (s/gen (spec-string-in-range 4 16 :gen-char gen/char-ascii)))
  (gen/generate (s/gen (spec-string-in-range 4 16 :gen-char gen/char)))

  ;;
  )


(s/def :u/uuid uuid?)
(s/def :u/username (spec-string-in-range 4 16 :gen-char gen/char-alphanumeric))
(s/def :u/fullname (spec-string-in-range 4 32 :gen-char gen/char-ascii))
(s/def :u/password (spec-string-in-range 8 64 :gen-char gen/char-alphanumeric))
(s/def :u/email (spec-email))

(s/def :u/user (s/keys :req [:u/uuid :u/username :u/email
                             :u/password :u/fullname]))


(def eventset-event
  #{:ev.c/delete-record :ev.u/create
    :ev.u/update :ev.u/delete
    :ev.g.u/create :ev.g.u/update-role
    :ev.g.u/delete :ev.g.u/configure
    :ev.g.u/start :ev.g.u/join
    :ev.g.u/leave :ev.g.p/move-cape
    :ev.g.p/collect-tile-value
    :ev.g.a/finish-game})

(s/def :ev/type eventset-event)

(s/def :ev.c/delete-record (with-gen-fmap
                             (s/keys :req [:ev/type])
                             #(assoc %  :ev/type :ev.c/delete-record)))

(s/def :ev.u/create (with-gen-fmap
                      (s/keys :req [:ev/type :u/uuid :u/email :u/username]
                              :opt [])
                      #(assoc %  :ev/type :ev.u/create)))

(s/def :ev.u/update (with-gen-fmap
                      (s/keys :req [:ev/type]
                              :opt [:u/email :u/username])
                      #(assoc %  :ev/type :ev.u/update)))

(s/def :ev.u/delete (with-gen-fmap
                      (s/keys :req [:ev/type]
                              :opt [])
                      #(assoc %  :ev/type :ev.u/delete)))

(defmulti ev (fn [x] (:ev/type x)))
(defmethod-set ev eventset-event)
(s/def :ev/event (s/multi-spec ev :ev/type))

(def setof-ev-u-event
  #{:ev.u/create :ev.u/update :ev.u/delete})

(defmulti ev-user (fn [x] (:ev/type x)))
(defmethod-set ev-user setof-ev-u-event)
(s/def :ev.u/event (s/multi-spec ev-user :ev/type))


(s/fdef starnet.common.alpha.user/next-state-user
  :args (s/cat :state (s/nilable :u/user)
               :k uuid?
               :ev :ev.u/event)
  :ret (s/nilable :u/user))

(comment

  (ns-unmap 'starnet.common.alpha.user 'next-state-user)
  (stest/instrument ['starnet.common.alpha.user/next-state-user])
  (stest/unstrument ['starnet.common.alpha.user/next-state-user])

  ;;
  )


(s/def :g.e/uuid uuid?)
(s/def :g.e/pos (s/tuple int? int?))
(s/def :g.e/numeric-value number?)
(s/def :g.e/type keyword?)

(s/def :g.e.type/teleport (s/keys :req [:g.e/type :g.e/uuid :g.e/pos]))
(s/def :g.e.type/cape (s/keys :req [:g.e/type :g.e/uuid  :g.e/pos]))
(s/def :g.e.type/value-tile (s/keys :req [:g.e/type :g.e/uuid :g.e/pos :g.e/numeric-value]))

(s/def :g.p/cape :g.e.type/cape)
(s/def :g.p/entities (s/keys :req [:g.p/cape]))
(s/def :g.p/sum number?)

(s/def :g.p/player (s/keys :req [:g.p/entities :g.p/sum]))

(s/def :g.r/host (s/nilable boolean?))
(s/def :g.r/player (s/nilable int?))
(s/def :g.r/observer (s/nilable boolean?))
(s/def :g.r/role (s/keys :req [:g.r/host :g.r/player :g.r/observer]))

(def setof-game-status #{:created :opened :started :finished})

(s/def :g/uuid uuid?)
(s/def :g/status setof-game-status)
(s/def :g/start-inst inst?)
(s/def :g/duration-ms number?)
(s/def :g/map-size (s/tuple int? int?))
(s/def :g/roles (s/map-of uuid? :g.r/role))
(s/def :g/player-states (s/map-of int? :g.p/player))
(s/def :g/exit-teleports (s/coll-of :g.e.type/teleport))
(s/def :g/value-tiles (s/coll-of :g.e.type/value-tile))

(s/def :g/game (s/keys :req [:g/uuid :g/status
                             :g/duration-ms :g/start-inst
                             :g/roles :g/player-states
                             :g/value-tiles :g/exit-teleports
                             :g/map-size]))

(s/def :ev.g.u/create (with-gen-fmap
                        (s/keys :req [:ev/type :u/uuid]
                                :opt [])
                        #(assoc %  :ev/type :ev.g.u/create)))

(s/def :ev.g.u/delete (with-gen-fmap
                        (s/keys :req [:ev/type]
                                :opt [])
                        #(assoc %  :ev/type :ev.g.u/delete)))

(s/def :ev.g.u/configure (with-gen-fmap
                           (s/keys :req [:ev/type :u/uuid :g/uuid]
                                   :opt [])
                           #(assoc %  :ev/type :ev.g.u/configure)))

(s/def :ev.g.u/start (with-gen-fmap
                       (s/keys :req [:ev/type :u/uuid :g/uuid]
                               :opt [])
                       #(assoc %  :ev/type :ev.g.u/start)))

(s/def :ev.g.u/join (with-gen-fmap
                      (s/keys :req [:ev/type :u/uuid :g/uuid]
                              :opt [])
                      #(assoc %  :ev/type :ev.g.u/join)))

(s/def :ev.g.u/leave (with-gen-fmap
                       (s/keys :req [:ev/type :u/uuid :g/uuid]
                               :opt [])
                       #(assoc %  :ev/type :ev.g.u/leave)))

(s/def :ev.g.u/update-role (with-gen-fmap
                             (s/keys :req [:ev/type :u/uuid :g/uuid :g.r/role]
                                     :opt [])
                             #(assoc %  :ev/type :ev.g.u/update-role)))

(s/def :ev.g.p/move-cape (with-gen-fmap
                           (s/keys :req [:ev/type :u/uuid :g/uuid
                                         :g.p/cape])
                           #(assoc %  :ev/type :ev.g.p/move-cape)))

(s/def :ev.g.p/collect-tile-value (with-gen-fmap
                                    (s/and (s/keys :req [:ev/type :u/uuid]))
                                    #(assoc %  :ev/type :ev.g.p/collect-tile-value)))

(s/def :ev.g.a/finish-game (with-gen-fmap
                             (s/and (s/keys :req [:ev/type :u/uuid]))
                             #(assoc %  :ev/type :ev.g.a/finish-game)))

(def setof-ev-g-p-event
  #{:ev.g.p/move-cape :ev.g.p/collect-tile-value})

(defmulti ev-game-player (fn [x] (:ev/type x)))
(defmethod-set ev-game-player setof-ev-g-p-event)
(s/def :ev.g.p/event (s/multi-spec ev-game-player :ev/type))

(defmulti ev-game-arbiter (fn [x] (:ev/type x)))
(defmethod ev-game-arbiter :ev.g.a/finish-game [x] :ev.g.a/finish-game)
(s/def :ev.g.a/event (s/multi-spec ev-game-arbiter :ev/type))

(def setof-ev-g-m-event
  #{:ev.g.p/move-cape :ev.g.p/collect-tile-value :ev.g.a/finish-game})

(defmulti ev-game-member (fn [x] (:ev/type x)))
(defmethod-set ev-game-member setof-ev-g-m-event)
(s/def :ev.g.m/event (s/multi-spec ev-game-member :ev/type))

(def setof-ev-g-u-event
  #{:ev.g.u/create :ev.g.u/delete :ev.c/delete-record
    :ev.g.u/configure :ev.g.u/start :ev.g.u/join
    :ev.g.u/leave})

(defmulti ev-game-user (fn [x] (:ev/type x)))
(defmethod-set ev-game-user setof-ev-g-u-event)
(s/def :ev.g.u/event (s/multi-spec ev-game-user :ev/type))

(def setof-ev-g-event
  #{:ev.g.u/create :ev.g.u/delete :ev.c/delete-record
    :ev.g.u/configure :ev.g.u/start :ev.g.u/join :ev.g.u/update-role
    :ev.g.u/leave :ev.g.p/move-cape
    :ev.g.p/collect-tile-value :ev.g.a/finish-game})

(defmulti ev-game (fn [x] (:ev/type x)))
(defmethod-set ev-game setof-ev-g-event)
(s/def :ev.g/event (s/multi-spec ev-game :ev/type))



(s/fdef starnet.common.alpha.game/make-game-state
  :args (s/cat)
  :ret :g/game)

(s/fdef starnet.common.alpha.game/next-state-game
  :args (s/cat :state (s/nilable :g/game)
               :k uuid?
               :ev :ev.g/event)
  :ret (s/nilable :g/game))

(comment

  (ns-unmap 'starnet.common.alpha.game 'next-state-game)
  (stest/instrument ['starnet.common.alpha.game/next-state-game])
  (stest/unstrument ['starnet.common.alpha.game/next-state-game])

  (gen/sample (s/gen :ev.g.u/update-role) 10)
  (gen/sample (s/gen :ev.g.u/create) 10)
  (gen/sample (s/gen :g.r/role) 10)


  (stest/check 'starnet.common.alpha.game/next-state-game)

  ;;
  )

(def topic-evtype-map
  {"alpha.user" #{:ev.u/create :ev.u/update :ev.u/delete}
   "alpha.game" #{:ev.g.u/create :ev.g.u/delete
                  :ev.g.u/join :ev.g.u/leave
                  :ev.g.u/configure :ev.g.u/start
                  :ev.g.p/move-cape :ev.g.p/collect-tile-value
                  :ev.g.a/finish-game}})

(def evtype-topic-map
  (->> topic-evtype-map
       (map (fn [[topic kset]]
              (map #(vector % topic) kset)))
       (mapcat identity)
       (into {})))

(def evtype-recordkey-map
  {:ev.u/create :u/uuid
   :ev.u/update :u/uuid
   :ev.u/delete :u/uuid
   :ev.g.u/configure :g/uuid})

(defn event-to-recordkey
  [ev]
  (or
   (-> ev :ev/type evtype-recordkey-map ev)
   (gen/generate gen/uuid)))

(defn event-to-topic
  [ev]
  (-> ev :ev/type evtype-topic-map))

(comment

  (def input-chan (a/chan))
  (def our-publication (a/pub input-chan :msg-type))
  (a/>!! input-chan {:msg-type :greeting :text "hello"})

  (def output-chan (a/chan))
  (a/sub our-publication :greeting output-chan)

  (def l (a/go-loop []
           (let [{:keys [text]} (a/<! output-chan)]
             (println (str "msg: " text))
             (recur))))

  (a/>!! input-chan {:msg-type :greeting :text "hello"})

  (let [c (a/chan)]
    (a/sub our-publication :greeting c)
    (a/go-loop []
      (let [{:keys [msg-type text]} (a/<! c)]
        (println (str "chan2 msg: " text))
        (recur))))

  (def loser-chan (a/chan))
  (a/sub our-publication :loser loser-chan)
  (a/>!! input-chan {:msg-type :loser :text "I won't be accepted"})

  (a/put! input-chan {:msg-type :greeting :text "hello2"})



  ;;
  )

; https://github.com/clojure/core.async/blob/master/examples/ex-go.clj

(defn fake-search [kind]
  (fn [c query]
    (go
      (<! (timeout (rand-int 100)))
      (>! c [kind query]))))

(def web1 (fake-search :web1))
(def web2 (fake-search :web2))
(def image1 (fake-search :image1))
(def image2 (fake-search :image2))
(def video1 (fake-search :video1))
(def video2 (fake-search :video2))

(defn fastest [query & replicas]
  (let [c (chan)]
    (doseq [replica replicas]
      (replica c query))
    c))

(defn google-go [query]
  (let [c (chan)
        t (timeout 80)]
    (go (>! c (<! (fastest query web1 web2))))
    (go (>! c (<! (fastest query image1 image2))))
    (go (>! c (<! (fastest query video1 video2))))
    (go (loop [i 0 ret []]
          (if (= i 3)
            ret
            (recur (inc i) (conj ret (alt! [c t] ([v] v)))))))))

(defn google-async [query]
  (let [c (chan)
        t (timeout 80)]
    (future (>!! c (<!! (fastest query web1 web2))))
    (future (>!! c (<!! (fastest query image1 image2))))
    (future (>!! c (<!! (fastest query video1 video2))))
    (loop [i 0 ret []]
      (if (= i 3)
        ret
        (recur (inc i) (conj ret (alt!! [c t] ([v] v))))))))

(comment

  (<!! (google-go "clojure"))
  (google-async "clojure")



  ;;
  )


(defn fan-in [ins]
  (let [c (chan)]
    (go (while true
          (let [[x c0] (alts! ins)]
            (>! c x))))
    c))

(defn fan-out [in cs-or-n]
  (let [cs (if (number? cs-or-n)
             (repeatedly cs-or-n chan)
             cs-or-n)]
    (go (while true
          (let [x (<! in)
                outs (map #(vector % x) cs)]
            (alts! outs))))
    cs))



(comment

  (let [cout (chan)
        cin (fan-in (fan-out cout (repeatedly 3 chan)))]
    (go (dotimes [n 10]
          (>! cout n)
          (prn (<! cin))))
    nil)

  ;;
  )


(defn fan-in-2 [ins]
  (let [c (chan)]
    (future (while true
              (let [[x] (alts!! ins)]
                (>!! c x))))
    c))

(defn fan-out-2 [in cs-or-n]
  (let [cs (if (number? cs-or-n)
             (repeatedly cs-or-n chan)
             cs-or-n)]
    (future (while true
              (let [x (<!! in)
                    outs (map #(vector % x) cs)]
                (alts!! outs))))
    cs))

(comment

  (let [cout (chan)
        cin (fan-in-2 (fan-out-2 cout (repeatedly 3 chan)))]
    (dotimes [n 10]
      (>!! cout n)
      (prn (<!! cin))))

  ;;
  )

  ; https://www.youtube.com/watch?v=enwIIGzhahw
  ; https://github.com/halgari/clojure-conj-2013-core.async-examples

(comment



  (def c (chan))
  (take! c (fn [v] (println v)))
  (put! c "hi")
  (put! c 42 (fn [x] (println "done putting" x)))
  (take! c (fn [v] (println v)))

  (defn takep [c]
    (let [p (promise)]
      (take! c (fn [v] (deliver p v)))
      @p))

  (future (println "Got " (takep c)))

  (put! c 42)

  (defn putp [c val]
    (let [p (promise)]
      (put! c val (fn [_] (deliver p nil)))
      @p))

  (future (println "Done " (putp c :a)))
  (future (println "Got!" (takep c)))

  (future (println "Done " (>!! c 42)))
  (future (println "Got!" (<!! c)))

  (thread 42)
  (<!! (thread 42))
  (<!! (thread (println "It works!" (<!! (thread 42)))))

  (go 42)
  (<!! (go 42))
  (go (println "It works!" (<! (go 42))))

  (def fbc (chan 1))
  (go (>! fbc 1)
      (println "done 1"))
  (go (>! fbc 2)
      (println "done 2"))
  (<!! fbc)

  (def dbc (chan (a/dropping-buffer 1)))
  (go (>! dbc 1)
      (println "done 1"))
  (go (>! dbc 2)
      (println "done 2"))
  (<!! dbc)

  (def sbc (chan (a/sliding-buffer 1)))
  (go (>! sbc 1)
      (println "done 1"))
  (go (>! sbc 2)
      (println "done 2"))
  (<!! sbc)

  (def c (chan))
  (a/close! c)
  (<!! c)

  (def a (chan))
  (def b (chan))
  (put! a 42)
  (alts!! [a b])

  (<!! (timeout 1000))

  (alts!! [a (timeout 1000)])
  (alts!! [[a 42]
           (timeout 1000)])
  (alts!! [a]
          :default :nothing-found)

  (put! a :a)
  (put! b :b)
  (alts!! [a b])

  (put! a :a)
  (put! b :b)
  (alts!! [a b]
          :priority true)

  ; log 

  (def log-chan (chan))

  (thread
    (loop []
      (when-let [v (<!! log-chan)]
        #_(.println System/out v)
        (println v)
        (recur)))
    (println "log 1 closed"))

  (thread
    (loop []
      (when-let [v (<!! log-chan)]
        (.println System/out v)
        (recur)))
    (println "log 2 closed"))

  (a/close! log-chan)

  (defn log [msg]
    (>!! log-chan msg))

  (log "foo")

  ; mult

  (def to-mult (chan 1))
  (def m (a/mult to-mult))

  (let [c (chan 1)]
    (a/tap m c)
    (go (loop []
          (when-let [v (<! c)]
            (println "Got " v)
            (recur)))
        (println "exiting")))

  (>!! to-mult 42)

  (a/close! to-mult)

  ;;
  )



(comment

  ;https://github.com/halgari/clojure-conj-2013-core.async-examples/blob/master/src/clojure_conj_talk/core.clj#L394

    ; pub sub

  (def to-pub (chan 1))
  (def p (pub to-pub :tag))

  (def print-chan (chan 1))

  (go (loop []
        (when-let [v (<! print-chan)]
          (println v)
          (recur))))

;; This guy likes updates about cats.
  (let [c (chan 1)]
    (sub p :cats c)
    (go (println "I like cats:")
        (loop []
          (when-let [v (<! c)]
            (>! print-chan (pr-str "Cat guy got: " v))
            (recur)))
        (println "Cat guy exiting")))

;; This guy likes updates about dogs
  (let [c (chan 1)]
    (sub p :dogs c)
    (go (println "I like dogs:")
        (loop []
          (when-let [v (<! c)]
            (>! print-chan (pr-str "Dog guy got: " v))
            (recur)))
        (println "Dog guy exiting")))

;; This guy likes updates about animals
  (let [c (chan 1)]
    (sub p :dogs c)
    (sub p :cats c)
    (go (println "I like cats or dogs:")
        (loop []
          (when-let [v (<! c)]
            (>! print-chan (pr-str "Cat/Dog guy got: " v))
            (recur)))
        (println "Cat/dog guy exiting")))


  (defn send-with-tags [msg]
    (doseq [tag (:tags msg)]
      (println "sending... " tag)
      (>!! to-pub {:tag tag
                   :msg (:msg msg)})))

  (send-with-tags {:msg "New Cat Story"
                   :tags [:cats]})

  (send-with-tags {:msg "New Dog Story"
                   :tags [:dogs]})

  (send-with-tags {:msg "New Pet Story"
                   :tags [:cats :dogs]})

  (a/close! to-pub)

  ;;
  )


(defn main-process
  [p]
  (let [c (chan 1)]
    (sub p :core c)
    (go
      (loop []
        (when-let [[[t v] c] (alts! [c])]
          (condp = v
            :up (do
                  (println "system is up")
                  (recur))
            :check (do
                     (println "system is ok")
                     (recur))
            :down (do
                    (println "system is down")
                    (recur))
            :close (do
                     (println "main process will close")
                     (a/close! c)))))
      (println "main-process exiting"))
    c))

(comment

  (def system-chan (chan (a/sliding-buffer 10)))
  (def system-chan-publication (pub system-chan first))

  (def c (main-process system-chan-publication))

  (put! system-chan [:core :up])
  (put! system-chan [:core :check])
  (put! system-chan [:core :down])
  (put! system-chan [:core :up])
  (put! system-chan [:core :close])

  (a/close! c)

  ;;
  )


(comment


  (def c (go
           (do
             (<! (timeout 1000))
             (println "done"))
           [1 2]))

  (take! c (fn [v] (println v)))


  (def c (go
           (let [c (chan 1)]
             (do
               (<! (timeout 1000))
               (println "done")
               (>! c [1 2])
               c))))

  (take! c (fn [v] (println v)))

  ;;
  )


(comment

  (<!! (go
         (<! (go
               (<! (timeout 1000))
               3))))

  ;;
  )


(comment


  (defn proc1
    [c]
    (go (loop [cnt 0]
          (if-let [v (<! c)]
            (println (format "proc1: val %s cnt %s" v cnt)))
          (recur (inc cnt)))
        (println "proc1 exiting")))

  (def c1 (chan (a/sliding-buffer 10)))

  (def p1 (proc1 c1))
  (put! c1 {})

  (a/poll! c1)

  (a/close! p1)
  (a/close! c1)

  (defn proc2
    [c1 c2]
    (go (loop [cnt 0]
          (if-let [[v c] (alts! [c1 c2])]
            (println (format "proc2: val %s cnt %s" v cnt)))
          (recur (inc cnt)))
        (println "proc2 exiting")))

  (def c1 (chan (a/sliding-buffer 10)))
  (def c2 (chan (a/sliding-buffer 10)))

  (def p2 (proc2 c1 c2))
  (put! c1 :a)
  (put! c2 :b)

  (let [[v c] (alts!! [c1 c2])]
    (println v))

  (a/close! c1)

  ;;
  )

(comment

  (def c1 (chan 1))
  (def c2 (chan 1))

  (go (loop []
        (alt!
          c1 ([v] (println v) (recur))
          c2 ([{:keys [a b]}] (println [a b]) (recur)))))

  (put! c1 {:a 1 :b 2})

  (put! c2 {:a 1 :b 2})



  ;;
  )


(comment

  (def chan-close (chan 1))
  (go (loop []
        (alt!
          (timeout (+ 500 (rand-int 800))) (do
                                             (println "tout")
                                             (recur))
          chan-close (println "chan-close ")))
      (println "exiting loop"))

  (a/close! chan-close)
  ;;
  )


(comment

  (def pc (a/promise-chan))
  (put! pc 3)
  (take! pc (fn [v] (println v)))

  ;;
  )


(comment

  (let [c (chan)]
    (offer! c 42))

  (let [c (chan 1)]
    (offer! c 42))

  ; pipeline

  (def c1 (chan 10))
  (def c2 (chan 10))

  (def _ (pipeline 4 c2 (map inc) c1))

  (doseq [i (range 10)]
    (put! c1 i))
  (go-loop []
    (println (<! c2))
    (recur))

  ; pipeline-async

  (def c1 (chan 10))
  (def c2 (chan 10))
  (def af (fn [input port]
            (go
              (<! (timeout (+ 500 (rand-int 1000))))
              (offer! port (inc input))
              (close! port))))

  (def _ (pipeline-async 4 c2 af c1))
  (doseq [i (range 10)]
    (put! c1 i))
  (go-loop []
    (println (<! c2))
    (recur))

  ; pipeline-blocking


  (time (let [blocking-operation (fn [arg] (do
                                             (<!! (timeout 1000))
                                             (inc arg)))
              concurrent 4
              output-chan (chan)
              input-coll (range 0 4)]
          (pipeline-blocking concurrent
                             output-chan
                             (map blocking-operation)
                             (to-chan input-coll))
          (count (<!! (a/into [] output-chan))))) ; ~ 1000 ms


  ;;
  )



