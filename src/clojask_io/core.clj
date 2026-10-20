(ns clojask-io.core
  (:require [clojure.string :as str]))

;; a String separator is matched literally, a Pattern as a regex
(def format-sep-map {"csv" ","
                     "txt" ", "
                     "dat" #" +"
                     "tsv" "\t"
                     "tab" "\t"})

;; separators for writing: a regex cannot be written, so dat writes one space
(def format-output-sep-map (assoc format-sep-map "dat" " "))

(def gen-format ["csv" "txt" "dat" "tsv" "tab"])
(def excel-format ["xls" "xlsx"])

(defn infer-format
  "infer the file format from the extension of the last path segment,
   otherwise return nil"
  [path]
  (let [file-name (last (str/split path #"[/\\]"))
        index (some-> file-name (str/last-index-of "."))]
    (when index
      (subs file-name (inc index)))))

(defn is-general
  [path]
  (.contains gen-format (infer-format path)))

(defn is-excel
  [path]
  (.contains excel-format (infer-format path)))

(defn supports
  "whether a format such as \"csv\" or \"xlsx\" can be read and written;
   nil (format unknown) is read as csv"
  [format]
  (or (nil? format) (.contains gen-format format) (.contains excel-format format)))
