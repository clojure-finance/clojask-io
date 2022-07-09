# Clojask-io

A Clojure library designed to extend the file support for [Clojask](https://github.com/clojure-finance/clojask). This library can also be used alone to read in and output dataset files.

### Installation

Available on Clojars: [![Clojars Project](https://img.shields.io/clojars/v/com.github.clojure-finance/clojask-io.svg)](https://clojars.org/com.github.clojure-finance/clojask-io)

### APIs

#### NS: clojask-io.input

#### `read-file`

Read in a file as lazy sequence. Optionally, provide size of the file, corresponding output functions.

| Argument   | Type                                     | Function                                                     | Remarks                                                      |
| ---------- | ---------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `path`     | String                                   | Indicates where to find the file (either on local machine or online) | - For local files, absolute / relative path of the file<br />- For online resources, url of the resources |
| [`format`] | String                                   | The format of the file                                       | Will be inferred from the path suffix if not provided. Will imply the separator (`sep`) based on pre-setting. |
| [`sep`]    | String / java.util.regex.Pattern (regex) | The separator of each row of the dataset file                |                                                              |
| [`wrap`]   | String                                   | Wrapper of each value                                        | Sometimes, the file will wrap each value some punctuations, e.g `""` / `''`. Can remove them automatically by setting this argument. Does not support asymmetric wrappers. |
| [`stat`]   | Boolean                                  | Whether to get the size of the file                          | If true, the return value will add a :size key-value pair in unit of bytes. Size value will be `nil` if cannot be retrieved. |
| [`output`] | Boolean                                  | Whether to also return the corresponding output function     | If true, the return value will add a :output key-value pair. |

**Return**

{:data `a lazy sequence of vectors representing each row` [:size `the size in byte`] [:output `output function`]}



#### NS: clojask-io.output



-----------------------

Copyright © 2022 Clojask-io
