# Clojask-io

A Clojure library designed to extend the file support for [Clojask](https://github.com/clojure-finance/clojask). This library can also be used alone to read in and output dataset files.

## Installation

Available on Clojars: [![Clojars Project](https://img.shields.io/clojars/v/com.github.clojure-finance/clojask-io.svg)](https://clojars.org/com.github.clojure-finance/clojask-io)

Leiningen:

```clojure
[com.github.clojure-finance/clojask-io "1.0.7"]
```

deps.edn:

```clojure
com.github.clojure-finance/clojask-io {:mvn/version "1.0.7"}
```

## Usage

```clojure
(require '[clojask-io.input :as input])

;; :data is a lazy sequence of row vectors; the underlying reader closes
;; itself when :data is fully consumed
(let [{:keys [data]} (input/read-file "data.csv")]
  (doseq [row data]
    (println row)))

;; if you stop before the end of the file, call :close to release the reader;
;; :close is always present and safe to call more than once
(let [{:keys [data close]} (input/read-file "data.csv")]
  (let [header (first data)]
    (close)
    header))
```

## APIs

### NS: clojask-io.core

#### `supports`

Check if this library supports to read and write this format of file.

| Argument | Type   | Function                                                     | Remarks |
| -------- | ------ | ------------------------------------------------------------ | ------- |
| `format` | String | Indicates the format of type, e.g. "csv", "xls", "txt", etc. |         |

**Return**

Boolean

<br>

#### `infer-format`

Infer the file format from the path (get the substring after the last `.`).

| Argument | Type   | Function                                                     | Remarks                                                      |
| -------- | ------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `path`   | String | Indicates where to find the file (either on local machine or online) | - For local files, absolute / relative path of the file<br />- For online resources, url of the resources |

**Return**

String, such as "csv", "xls" (`nil` if fails to infer)

-----

### NS: clojask-io.input

#### `read-file`

Read in a file as lazy sequence. Optionally, provide size of the file, corresponding output functions.

*Supported file types: **csv, txt, dat, tsv, tab**. The file size can be larger than memory.* 

| Argument   | Type                                     | Function                                                     | Remarks                                                      |
| ---------- | ---------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `path`     | String                                   | Indicates where to find the file (either on local machine or online) | - For local files, absolute / relative path of the file<br/>- For online resources, url of the resources |
| [`format`] | String                                   | The format of the file                                       | Will be inferred from the path suffix if not provided. Will imply the separator (`sep`) based on pre-setting. |
| [`sep`]    | String / java.util.regex.Pattern (regex) | The separator of each row of the dataset file                |                                                              |
| [`wrap`]   | String                                   | Wrapper of each value                                        | Sometimes, the file will wrap each value some punctuations, e.g `""` / `''`. Can remove them automatically by setting this argument. Does not support asymmetric wrappers. |
| [`stat`]   | Boolean                                  | Whether to get the size of the file                          | If true, the return value will add a :size key-value pair in unit of bytes. Size value will be `nil` if cannot be retrieved. |
| [`output`] | Boolean                                  | Whether to also return the corresponding output function     | If true, the return value will add a :output key-value pair. |

**Return**

{:data `a lazy sequence of vectors representing each row` :close `0-arity function that closes the underlying reader (only needed if :data is not fully consumed; consuming :data to the end closes it automatically)` [:size `the size in byte`] [:output `output function`]}



#### `read-excel`

Read in an excel file as a sequence of rows (loaded eagerly, unlike `read-file`). Optionally, provide size of the file.

*A simplified wrapper function of [Docjure](https://github.com/mjul/docjure). The excel file should be smaller than memory size.* 

| Argument | Type    | Function                                                     | Remarks                                                      |
| -------- | ------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `path`   | String  | Indicates where to find the file (either on local machine or online) | - For local files, absolute / relative path of the file<br />- For online resources, url of the resources |
| `sheet`  | String  | Name of the sheet                                            |                                                              |
| [`stat`] | Boolean | Whether to get the size of the file                          | If true, the return value will add a :size key-value pair in unit of bytes. Size value will be `nil` if cannot be retrieved. |

**Return**

{:data `a sequence of sequences representing each row (the whole sheet is loaded into memory)` :close `0-arity no-op function (excel files are read eagerly; present for interface uniformity with read-file)` [:size `the size in byte`]}

----

### NS: clojask-io.output

#### `write-csv`

Synchronously write a collection of collections to a csv-like file.

| Argument    | Type                     | Function                                     | Remarks                                                      |
| ----------- | ------------------------ | -------------------------------------------- | ------------------------------------------------------------ |
| `writer`    | `java.io.BufferedWriter` | The writer initialized to the output file    |                                                              |
| `sequence`  | Collection               | The output content                           | Should be a two-dimensional collection                       |
| `separator` | String                   | The separator between values in the same row |                                                               |

**Implementation**

```clojure
(defn write-csv
  "output to a csv file using a collection of collections"
  [writer seq sep]
  (doseq [row seq]
    (.write writer (str (str/join sep row) "\n"))
    ))
```

**Return**

`nil`

<br>

#### `write-excel`

Synchronously write a collection of collections to an excel file.

*A simplified wrapper function of [Docjure](https://github.com/mjul/docjure).*

| Argument   | Type       | Function                          | Remarks                                       |
| ---------- | ---------- | --------------------------------- | --------------------------------------------- |
| `path`     | String     | Indicates the path of output file | Absolute / relative path of local file system |
| `sheet`    | String     | Name of the sheet                 |                                               |
| `sequence` | Collection | The output content                | Should be a two-dimensional collection        |

**Return**

`nil`

-----------------------

Copyright © 2022 Clojask-io
