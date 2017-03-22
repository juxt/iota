;; Copyright © 2015, JUXT LTD.

(ns juxt.iota
  (:require
    #?(:clj  [clojure.test]
       :cljs [cljs.test :include-macros true])
    [clojure.set :as set]
    [schema.core :as s]))

;; See the test at the end of this ns to understand the point of this code

(defprotocol TestClause
  (as-test-function [_] "Take the clause and return a function which is applied to the value under test"))

(extend-protocol TestClause
  #?(:clj  clojure.lang.APersistentVector
     :cljs PersistentVector)
  ;; A function application 'path', which is simply a composed function,
  ;; left-to-right rather than right-to-left.
  (as-test-function [v] (apply comp (map as-test-function (reverse v))))

  #?(:clj  clojure.lang.Keyword
     :cljs Keyword)
  (as-test-function [k] k)

  #?(:clj  clojure.lang.Fn
     :cljs function)
  (as-test-function [f] f)

  #?(:clj  String
     :cljs string)
  (as-test-function [s] #(get % s))

  #?(:clj  Long
     :cljs long)
  (as-test-function [n] #(get (vec %) n)))

(defn- cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [then else]
  (if (cljs-env? &env) then else))

(defmacro is [& args]
  `(if-cljs
     (cljs.test/is ~@args)
     (clojure.test/is ~@args)))

(defmacro given "Given v, assert the following…"
  {:style/indent 1}
  [v & body]
  (let [t (gensym)]
    `(do
       (let [~t ~v]
         ~@(for [[a b c] (partition 3 body)]
             (case b
               ;; Equals?
               (:= :equals) `(is (= ((as-test-function ~a) ~t) ~c))
               (:!= :not-equals) `(is (not= ((as-test-function ~a) ~t) ~c))

               ;; Schema checks
               (:- :conforms) `(is (nil? (s/check ~c ((as-test-function ~a) ~t))))
               (:!- :not-conforms) `(is (not (nil? (s/check ~c ((as-test-function ~a) ~t)))))

               ;; Is?
               (:? :satisfies) `(is (~c ((as-test-function ~a) ~t)))
               (:!? :not-satisfies) `(is (not (~c ((as-test-function ~a) ~t))))

               ;; Matches regex?
               (:# :matches) `(is (re-matches (re-pattern ~c) ((as-test-function ~a) ~t)))
               (:!# :not-matches) `(is (not (re-matches (re-pattern ~c) ((as-test-function ~a) ~t))))

               ;; Is superset?
               (:> :⊃ :superset) `(is (set/superset? (set ((as-test-function ~a) ~t)) (set ~c)))
               (:!> :⊅ :not-superset) `(is (not (set/superset? (set ((as-test-function ~a) ~t)) (set ~c))))

               ;; Is subset?
               (:< :⊂ :subset) `(is (set/subset? (set ((as-test-function ~a) ~t)) (set ~c)))
               (:!< :⊄ :not-subset) `(is (not (set/subset? (set ((as-test-function ~a) ~t)) (set ~c))))

               ;; Is an instance of
               (:instanceof :instance) `(is (instance? ~c ((as-test-function ~a) ~t)))
               (:!instanceof :not-instance) `(is (not (instance? ~c ((as-test-function ~a) ~t))))

               ;; Every satisfies predicate
               (:∀ :every) `(is (every? ~c ((as-test-function ~a) ~t)))
               (:!∀ :not-every) `(is (not (every? ~c ((as-test-function ~a) ~t))))

               ))))))
