# Clojask-io

A Clojure library designed to extend the file support for [Clojask](https://github.com/clojure-finance/clojask). This library can also be used alone to read in and output dataset files.

## Installation

Available on Clojars: [![Clojars Project](https://img.shields.io/clojars/v/com.github.clojure-finance/clojask-io.svg)](https://clojars.org/com.github.clojure-finance/clojask-io)

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

{:data `a lazy sequence of vectors representing each row` [:size `the size in byte`] [:output `output function`]}



#### `read-excel`

Read in an excel file as lazy sequence. Optionally, provide size of the file.

*A simplified wrapper function of [Docjure](https://github.com/mjul/docjure). The excel file should be smaller than memory size.* 

| Argument | Type    | Function                                                     | Remarks                                                      |
| -------- | ------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `path`   | String  | Indicates where to find the file (either on local machine or online) | - For local files, absolute / relative path of the file<br />- For online resources, url of the resources |
| `sheet`  | String  | Name of the sheet                                            |                                                              |
| [`stat`] | Boolean | Whether to get the size of the file                          | If true, the return value will add a :size key-value pair in unit of bytes. Size value will be `nil` if cannot be retrieved. |

**Return**

{:data `a lazy sequence of vectors representing each row` [:size `the size in byte`]}

----

### NS: clojask-io.output

#### `write-csv`

Synchronously write a collection of collections to a csv-like file.

| Argument    | Type                     | Function                                     | Remarks                                                      |
| ----------- | ------------------------ | -------------------------------------------- | ------------------------------------------------------------ |
| `writer`    | `java.io.BufferedWriter` | The writer initialized to the output file    |                                                              |
| `sequence`  | Collection               | The output content                           | Should a two-dimensional collection                          |
| `separator` | String                   | The separator between values in the same row | If true, the return value will add a :size key-value pair in unit of bytes. Size value will be `nil` if cannot be retrieved. |

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
| `sequence` | Collection | The output content                | Should a two-dimensional collection           |

**Return**

`nil`

-----------------------

Copyright © 2022 Clojask-io
