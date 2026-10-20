(ns clojask-io.delimiter
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn format-delimiter
  "Return a regex matching delimiter literally, so characters such as | * ?
   are not interpreted as regex syntax.
   e.g. (str/split \"123@!?456\" (format-delimiter \"@!?\")) => [\"123\" \"456\"]"
  [delimiter]
  (re-pattern (java.util.regex.Pattern/quote delimiter)))

(def ^:private sample-size 1000)

;; tried in this order when several candidates remain
(def ^:private preferred [", " "," "\t" ";" "|" " "])

(defn- strip-quoted
  "remove quoted values and backslash-escaped characters, so delimiters
   inside values are not counted"
  [row]
  (str/replace row #"\"[^\"]*\"?|\\.?" ""))

(defn- substring-frequencies
  "occurrences of each substring of 1 to max-len characters that contains no
   letter or digit"
  [row max-len]
  (frequencies
   (for [len (range 1 (inc max-len))
         start (range (inc (- (count row) len)))
         :let [s (subs row start (+ start len))]
         :when (not (re-find #"[\p{L}\p{N}]" s))]
     s)))

(defn- consistent-candidates
  "the substrings that occur equally often on every row"
  [rows max-len]
  (reduce (fn [candidates freqs]
            (into {} (filter (fn [[s n]] (= n (get freqs s)))) candidates))
          (map #(substring-frequencies % max-len) rows)))

(defn- drop-redundant
  "drop each candidate that only occurs as part of a longer candidate,
   e.g. \",\" and \" \" when the delimiter is \", \""
  [candidates rows]
  (let [ks (keys candidates)
        redundant? (fn [y]
                     (some (fn [x]
                             (and (not= x y)
                                  (str/includes? x y)
                                  (not-any? #(str/includes? (str/replace % x "") y) rows)))
                           ks))]
    (apply dissoc candidates (filter redundant? ks))))

(defn get-delimiter
  "Infer the delimiter of a delimited text file: a substring of up to max-len
   characters (default 3), without letters or digits, that occurs equally
   often on each of the first 1000 non-blank lines (quoted values ignored).
   Common delimiters are preferred, otherwise the most frequent candidate.
   Returns nil if there is no candidate or the file cannot be read."
  ([name]
   (get-delimiter name 3))
  ([name max-len]
   (try
     (with-open [rdr (io/reader name)]
       (let [rows (->> (line-seq rdr)
                       (remove str/blank?)
                       (take sample-size)
                       (mapv strip-quoted))
             candidates (when (seq rows)
                          (drop-redundant (consistent-candidates rows max-len) rows))]
         (when (seq candidates)
           (or (some (set (keys candidates)) preferred)
               (key (first (sort-by (juxt (comp - val) key) candidates)))))))
     (catch Exception _ nil))))
