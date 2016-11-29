;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions are met:
;;
;; * Redistributions of source code must retain the above copyright notice, this
;;   list of conditions and the following disclaimer.
;;
;; * Redistributions in binary form must reproduce the above copyright notice,
;;   this list of conditions and the following disclaimer in the documentation
;;   and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
;; AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
;; IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
;; DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
;; FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
;; DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
;; SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
;; CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
;; OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
;; OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns datoteka.impl
  "Implementation details and helpers."
  (:require [datoteka.proto :as pt]
            [datoteka.core :as fs]
            [clojure.java.io :as io])
  (:import java.io.File
           java.io.ByteArrayInputStream
           java.io.InputStream
           java.net.URL
           java.net.URI
           java.nio.file.Path
           java.nio.file.Paths
           java.nio.file.Files))

(extend-protocol pt/IContent
  String
  (-input-stream [v]
    (let [data (.getBytes v "UTF-8")]
      (ByteArrayInputStream. ^bytes data)))

  Object
  (-input-stream [v]
    (io/input-stream v)))

(extend-protocol pt/IUri
  URI
  (-uri [v] v)

  String
  (-uri [v] (URI. v)))

(def ^:private empty-string-array
  (make-array String 0))

(extend-protocol pt/IPath
  Path
  (-path [v] v)

  URI
  (-path [v] (Paths/get v))

  URL
  (-path [v] (Paths/get (.toURI v)))

  String
  (-path [v] (Paths/get v empty-string-array))

  clojure.lang.Sequential
  (-path [v]
    (reduce #(.resolve %1 %2)
            (pt/-path (first v))
            (map pt/-path (rest v)))))

(defn- path->input-stream
  [^Path path]
  (Files/newInputStream path fs/read-open-opts))

(defn- path->output-stream
  [^Path path]
  (Files/newOutputStream path fs/write-open-opts))

(extend-type Path
  io/IOFactory
  (make-reader [path opts]
    (let [^InputStream is (path->input-stream path)]
      (io/make-reader is opts)))
  (make-writer [path opts]
    (let [^OutputStream os (path->output-stream path)]
      (io/make-writer os opts)))
  (make-input-stream [path opts]
    (let [^InputStream is (path->input-stream path)]
      (io/make-input-stream is opts)))
  (make-output-stream [path opts]
    (let [^OutputStream os (path->output-stream path)]
      (io/make-output-stream os opts))))
