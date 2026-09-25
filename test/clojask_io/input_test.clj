(ns clojask-io.input-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojask-io.input :as input])
  (:import [java.io BufferedReader StringReader]))

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
  (let [file (java.io.File/createTempFile "clojask-io-test" ".csv")
        closed? (atom false)
        orig-reader io/reader]
    (try
      (spit file "a,b\n1,2\n3,4\n")
      (with-redefs [io/reader (fn [x & opts]
                                (let [r (apply orig-reader x opts)]
                                  (proxy [BufferedReader] [r]
                                    (close []
                                      (reset! closed? true)
                                      (proxy-super close)))))]
        (let [{:keys [data]} (input/csv-local (.getPath file))]
          (is (= [["a" "b"] ["1" "2"] ["3" "4"]] (doall data)))
          (is @closed?)))
      (finally
        (.delete file)))))

(deftest csv-local-close-fn-closes-after-partial-consumption
  (let [file (java.io.File/createTempFile "clojask-io-test" ".csv")
        closed? (atom false)
        orig-reader io/reader]
    (try
      (spit file "a,b\n1,2\n3,4\n")
      (with-redefs [io/reader (fn [x & opts]
                                (let [r (apply orig-reader x opts)]
                                  (proxy [BufferedReader] [r]
                                    (close []
                                      (reset! closed? true)
                                      (proxy-super close)))))]
        (let [{:keys [data close]} (input/csv-local (.getPath file))]
          (is (= ["a" "b"] (first data)))
          (is (not @closed?))
          (close)
          (is @closed?)))
      (finally
        (.delete file)))))

(deftest close-fn-is-idempotent-after-exhaustion
  (let [file (java.io.File/createTempFile "clojask-io-test" ".csv")]
    (try
      (spit file "a,b\n1,2\n")
      (let [{:keys [data close]} (input/csv-local (.getPath file))]
        (doall data)
        (is (nil? (close))))
      (finally
        (.delete file)))))
