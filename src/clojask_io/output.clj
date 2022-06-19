(ns clojask-io.output
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn write-csv
  "output to a csv file using a vector of vectors"
  [writer seq sep]
  (doseq [row seq]
    (.write writer (str (str/join sep row) "\n"))
    ))