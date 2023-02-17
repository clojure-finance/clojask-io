(ns clojask-io.delimiter)

(require '[clojure.string :as str]
         '[clojure.java.io])


(defn format-delimiter
  [delimiter]
  (def result [])
  (doseq [character (str/split delimiter #"")]
    (def result (conj result (str "[" character "]" ))))
  (re-pattern (str/join result)))

;;this function is for formatting delimiters from string into regular expressions to be passed into str/split
;;eg. (format-delimiter "@!?") will return "[@][!][?]"
;;this can avoid characters in delimiters such as *, ?, etc. to be interpreted as re symbols, eg.(? as zero or one occurrence of the previous character)
;;eg. (str/split "123@!?456" #"[@][!][?]") returns ["123" "456"], but (str/split "123@!?456" #"@!?") returns ["123" "?456"]

(defn get-one-string
  [vector-of-str, str-len, start-pos]
  (loop [len-count 0 result [] curr-pos start-pos]
    (if (= str-len len-count)
      (str/join result)
      (recur (inc len-count) (conj result (nth vector-of-str curr-pos)) (inc curr-pos)))))

(defn get-all-lenx-string
  [vector-of-str,max-len]
  (loop [result [] curr-len 1]
    (if (= curr-len (+ 1 max-len))
      result
      (recur (conj result
                   (loop [result-of-a-len [] curr-pos 0]
                     (if (= curr-pos (- (count vector-of-str) (- curr-len 1)))
                       result-of-a-len
                       (recur (conj result-of-a-len (get-one-string vector-of-str curr-len curr-pos)) (inc curr-pos))))) (inc curr-len)))))

(defn read-with-escape
  [string-of-a-row]
  (def result-vec [])
  (def quote-flag false)
  (def slash-flag false)
  (doseq [item (str/split string-of-a-row #"")]
    (if quote-flag
      (when (= item "\"") (def quote-flag false))
      (if slash-flag
        (def slash-flag false)
        (if (= item "\\")
          (def slash-flag true)
          (if (= item "\"")
            (def quote-flag true)
            (def result-vec (conj result-vec item)))))))
  result-vec)



(defn my-frequencies
  [string-of-a-row,max-len]
  (frequencies (flatten (get-all-lenx-string (read-with-escape string-of-a-row) max-len))))

(defn intersection
  "Return a set that is the intersection of the input sets"
  ([s1 s2]
   (def result {})
   (if (= s1 {})
     (def result s2)
     (doseq [item (keys s1)]
       (when (and (contains? s2 item) (= (get s1 item) (get s2 item)))
         (def result (assoc result item (get s1 item))))))
   result))

(defn check-delimiter [x,y,string-of-a-row]
  (if  (= false (str/includes? (str/replace string-of-a-row x "") y))
    true
    false))

(defn reduce-candidates  [map-of-candidates,string-of-a-row]
  (def updated  map-of-candidates)
  (doseq [x (keys updated)
          y (keys updated)]
    (when (and (not= x y) (str/includes? x y)
               (check-delimiter x y  string-of-a-row))
      (def updated (dissoc updated y))))
  updated)

(defn get-delimiter-main
  "reads in specified number of lines  and stores frequencies of each line in a vector"
  ([name,max-len]
   (with-open [rdr (clojure.java.io/reader name)]
     (loop [count_ 0 curr  {}  result {}  temp-seq (line-seq rdr)]
       (if (or (= temp-seq ())  (= 1 (count result)) (= 1000 count_)) 
           (if (= 1 (count result)) (first (keys result))
               (do
                 (println "warning: there are multiple potential delimiters, and the one with most occurrences have been returned")
                 (first (last (sort-by second result))))) 
           (recur (inc count_) (my-frequencies (first temp-seq) max-len)  (reduce-candidates (intersection result curr) (first (line-seq rdr))) (drop 1 temp-seq))))))
  ([name]
   (with-open [rdr (clojure.java.io/reader name)]
     (loop [count_ 0 curr  {}  result {}  temp-seq (line-seq rdr)]
       (if (or (= temp-seq ())  (= 1 (count result)) (= 3000 count_)) 
           (if (= 1 (count result)) (first (keys result))
               (do
                 (println "warning: there are multiple potential delimiters, and the with most occurrences have been returned")
                 (first (last (sort-by second result))))) 
           (recur (inc count_) (my-frequencies (first temp-seq) 3)  (reduce-candidates (intersection result curr) (first (line-seq rdr))) (drop 1 temp-seq)))))))

(defn get-delimiter
  ([name,max-len]
   (try
     (get-delimiter-main name max-len)
     (catch Exception e (println (str "caught exception: " (.getMessage e))))))
  ([name]
   (try
     (get-delimiter-main name)
     (catch Exception e (println (str "caught exception: " (.getMessage e))))))
   )
