(defproject com.github.clojure-finance/clojask-io "1.0.3"
  :description "A Clojure library designed to extend the file support for Clojask. This library can also be used alone to read in and output dataset files."
  :url "https://clojure-finance.github.io/clojask-website"
  :license {:name "MIT"
            :url "https://github.com/clojure-finance/clojask-io/blob/main/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clojure-interop/java.net "1.0.5"]
                 [com.netflix.pigpen/pigpen "0.3.3"]
                 [com.netflix.pigpen/pigpen-parquet-pig "0.3.3"]]
  :repl-options {:init-ns clojask-io.core})
