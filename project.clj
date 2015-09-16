;; Copyright © 2015, JUXT LTD.

(defproject juxt/iota "0.1.2"
  :description "Infix operators for test assertions"
  :url "http://github.com/juxt/iota"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[prismatic/schema "0.4.2"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [org.clojure/clojurescript "1.7.48"]]
                   :plugins [[lein-cljsbuild "1.1.0"]
                             [lein-doo "0.1.4"]]}}
  :cljsbuild {
    :builds {:test {:source-paths ["src" "test"]
                    :compiler {:output-to "resources/public/js/testable.js"
                               :main 'juxt.runner
                               :optimizations :none}}
             :node-test {:source-paths ["src" "test"]
                         :compiler {:output-to "target/testable.js"
                                    :output-dir "target"
                                    :main 'juxt.runner
                                    :optimizations :none
                                    :hashbang false
                                    :target :nodejs}}}})
