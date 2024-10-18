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
   java.io.BufferedOutputStream
   java.io.ByteArrayInputStream
   java.io.ByteArrayOutputStream
   java.io.DataInputStream
   java.io.DataOutputStream
   java.io.InputStream
   java.io.InputStreamReader
   java.io.OutputStream
   java.io.Reader
   java.io.Writer
   java.lang.AutoCloseable
   org.apache.commons.io.IOUtils
   org.apache.commons.io.input.BoundedInputStream
   org.apache.commons.io.input.BoundedInputStream$Builder
   org.apache.commons.io.input.UnsynchronizedBufferedInputStream
   org.apache.commons.io.input.UnsynchronizedBufferedInputStream$Builder
   org.apache.commons.io.input.UnsynchronizedByteArrayInputStream
   org.apache.commons.io.input.UnsynchronizedByteArrayInputStream$Builder
   org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream
   org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream$Builder))

(set! *warn-on-reflection* true)

(def ^:const default-buffer-size IOUtils/DEFAULT_BUFFER_SIZE) ;; 8 KiB

(defn coercible?
  "Check if the provided object can be coercible to input stream or
  output stream. In other workds: checks if it satisfies the
  jio/IOFactory protocol."
  [o]
  (satisfies? jio/IOFactory o))

(defn input-stream?
  "Check if provided object is an instance of InputStream."
  [s]
  (instance? InputStream s))

(defn output-stream?
  "Check if provided object is an instance of OutputStream."
  [s]
  (instance? OutputStream s))

(defn data-input-stream?
  "Check if provided object is an instance of DataInputStream."
  [s]
  (instance? DataInputStream s))

(defn data-output-stream?
  "Check if provided object is an instance of DataOutputStream."
  [s]
  (instance? DataOutputStream s))

(defn input-stream
  "Attempts to coerce its argument into an open java.io.InputStream.
  Default implementations always return a java.io.BufferedInputStream.

  Convenciency API, it forwards directly to the
  `clojure.java.io/make-input-stream`."
  [x & {:as opts}]
  (jio/make-input-stream x opts))

(defn output-stream
  "Attempts to coerce its argument into an open java.io.InputStream.
  Default implementations always return a java.io.BufferedInputStream.

  Convenciency API, it forwards directly to the
  `clojure.java.io/make-output-stream`."
  [x & {:as opts}]
  (jio/make-output-stream x opts))

(defn reader
  "Attempts to coerce its argument into an open java.io.Reader.
  Default implementations always return a java.io.BufferedReader.

  Convenciency API, it forwards directly to the
  `clojure.java.io/make-reader`."
  ^Reader
  [x & {:as opts}]
  (jio/make-reader x opts))

(defn writer
  "Attempts to coerce its argument into an open java.io.Writer.
  Default implementations always return a java.io.BufferedWriter.

  Convenciency API, it forwards directly to the
  `clojure.java.io/make-writer`."
  ^Writer
  [x & {:as opts}]
  (jio/make-writer x opts))

(defn resource
  "Returns the URL for a named resource. Use the context class loader
  if no loader is specified.
  Convenciency API, it forwards directly to the
  `clojure.java.io/resource`."
  (^java.net.URL [x] (jio/resource x))
  (^java.net.URL [x loader] (jio/resource x loader)))

(defn bytes-input-stream
  "Creates an instance of unsyncronized ByteArrayInputStream instance holding the
  provided data."
  ^InputStream
  [^bytes data & {:keys [offset size]}]
  (let [builder (doto (UnsynchronizedByteArrayInputStream$Builder.)
                  (.setByteArray data))
        builder (if offset
                  (.setOffset builder (int offset))
                  builder)
        builder (if size
                  (.setLength builder (int size))
                  builder)]
    (.get ^UnsynchronizedByteArrayInputStream$Builder builder)))

(defn bytes-output-stream
  "Creates an instance of ByteArrayOutputStream."
  ^UnsynchronizedByteArrayOutputStream
  [& {:keys [size]}]
  (let [builder (UnsynchronizedByteArrayOutputStream$Builder.)
        builder (if size
                  (.setBufferSize builder (int size))
                  builder)]
    (.get ^UnsynchronizedByteArrayOutputStream$Builder builder)))

(defn buffered-input-stream
  [input & {:keys [buffer-size] :or {buffer-size default-buffer-size}}]
  (let [builder (UnsynchronizedBufferedInputStream$Builder.)
        builder (.setInputStream ^UnsynchronizedBufferedInputStream$Builder builder
                                 ^InputStream input)
        builder (.setBufferSize ^UnsynchronizedBufferedInputStream$Builder builder
                                (int buffer-size))]
    (.get ^UnsynchronizedBufferedInputStream$Builder builder)))

