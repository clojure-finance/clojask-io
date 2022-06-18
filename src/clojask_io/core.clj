(ns clojask-io.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojask-io.input :refer :all]))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
