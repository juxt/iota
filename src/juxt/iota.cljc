;; Copyright © 2015, JUXT LTD.

(ns juxt.iota
  (:refer-clojure :exclude [re-matches instance?])
  (:require
   #?(:clj clojure.test
      :cljs [cljs.test :include-macros true])
    [clojure.set :as set]
    [schema.core :as s]
    schema.utils))

;; See the test at the end of this ns to understand the point of this code

(defprotocol TestClause
  (as-test-function [_] "Take the clause and return a function which is applied to the value under test"))

(extend-protocol TestClause
  #?(:clj clojure.lang.APersistentVector
     :cljs PersistentVector)
  ;; A function application 'path', which is simply a composed function,
  ;; left-to-right rather than right-to-left.
  (as-test-function [v] (apply comp (map as-test-function (reverse v))))

  #?(:clj clojure.lang.Keyword
     :cljs Keyword)
  (as-test-function [k] k)

  #?(:clj clojure.lang.Fn
     :cljs function)
  (as-test-function [f] f)

  #?(:clj String
     :cljs string)
  (as-test-function [s] #(get % s))

  #?(:clj Long
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

(defmacro given-infix "Given v, assert the following…"
  {:style/indent 1}
  [v & body]
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
               :# `(is (clojure.core/re-matches (re-pattern ~c) ((as-test-function ~a) ~t)))
               :!# `(is (not (clojure.core/re-matches (re-pattern ~c) ((as-test-function ~a) ~t))))

               ;; Is superset?
               (:> :⊃) `(is (set/superset? (set ((as-test-function ~a) ~t)) (set ~c)))
               (:!> :⊅) `(is (not (set/superset? (set ((as-test-function ~a) ~t)) (set ~c))))

               ;; Is subset?
               (:< :⊂) `(is (set/subset? (set ((as-test-function ~a) ~t)) (set ~c)))
               (:!< :⊄) `(is (not (set/subset? (set ((as-test-function ~a) ~t)) (set ~c))))

               ;; Is an instance of
               :instanceof `(is (clojure.core/instance? ~c ((as-test-function ~a) ~t)))
               :!instanceof `(is (not (clojure.core/instance? ~c ((as-test-function ~a) ~t))))

               ;; Every satisfies predicate
               :∀ `(is (every? ~c ((as-test-function ~a) ~t)))
               :!∀ `(is (not (every? ~c ((as-test-function ~a) ~t))))

               ))))))

;; Some people (usually died-in-the-wool Lispers) don't like the infix
;; syntax of juxt.iota/given, and think s-expression are better. They
;; make a good point. So here is an implementation juxt.iota dedicated
;; to old-school Lispers. See tests for usage details.

(defmacro re-matches [actual pattern]
  `(is (clojure.core/re-matches (re-pattern ~pattern) ~actual)))

(defmacro not-re-matches [actual pattern]
  `(is (not (clojure.core/re-matches (re-pattern ~pattern) ~actual))))

(defmacro check [actual schema]
  `(is (nil? (s/check schema actual))))

(defmacro error? [actual schema]
  `(is (schema.util/error? (s/check schema actual))))

(defmacro ⊂ [subject arg]
  `(is (set/subset? (set ~subject) (set ~arg))))

(defmacro ⊄ [subject arg]
  `(is (not (set/subset? (set ~subject) (set ~arg)))))

(defmacro ⊃ [subject arg]
  `(is (set/superset? (set ~subject) (set ~arg) )))

(defmacro ⊅ [subject arg]
  `(is (not (set/superset? (set ~subject) (set ~arg) ))))

(defmacro instance? [actual class]
  `(is (clojure.core/instance? ~class ~actual)))

(defmacro not-instance? [actual class]
  `(is (not (clojure.core/instance? ~class ~actual))))

(defmacro given-prefix
  "Check assertions against value p"
  {:style/indent 1}
  [p & assertions]
  (let [p' (gensym)]
    `(let [~p' ~p]
       ~@(for [a assertions]
           (if (and (list? a) (> (count a) 1))
             (cond
               (= (count a) 2) `(is (~(first a) ~p' ~(second a)))
               (= (count a) 3) `(is (~(first a) ((as-test-function ~(second a)) ~p') ~(last a)))
               :otherwise `(throw (ex-info "Too many arguments" {})))
             (case (if (list? a) (first a) a)
               println `(println ~p')
               ))))))

;; Some folks don't care for Unicode characters, so we provide
;; aliases. I think you can object to having to write such characters
;; but I don't think you can reasonably complain about reading them.
(def subset? #'⊂)
(def not-subset? #'⊄)
(def superset? #'⊃)
(def not-superset? #'⊅)

(defmacro given
  "Given wrapper that allows both infix and prefix forms. If there's a
  keyword in the argument list (not counting the first), then it is
  considered to be the original infix form. Call given-infix and
  given-prefix directly if you want."
  {:style/indent 1}
  [& args]
  (if (some keyword? (rest args))
    `(given-infix ~@args)
    `(given-prefix ~@args)))
