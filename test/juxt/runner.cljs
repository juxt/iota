(ns juxt.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [juxt.iota-test]))

(doo-tests 'juxt.iota-test)
