(ns clojask-io.delimiter-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clojask-io.delimiter :as delimiter])
  (:import [java.io File]))

(defn- delimiter-of
  [content]
  (let [file (File/createTempFile "clojask-io-test" ".txt")]
    (try
      (spit file content)
      (delimiter/get-delimiter (.getPath file))
      (finally
        (.delete file)))))

(deftest format-delimiter-is-literal
  (is (= ["123" "456"] (str/split "123@!?456" (delimiter/format-delimiter "@!?"))))
  (doseq [d ["|" "\\" "^" "]" "." "\t"]]
    (is (= ["a" "b"] (str/split (str "a" d "b") (delimiter/format-delimiter d))))))

(deftest get-delimiter-infers-common-delimiters
  (is (= "," (delimiter-of "x,y,z\n1,2,3\n4,5,6\n")))
  (is (= "|" (delimiter-of "x|y\n1|2\n3|4\n")))
  (is (= "\t" (delimiter-of "x\ty\tz\n1\t2\t3\n")))
  (is (= ";" (delimiter-of "x;y\n1;2\n\n")))
  (is (= ", " (delimiter-of "a, b, c\n1, 2, 3\n")))
  (is (= "||" (delimiter-of "a||b\n1||2\n"))))

(deftest get-delimiter-ignores-quoted-values-and-letters
  (is (= "," (delimiter-of "\"a;b\",c\n\"1;;2\",3\n")))
  (is (= "," (delimiter-of "a1-x,b2\na3-y,b4\n"))))

(deftest get-delimiter-returns-nil-without-a-candidate
  (is (nil? (delimiter-of "")))
  (is (nil? (delimiter-of "abc\ndef\n")))
  (is (nil? (delimiter-of "a  b   c\n1 2  3\n")))
  (is (nil? (delimiter/get-delimiter "does-not-exist.csv"))))