(defn buffered-output-stream
  [output & {:keys [buffer-size] :or {buffer-size default-buffer-size}}]
  (BufferedOutputStream. ^OutputStream output (int buffer-size)))

(defn bounded-input-stream
  "Creates an instance of InputStream bounded to a specified size."
  ^InputStream
  [input size & {:keys [propagate-close] :or {propagate-close true}}]
  (let [builder (doto (BoundedInputStream$Builder.)
                  (.setInputStream ^InputStream input)
                  (.setMaxCount (long size))
                  (.setPropagateClose (boolean propagate-close)))]
    (.get ^BoundedInputStream$Builder builder)))

(defn data-input-stream
  ^DataInputStream
  [input]
  (DataInputStream. ^InputStream input))

(defn data-output-stream
  ^DataOutputStream
  [output]
  (DataOutputStream. ^OutputStream output))

(defn close!
  "Close any AutoCloseable resource."
  [^AutoCloseable stream]
  (.close stream))

(defn flush!
  "Flush the OutputStream"
  [^OutputStream stream]
  (.flush stream))

(defn copy!
  "Efficiently copy data from `src` (should be instance of
  InputStream) to the `dst` (which should be instance of
  OutputStream).

  You can specify the size for delimit how much bytes should be
  written to the `dst`."
  [src dst & {:keys [offset size buffer-size]
              :or {offset 0 size -1 buffer-size default-buffer-size}}]
  (let [^bytes buff (byte-array buffer-size)]
    (IOUtils/copyLarge ^InputStream src ^OutputStream dst (long offset) (long size) buff)))

(defn write!
  "Writes content from `src` to the `dst`.

  If `dst` in an OutputStream it will be closed when copy is finished,
  you can pass `:close?` option with `false` for avoid this behavior.

  If size is provided, no more than that bytes will be written to the
  `dst`."
  [src dst & {:keys [size offset close] :or {close false} :as opts}]
  (let [^OutputStream output (jio/make-output-stream dst opts)]
    (try
      (cond
        (instance? InputStream src)
        (copy! src output opts)

        ;; A faster write operation if we already have a byte array
        ;; and we don't specify the size.
        (and (bytes? src)
             (not size)
             (not offset))
        (do
          (IOUtils/writeChunked ^bytes src output)
          (alength ^bytes src))

        (string? src)
        (let [encoding (or (:encoding opts) "UTF-8")
              data     (.getBytes ^String src ^String encoding)]
          (write! data dst opts))

        :else
        (let [src (jio/make-input-stream src opts)
              _   (when offset
                    (IOUtils/skipFully ^InputStream src (long offset)))

              src (if size
                    (bounded-input-stream src size)
                    src)]
          (try
            (copy! src output)
            (finally
              (.close ^InputStream src)))))

      (finally
        (flush! output)
        (when close
          (.close ^OutputStream output))))))

(defn write-to-file!
  {:deprecated true}
  [src dst & {:as opts}]
  (write! src dst opts))

(defn read-as-bytes
  "Read all data or specified size input and return a byte array."

  [input & {:keys [size offset close] :or {close false} :as opts}]
  (let [input (jio/make-input-stream input {})
        _     (when offset
                (IOUtils/skipFully ^InputStream input (long offset)))
        input (if size
                (bounded-input-stream input size)
                input)]
    (try
      (IOUtils/toByteArray ^InputStream input)
      (finally
        (when close
          (.close ^InputStream input))))))

(extend UnsynchronizedByteArrayOutputStream
  jio/IOFactory
  (assoc jio/default-streams-impl
         :make-input-stream (fn [x opts] (.toInputStream ^UnsynchronizedByteArrayOutputStream x))
         :make-output-stream (fn [x opts] x)))

(extend ByteArrayOutputStream
  jio/IOFactory
  (assoc jio/default-streams-impl
         :make-input-stream (fn [x opts]
                              (bytes-input-stream (.toByteArray ^ByteArrayOutputStream x)))
         :make-output-stream (fn [x opts] x)))

(extend UnsynchronizedBufferedInputStream
  jio/IOFactory
  (assoc jio/default-streams-impl
    :make-input-stream (fn [x opts] x)
    :make-reader (fn [^InputStream is opts]
                   (let [encoding (or (:encoding opts) "UTF-8")]
                     (-> (InputStreamReader. is ^String encoding)
                         (jio/make-reader opts))))))

;; Replace the default impl for InputStream to return an
;; unsynchronozed variant of BufferedInputStream.
(extend InputStream
  jio/IOFactory
  (assoc jio/default-streams-impl
    :make-input-stream (fn [x opts] (buffered-input-stream x opts))
    :make-reader (fn [^InputStream is opts]
                   (let [encoding (or (:encoding opts) "UTF-8")]
                     (-> (InputStreamReader. is ^String encoding)
                         (jio/make-reader opts))))))
