(ns clojask-io.output
  (:require [clojure.string :as str]
            [clojask-io.core :refer :all]
            [dk.ative.docjure.spreadsheet :as ds]))

(defn write-csv
  "output to a csv file using a collection of collections"
  [writer seq sep]
  (doseq [row seq]
    (.write writer (str (str/join sep row) "\n"))))

(defn write-excel
  "Create an excel file and write the vectors to it."
  [path sheet doc]
  (ds/save-workbook! path (ds/create-workbook sheet doc))
  nil)

;; ======  deprecated ======
(defn get-output-func
  "get the corresponding output function based on the file format"
  [format]
  (when-not (.contains excel-format format)
    (let [sep (get format-output-sep-map format ",")]
      (fn [wtr seq] (write-csv wtr seq sep)))))
