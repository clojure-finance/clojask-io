(ns clojask-io.core-test
  (:require [clojure.test :refer :all]
            [clojask-io.core :as core]))

(deftest infer-format-uses-last-path-segment
  (is (= "csv" (core/infer-format "data/file.csv")))
  (is (= "xlsx" (core/infer-format "https://example.com/a.b/file.xlsx")))
  (is (= "csv" (core/infer-format "C:\\data\\file.csv")))
  (is (nil? (core/infer-format "a.b/file")))
  (is (nil? (core/infer-format "file"))))

(deftest supports-takes-a-format
  (is (core/supports "csv"))
  (is (core/supports "xlsx"))
  (is (core/supports nil))
  (is (not (core/supports "parquet"))))
