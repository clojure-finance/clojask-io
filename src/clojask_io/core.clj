(ns clojask-io.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [pigpen.core :as pig]
            [pigpen.parquet :as pqt]))

(def format-sep-map {"csv" ","
                     "txt" ", "
                     "dat" " +"
                     "tsv" "\t"
                     "tab" "\t"})

(def gen-format ["csv" "txt" "dat" "tsv" "tab"])
(def excel-format ["xls" "xlsx"])

(defn infer-format
  "infer the file format from a path"
  [path]
  (let [index (str/last-index-of path ".")
        format (if (not= index nil) (subs path (inc (str/last-index-of path "."))) nil)]
    format))

(defn is-general
  [path]
  (.contains gen-format (infer-format path)))

(defn is-excel
  [path]
  (.contains excel-format (infer-format path)))

