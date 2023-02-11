(ns clojask-io.input
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [jdk.net.URL :refer [->url open-connection]]
            [jdk.net.URLConnection :refer [get-content-length]]
            [clojask-io.output :refer :all]
            [clojask-io.core :refer :all]
            [clojask-io.delimiter]
            [dk.ative.docjure.spreadsheet :as excel]))


(defn- get-online-size
  "get the size of the response file"
  [url]
  (try
    (let [url (->url url)
          conn (open-connection url)]
      (get-content-length conn))
    (catch Exception e nil))
  )


(defn- get-local-size
  [path]
  (.length (io/file path)))

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
      {:clojask-io true :path path :data data :size (get-local-size path)}
      {:clojask-io true :path path :data data})))

(defn csv-online
  [path & {:keys [sep stat wrap] :or {sep #"," stat false wrap nil}}]
  (let [data (csv-local path :sep sep :stat false :wrap wrap)]
    (if stat
      (assoc data :size (get-online-size path))
      data)))

(defn- is-online
  [path]
  (or (str/starts-with? path "https://") (str/starts-with? path "http://")))

(defn read-file
  "Lazily read a dataset file (csv, txt, dat, tsv, tab) into a vector of vectors"
  [path & {:keys [sep format stat wrap output] :or {sep nil format nil stat false wrap nil output false}}]
  (let [format (or format (infer-format path))
        sep (or (clojask-io.delimiter/get-delimiter path) sep (get format-sep-map format) ",")]
    (if (.contains ["piquet" "dta"] format)
      ;; not supported type
      (do
        (throw (Exception. (str "ERROR: The file format " format " is not supported.")))
        nil)
      ;; ["csv" "txt" "dat" "tsv" "tab" nil]
      ;; (try
      (if (.contains excel-format format)
        (do
          (throw (Exception. (str "ERROR: The file format " format " is not supported. Please try using function read-excel.")))
          nil)
        (do
          (if (or (= format nil) (not (.contains gen-format format))) (println "WARNING: The format of the file cannot be inferred. Use \"csv\" by default"))
          (if (or (is-online path))
            (if output
              (assoc (csv-online path :sep sep :stat stat :wrap wrap) :output (fn [wtr seq] (write-csv wtr seq sep)))
              (csv-online path :sep sep :stat stat :wrap wrap))
            (if output
              (assoc (csv-local path :sep sep :stat stat :wrap wrap) :output (fn [wtr seq] (write-csv wtr seq sep)))
              (csv-local path :sep sep :stat stat :wrap wrap)))))
        ;; (catch Exception e 
        ;;   (do
        ;;     (throw (Exception. "Error in decoding the file. Make sure you specified the correct seperator." e)))))
      ))
  )

(defn excel-local
  [path sheet stat]
  (let [data (->> (excel/load-workbook path)
                  (excel/select-sheet sheet)
                  (excel/row-seq)
                  (remove nil?)
                  (map excel/cell-seq)
                  (map #(map excel/read-cell %)))]
    (if stat
      {:clojask-io true :path path :data data :stat (get-local-size path)}
      {:clojask-io true :path path :data data})))

(defn excel-online
  [path sheet stat]
  (let [url (->url path)
        data (with-open [stream (io/input-stream url)]
               (->> (excel/load-workbook stream)
                    (excel/select-sheet sheet)
                    (excel/row-seq)
                    (remove nil?)
                    (map excel/cell-seq)
                    (map #(map excel/read-cell %))))]
    (if stat
      {:clojask-io true :path path :data data :stat (get-online-size path)}
      {:clojask-io true :path path :data data})))

(defn read-excel
  "Read an excel sheet as a vector of vectors (not lazy).\n
   More specified input can be found in https://github.com/mjul/docjure ."
  [path sheet & {:keys [stat] :or {stat false}}]
  (if (is-online path)
    (excel-online path sheet stat)
    (excel-local path sheet stat)))
