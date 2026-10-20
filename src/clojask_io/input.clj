(ns clojask-io.input
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojask-io.output :refer :all]
            [clojask-io.core :refer :all]
            [clojask-io.delimiter]
            [dk.ative.docjure.spreadsheet :as excel]))

(defn- get-online-size
  "get the size of the response file, nil if the server does not report it"
  [url]
  (try
    (let [conn (.openConnection (.toURL (java.net.URI. url)))]
      (try
        (let [size (.getContentLengthLong conn)]
          (when-not (neg? size) size))
        (finally
          (when (instance? java.net.HttpURLConnection conn)
            (.disconnect ^java.net.HttpURLConnection conn)))))
    (catch Exception e nil)))

(defn- get-local-size
  [path]
  (.length (io/file path)))

(defn- closing-line-seq
  "like line-seq, but closes the reader when the sequence is fully consumed"
  [^java.io.BufferedReader reader]
  (lazy-seq
   (if-let [line (.readLine reader)]
     (cons line (closing-line-seq reader))
     (do (.close reader) nil))))

(defn csv-local
  "read in a local csv dataset\n
   :data is lazy; the underlying reader closes itself when :data is fully
   consumed, and :close closes it early if consumption stops before the end"
  [path & {:keys [sep stat wrap] :or {sep #"," stat false wrap nil}}]
  (let [sep (if (string? sep) (clojask-io.delimiter/format-delimiter sep) sep)
        reader (io/reader path)
        data (closing-line-seq reader)
        data (map #(str/split % sep -1) data)
        data (if (= wrap nil)
               data
               (let [wrap-len (count wrap)]
                 (map #(mapv
                        (fn [value]
                          (if (and (>= (count value) (* 2 wrap-len))
                                   (str/starts-with? value wrap)
                                   (str/ends-with? value wrap))
                            (subs value wrap-len (- (count value) wrap-len))
                            value))
                        %) data)))
        res {:clojask-io true :path path :data data :close (fn [] (.close reader))}]
    (if stat
      (assoc res :size (get-local-size path))
      res)))

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
  "Lazily read a dataset file (csv, txt, dat, tsv, tab) as a sequence of row vectors.\n
   The separator is :sep if given, else the default of the format, else
   inferred from the file content, else \",\""
  [path & {:keys [sep format stat wrap output] :or {sep nil format nil stat false wrap nil output false}}]
  (let [format (or format (infer-format path))]
    (cond
      (.contains ["parquet" "dta"] format)
      (throw (Exception. (str "ERROR: The file format " format " is not supported.")))

      (.contains excel-format format)
      (throw (Exception. (str "ERROR: The file format " format " is not supported. Please try using function read-excel.")))

      :else
      (let [sep (or sep (get format-sep-map format) (clojask-io.delimiter/get-delimiter path) ",")
            _ (when-not (.contains gen-format format)
                (println (str "WARNING: The format of the file cannot be inferred. Reading it as csv with separator " (pr-str sep))))
            res (if (is-online path)
                  (csv-online path :sep sep :stat stat :wrap wrap)
                  (csv-local path :sep sep :stat stat :wrap wrap))]
        (if output
          (let [out-sep (if (string? sep) sep (get format-output-sep-map format ","))]
            (assoc res :output (fn [wtr seq] (write-csv wtr seq out-sep))))
          res)))))

(defn excel-local
  [path sheet stat]
  (let [data (->> (excel/load-workbook path)
                  (excel/select-sheet sheet)
                  (excel/row-seq)
                  (remove nil?)
                  (map excel/cell-seq)
                  (map #(map excel/read-cell %)))
        res {:clojask-io true :path path :data data :close (fn [] nil)}]
    (if stat
      (assoc res :size (get-local-size path))
      res)))

(defn excel-online
  [path sheet stat]
  (let [data (with-open [stream (io/input-stream path)]
               (->> (excel/load-workbook stream)
                    (excel/select-sheet sheet)
                    (excel/row-seq)
                    (remove nil?)
                    (map excel/cell-seq)
                    (map #(map excel/read-cell %))))
        res {:clojask-io true :path path :data data :close (fn [] nil)}]
    (if stat
      (assoc res :size (get-online-size path))
      res)))

(defn read-excel
  "Read an excel sheet as a sequence of rows; the whole workbook is loaded into memory.\n
   More specified input can be found in https://github.com/mjul/docjure ."
  [path sheet & {:keys [stat] :or {stat false}}]
  (if (is-online path)
    (excel-online path sheet stat)
    (excel-local path sheet stat)))
