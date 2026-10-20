(ns clojask-io.input-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojask-io.input :as input]
            [clojask-io.output :as output])
  (:import [java.io BufferedReader File StringReader StringWriter]))

(defmacro ^:private with-temp-file
  "bind path to a temp file with the given suffix and content, deleted after body"
  [[path suffix content] & body]
  `(let [file# (File/createTempFile "clojask-io-test" ~suffix)
         ~path (.getPath file#)]
     (try
       (spit file# ~content)
       ~@body
       (finally
         (.delete file#)))))

(defmacro ^:private with-tracked-reader
  "bind closed? to an atom that turns true once a reader opened by io/reader closes"
  [[closed?] & body]
  `(let [~closed? (atom false)
         orig-reader# io/reader]
     (with-redefs [io/reader (fn [x# & opts#]
                               (let [r# (apply orig-reader# x# opts#)]
                                 (proxy [BufferedReader] [r#]
                                   (close []
                                     (reset! ~closed? true)
                                     (proxy-super close)))))]
       ~@body)))

(defn- write-rows
  [output rows]
  (let [w (StringWriter.)]
    (output w rows)
    (str w)))

(deftest closing-line-seq-closes-on-exhaustion
  (let [closed? (atom false)
        reader (proxy [BufferedReader] [(StringReader. "a,b\n1,2\n")]
                 (close []
                   (reset! closed? true)
                   (proxy-super close)))
        lines (#'input/closing-line-seq reader)]
    (is (= ["a,b" "1,2"] (doall lines)))
    (is @closed?)))

(deftest csv-local-closes-reader-when-fully-consumed
  (with-temp-file [path ".csv" "a,b\n1,2\n3,4\n"]
    (with-tracked-reader [closed?]
      (let [{:keys [data]} (input/csv-local path)]
        (is (= [["a" "b"] ["1" "2"] ["3" "4"]] (doall data)))
        (is @closed?)))))

(deftest csv-local-close-fn-closes-after-partial-consumption
  (with-temp-file [path ".csv" "a,b\n1,2\n3,4\n"]
    (with-tracked-reader [closed?]
      (let [{:keys [data close]} (input/csv-local path)]
        (is (= ["a" "b"] (first data)))
        (is (not @closed?))
        (close)
        (is @closed?)))))

(deftest close-fn-is-idempotent-after-exhaustion
  (with-temp-file [path ".csv" "a,b\n1,2\n"]
    (let [{:keys [data close]} (input/csv-local path)]
      (doall data)
      (is (nil? (close))))))

(deftest read-file-known-format-uses-format-separator-silently
  (with-temp-file [path ".csv" "a|b,c\n1|2,3\n"]
    (is (= "" (with-out-str
                (is (= [["a|b" "c"] ["1|2" "3"]] (vec (:data (input/read-file path))))))))))

(deftest read-file-explicit-sep-is-literal
  (with-temp-file [path ".csv" "a|b\n1|2\n"]
    (is (= [["a" "b"] ["1" "2"]] (vec (:data (input/read-file path :sep "|")))))))

(deftest read-file-dat-splits-on-runs-of-spaces
  (with-temp-file [path ".dat" "a  b   c\n1 2  3\n"]
    (let [{:keys [data output]} (input/read-file path :output true)]
      (is (= [["a" "b" "c"] ["1" "2" "3"]] (vec data)))
      (is (= "1 2\n" (write-rows output [["1" "2"]]))))))

(deftest read-file-unknown-format-infers-separator
  (with-temp-file [path ".foo" "a|b|c\n1|2|3\n"]
    (let [res (atom nil)]
      (is (re-find #"WARNING" (with-out-str (reset! res (input/read-file path :output true)))))
      (is (= [["a" "b" "c"] ["1" "2" "3"]] (vec (:data @res))))
      (is (= "x|y\n" (write-rows (:output @res) [["x" "y"]]))))))

(deftest read-file-wrap-keeps-values-shorter-than-two-wrappers
  (with-temp-file [path ".csv" "\"a\",\",\"\"\n"]
    (is (= [["a" "\"" ""]] (vec (:data (input/read-file path :wrap "\"")))))))

(deftest read-file-rejects-excel-and-unsupported-formats
  (is (thrown-with-msg? Exception #"read-excel" (input/read-file "missing.xlsx")))
  (is (thrown-with-msg? Exception #"parquet is not supported" (input/read-file "missing.parquet"))))

(deftest read-excel-reports-size
  (with-temp-file [path ".xlsx" ""]
    (output/write-excel path "S" [["a" "b"] [1.0 2.0]])
    (let [res (input/read-excel path "S" :stat true)]
      (is (= [["a" "b"] [1.0 2.0]] (map vec (:data res))))
      (is (= (.length (io/file path)) (:size res)))
      (is (nil? ((:close res)))))))
