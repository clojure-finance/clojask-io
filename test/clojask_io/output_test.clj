(ns clojask-io.output-test
  (:require [clojure.test :refer :all]
            [clojask-io.output :as output])
  (:import [java.io StringWriter]))

(defn- write-rows
  [output rows]
  (let [w (StringWriter.)]
    (output w rows)
    (str w)))

(deftest get-output-func-writes-the-format-separator
  (is (= "1,2\n" (write-rows (output/get-output-func "csv") [[1 2]])))
  (is (= "1\t2\n" (write-rows (output/get-output-func "tsv") [[1 2]])))
  (is (= "1 2\n" (write-rows (output/get-output-func "dat") [[1 2]])))
  (is (= "1,2\n" (write-rows (output/get-output-func nil) [[1 2]])))
  (is (= "1,2\n" (write-rows (output/get-output-func "foo") [[1 2]])))
  (is (nil? (output/get-output-func "xlsx"))))
