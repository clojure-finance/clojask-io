(ns clojask-io.input
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [jdk.net.URL :refer [->url open-connection]]
            [jdk.net.URLConnection :refer [get-content-length]]
            [clojask-io.output :refer :all]))


(defn get-online-size
  "get the size of the response file"
  [url]
  (try
    (let [url (->url url)
          conn (open-connection url)]
      (get-content-length conn))
    (catch Exception e nil))
  )

(defn csv-local
  "read in a local csv dataset"
  [path & {:keys [sep stat wrap] :or {sep #"," stat false wrap nil}}]
  (let [sep (if (string? sep) (re-pattern sep) sep)
        reader (io/reader path)
        data (line-seq reader)
        data (map #(str/split % sep -1) data)
        data (if (= wrap nil)
               data
               (let [wrap-len (count wrap)]
                 (map #(mapv
                        (fn [value]
                          (let [value (if (and (str/ends-with? value wrap) (str/starts-with? value wrap))
                                        (subs value wrap-len (- (count value) wrap-len))
                                        value)]
                            value))
                        %) data)))]
    (if stat
      {:clojask-io true :data data :size (.length (io/file path))}
      {:clojask-io true :data data})))

(defn csv-online
  [path & {:keys [sep stat wrap] :or {sep #"," stat false wrap nil}}]
  (let [data (csv-local path :sep sep :stat false :wrap wrap)]
    (if stat
      (assoc data :size (get-online-size path))
      data)))

(defn infer-format
  "infer the file format from a path"
  [path]
  (let [index (str/last-index-of path ".")
        format (if (not= index nil) (subs path (inc (str/last-index-of path "."))) nil)]
    format))

(def format-sep-map {"csv" ","
                     "txt" ", "
                     "dat" " +"
                     "tsv" "\t"})

(defn read-file
  "Lazily read a dataset file into a vector of vectors"
  [path & {:keys [sep format stat wrap output] :or {sep nil format nil stat false wrap nil output false}}]
  (let [format (or format (infer-format path))
        sep (or sep (get format-sep-map format) ",")]
    (if (.contains ["piquet" "xls" "xlsx" "dta"] format)
      ;; not supported type
      (do
        (throw (Exception. (str "ERROR: The file format " format " is not supported.")))
        nil)
      ;; ["csv" "txt" "dat" "tsv" "tab" nil]
      (try
        (do
        (if (or (= format nil) (not (.contains ["csv" "txt" "dat" "tsv" "tab"] format))) (println "WARNING: The format of the file cannot be inferred. Use \"csv\" by default"))
        (if (or (str/starts-with? path "https://") (str/starts-with? path "http://"))
          (if output 
            (assoc (csv-online path :sep sep :stat stat :wrap wrap) :output (fn [wtr seq] (write-csv wtr seq sep)))
            (csv-online path :sep sep :stat stat :wrap wrap))
          (if output
            (assoc (csv-local path :sep sep :stat stat :wrap wrap) :output (fn [wtr seq] (write-csv wtr seq sep)))
            (csv-local path :sep sep :stat stat :wrap wrap))))
        (catch Exception e 
          (do
            (throw (Exception. (println "Error in decoding the file. Make sure you specified the correct seperator.") e)))))))
  )