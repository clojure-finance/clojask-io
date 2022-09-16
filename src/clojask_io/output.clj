(ns clojask-io.output
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojask-io.core :refer :all]
            [dk.ative.docjure.spreadsheet :as ds]))

(defn write-csv
  "output to a csv file using a vector of vectors"
  [writer seq sep]
  (doseq [row seq]
    (.write writer (str (str/join sep row) "\n"))
    ))

(defn get-output-func
  "get the corresponding output function based on the file format"
  [path]
  (let [format (infer-format path)]
    (cond
      (.contains (keys format-sep-map) format) (fn [wtr seq] (write-csv wtr seq (get format-sep-map format)))
      (.contains ["xls" "xlsx"] format) nil
      :else  (fn [wtr seq] (write-csv wtr seq (get format-sep-map "csv"))))))

(defn write-excel
  "Create an excel file and write the vectors to it."
  [path sheet doc]
  (let [wb (ds/create-workbook sheet
                               doc)
        sheet (ds/select-sheet sheet wb)
        header-row (first (ds/row-seq sheet))]
    ;; (set-row-style! header-row (create-cell-style! wb {:background :yellow,
    ;;                                                    :font {:bold true}}))
    (ds/save-workbook! path wb))
  )

;; (defn write-vec
;;   "output to a vector using sequential vector of vectors"
;;   [writer seq sep]
;;   )