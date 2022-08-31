;; Copyright (c) Andrey Antukh <niwi@niwi.nz>
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

(ns datoteka.io
  "IO helpers (experimental, changes expected)."
  (:require
   [clojure.core :as c]
   [clojure.java.io :as jio]
   [datoteka.fs :as fs]
   [datoteka.proto :as pt])
  (:import
   java.io.ByteArrayInputStream
   java.io.ByteArrayOutputStream
   java.io.DataInputStream
   java.io.DataOutputStream
   java.io.OutputStream
   java.io.InputStream
   java.lang.AutoCloseable
   org.apache.commons.io.IOUtils
   org.apache.commons.io.input.BoundedInputStream))

(set! *warn-on-reflection* true)

(def ^:const default-buffer-size (* 1024 16)) ; 16 KiB default buffer

(defn input-stream?
  [s]
  (instance? InputStream s))

(defn output-stream?
  [s]
  (instance? OutputStream s))

(defn data-input-stream?
  [s]
  (instance? DataInputStream s))

(defn data-output-stream?
  [s]
  (instance? DataOutputStream s))

(defn input-stream
  [x & opts]
  (jio/input-stream x opts))

(defn output-stream
  [x & opts]
  (jio/output-stream x opts))

(defn resource
  ([x] (jio/resource x))
  ([x loader] (jio/resource x loader)))

(defn bytes-input-stream
  "Creates an instance of ByteArrayInputStream."
  [^bytes data]
  (ByteArrayInputStream. data))

(defn bounded-input-stream
  [input size & {:keys [close?] :or {close? true}}]
  (doto (BoundedInputStream. ^InputStream input ^long size)
    (.setPropagateClose close?)))

(defn data-input-stream
  ^DataInputStream
  [input]
  (DataInputStream. ^InputStream input))

(defn data-output-stream
  ^DataOutputStream
  [output]
  (DataOutputStream. ^OutputStream output))

(defn close!
  [^AutoCloseable stream]
  (.close stream))

(defn copy!
  [src dst & {:keys [offset size buffer-size]
              :or {offset 0 buffer-size default-buffer-size}}]
  (let [^bytes buff (byte-array buffer-size)]
    (if size
      (IOUtils/copyLarge ^InputStream src ^OutputStream dst (long offset) (long size) buff)
      (IOUtils/copyLarge ^InputStream src ^OutputStream dst buff))))

(defn write-to-file!
  [src dst & {:keys [size]}]
  (with-open [^OutputStream output (jio/output-stream dst)]
    (cond
      (bytes? src)
      (if size
        (with-open [^InputStream input (ByteArrayInputStream. ^bytes src)]
          (with-open [^InputStream input (BoundedInputStream. input (or size (alength ^bytes src)))]
            (copy! input output :size size)))

        (do
          (IOUtils/writeChunked ^bytes src output)
          (.flush ^OutputStream output)
          (alength ^bytes src)))

      (instance? InputStream src)
      (copy! src output :size size)

      :else
      (throw (IllegalArgumentException. "invalid arguments")))))

(defn read-as-bytes
  "Read input stream as byte array."
  [input & {:keys [size]}]
  (cond
    (instance? InputStream input)
    (with-open [output (ByteArrayOutputStream. (or size (.available ^InputStream input)))]
      (copy! input output :size size)
      (.toByteArray output))

    (fs/path? input)
    (with-open [input  (jio/input-stream input)
                output (ByteArrayOutputStream. (or size (.available input)))]
      (copy! input output :size size)
      (.toByteArray output))

    :else
    (throw (IllegalArgumentException. "invalid arguments"))))

