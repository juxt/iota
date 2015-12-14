;; Copyright © 2015, JUXT LTD.

(ns juxt.iota-test
  (:require
   [juxt.iota #?(:clj :refer :cljs :refer-macros) [given] :as i]
   #?(:clj  [clojure.test :refer [deftest]]
      :cljs [cljs.test :refer-macros [deftest]])
   [schema.core :as s]))

(deftest given-infix-test
  (given {:foo "foo" :bar "bar"}
    identity := {:foo "foo" :bar "bar"}
    identity :- {:foo s/Str :bar s/Str}
    :foo := "foo"
    :foo :!= "bar"
    :foo :? string?
    count := 2
    count :!= 3
    [:foo count] := (count "foo")
    [:foo count] :!= 10
    :foo :# "fo+"
    :foo :!# "fo+d"
    identity :> {:foo "foo"}
    identity :< {:foo "foo" :bar "bar" :zip "zip"})
  
  (given [1 2 3]
    first := 1
    identity :- [s/Num]
    identity :<  [1 2 3 4]
    identity :!<  [1 3 4]
    identity :> [1 2]
    identity :!> [1 4]
    count := 3
    count :- s/Num))

(deftest given-prefix-test
  (given {:foo "foo" :bar "bar"}
    (= {:foo "foo" :bar "bar"})
    (= :foo "foo")
    (= :bar "bar")
    (not= :foo "bar"))

  (given {:s #{:a :b :c}}
    (i/⊂ :s #{:a :b :c :d})
    (i/⊃ :s #{:a :b}))

  (given "abc"
    (i/re-matches "abc")
    (= first \a))

  (given {:s "abc"}
    (i/re-matches :s "abc")
    (i/instance? :s String)
    (i/not-instance? :s Long)))


