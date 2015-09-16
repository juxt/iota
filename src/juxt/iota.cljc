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
  (as-test-function [s] #(get % s)))

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

(defmacro given [v & body]
  (let [t (gensym)]
    `(do
       (let [~t ~v]
         ~@(for [[a b c] (partition 3 body)]
             (case b
               ;; Equals?
               := `(is (= ((as-test-function ~a) ~t) ~c))
               :!= `(is (not= ((as-test-function ~a) ~t) ~c))

               ;; Schema checks
               :- `(is (nil? (s/check ~c ((as-test-function ~a) ~t))))
               :!- `(is (not (nil? (s/check ~c ((as-test-function ~a) ~t)))))

               ;; Is?
               :? `(is (~c ((as-test-function ~a) ~t)))
               :!? `(is (not (~c ((as-test-function ~a) ~t))))

               ;; Matches regex?
               :# `(is (re-matches (re-pattern ~c) ((as-test-function ~a) ~t)))
               :!# `(is (not (re-matches (re-pattern ~c) ((as-test-function ~a) ~t))))

               ;; Is superset?
               (:> :⊃) `(is (set/superset? (set ((as-test-function ~a) ~t)) (set ~c)))
               (:!> :⊅) `(is (not (set/superset? (set ((as-test-function ~a) ~t)) (set ~c))))

               ;; Is subset?
               (:< :⊂) `(is (set/subset? (set ((as-test-function ~a) ~t)) (set ~c)))
               (:!< :⊄) `(is (not (set/subset? (set ((as-test-function ~a) ~t)) (set ~c))))

               ;; Is an instance of
               :instanceof `(is (instance? ~c ((as-test-function ~a) ~t)))
               :!instanceof `(is (not (instance? ~c ((as-test-function ~a) ~t))))
               ))))))
