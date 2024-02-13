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

(ns datoteka.fs
  "File System helpers."
  (:refer-clojure :exclude [name with-open])
  (:require
   [datoteka.proto :as pt]
   [clojure.java.io :as jio]
   [clojure.spec.alpha :as s]
   [clojure.core :as c])
  (:import
   java.io.ByteArrayInputStream
   java.io.ByteArrayOutputStream
   java.io.File
   java.io.InputStream
   java.io.Writer
   java.net.URI
   java.net.URL
   java.nio.file.CopyOption
   java.nio.file.FileVisitResult
   java.nio.file.Files
   java.nio.file.LinkOption
   java.nio.file.OpenOption
   java.nio.file.Path
   java.nio.file.Paths
   java.nio.file.SimpleFileVisitor
   java.nio.file.StandardCopyOption
   java.nio.file.StandardOpenOption
   java.nio.file.attribute.FileAttribute
   java.nio.file.attribute.PosixFilePermissions
   java.util.UUID))

(set! *warn-on-reflection* true)

(def ^:private empty-string-array
  (make-array String 0))

(extend-type String
  pt/IPath
  (-path [v] (Paths/get v empty-string-array)))

(def ^:dynamic *cwd* (pt/-path (.getCanonicalPath (java.io.File. "."))))
(def ^:dynamic *sep* (System/getProperty "file.separator"))
(def ^:dynamic *home* (pt/-path (System/getProperty "user.home")))
(def ^:dynamic *tmp-dir* (pt/-path (System/getProperty "java.io.tmpdir")))
(def ^:dynamic *os-name* (System/getProperty "os.name"))
(def ^:dynamic *system*
  (if (.startsWith ^String *os-name* "Windows") :dos :unix))

(defn path
  "Create path from string or more than one string."
  ([fst]
   (pt/-path fst))
  ([fst & more]
   (pt/-path (cons fst more))))

(def ^:private open-opts-map
  {:truncate StandardOpenOption/TRUNCATE_EXISTING
   :create StandardOpenOption/CREATE
   :append StandardOpenOption/APPEND
   :create-new StandardOpenOption/CREATE_NEW
   :delete-on-close StandardOpenOption/DELETE_ON_CLOSE
   :dsync StandardOpenOption/DSYNC
   :read StandardOpenOption/READ
   :write StandardOpenOption/WRITE
   :sparse StandardOpenOption/SPARSE
   :sync StandardOpenOption/SYNC})

(def ^:private copy-opts-map
  {:atomic StandardCopyOption/ATOMIC_MOVE
   :replace StandardCopyOption/REPLACE_EXISTING
   :copy-attributes StandardCopyOption/COPY_ATTRIBUTES})

(defn- link-opts ^"[Ljava.nio.file.LinkOption;"
  [{:keys [nofollow-links]}]
  (if nofollow-links
    (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])
    (into-array LinkOption [])))

(defn- interpret-open-opts
  [opts]
  {:pre [(every? open-opts-map opts)]}
  (->> (map open-opts-map opts)
       (into-array OpenOption)))

(defn- interpret-copy-opts
  [opts]
  {:pre [(every? copy-opts-map opts)]}
  (->> (map copy-opts-map opts)
       (into-array CopyOption)))

(defn make-permissions
  "Generate a array of `FileAttribute` instances
  generated from `rwxr-xr-x` kind of expressions."
  [^String expr]
  (let [perms (PosixFilePermissions/fromString expr)
        attr  (PosixFilePermissions/asFileAttribute perms)]
    (into-array FileAttribute [attr])))

(defn path?
  "Return `true` if provided value is an instance of Path."
  [v]
  (instance? Path v))

(defn file?
  "Check if `v` is an instance of java.io.File"
  [v]
  (instance? File v))

(defn absolute?
  "Checks if the provided path is absolute."
  [path]
  (let [^Path path (pt/-path path)]
    (.isAbsolute path)))

(defn relative?
  "Check if the provided path is relative."
  [path]
  (not (absolute? path)))

(defn executable?
  "Checks if the provided path is executable."
  [path]
  (Files/isExecutable ^Path (pt/-path path)))

(defn exists?
  "Check if the provided path exists."
  ([path] (exists? path nil))
  ([path params]
   (let [^Path path (pt/-path path)]
     (Files/exists path (link-opts params)))))

(defn directory?
  "Checks if the provided path is a directory."
  ([path] (directory? path nil))
  ([path params]
   (let [^Path path (pt/-path path)]
     (Files/isDirectory path (link-opts params)))))

(defn regular-file?
  "Checks if the provided path is a plain file."
  ([path] (regular-file? path nil))
  ([path params]
   (Files/isRegularFile (pt/-path path) (link-opts params))))

(defn link?
  "Checks if the provided path is a link."
  [path]
  (Files/isSymbolicLink (pt/-path path)))

(defn hidden?
  "Check if the provided path is hidden."
  [path]
  (Files/isHidden (pt/-path path)))

(defn readable?
  "Check if the provided path is readable."
  [path]
  (Files/isReadable (pt/-path path)))

(defn writable?
  "Check if the provided path is writable."
  [path]
  (Files/isWritable (pt/-path path)))

(defn size
  "Return the file size."
  [path]
  (-> path pt/-path Files/size))

(defn permissions
  "Returns the string representation of the
  permissions of the provided path."
  ([path] (permissions path nil))
  ([path params]
   (let [^Path path (pt/-path path)]
     (->> (Files/getPosixFilePermissions path (link-opts params))
          (PosixFilePermissions/toString)))))

(defn last-modified-time
  ([path] (last-modified-time path nil))
  ([path params]
   (let [^Path path (pt/-path path)]
     (Files/getLastModifiedTime path (link-opts params)))))

(defn real
  "Converts f into real path via Path#toRealPath."
  ([path] (real path nil))
  ([path params]
   (.toRealPath ^Path (pt/-path path)
                (link-opts params))))

(defn absolute
  "Return absolute path."
  [path]
  (.toAbsolutePath ^Path (pt/-path path)))

(defn parent
  "Get parent path if it exists."
  [path]
  (.getParent ^Path (pt/-path path)))

(defn name
  "Return a path representing the name of the file or
  directory, or null if this path has zero elements."
  [path]
  (str (.getFileName ^Path (pt/-path path))))

(defn split-ext
  "Returns a vector of `[^String name ^String extension]`."
  [path]
  (let [^Path path (pt/-path path)
        ^String path-str (.toString path)
        i (.lastIndexOf path-str ".")]
    (if (pos? i)
      [(subs path-str 0 i)
       (subs path-str i)]
      [path-str nil])))

(defn ext
  "Return the extension part of a file."
  [path]
  (some-> (last (split-ext path))
          (subs 1)))

(defn base
  "Return the base part of a file."
  [path]
  (first (split-ext path)))

(defn normalize
  "Normalize the path."
  [path]
  (let [^String path (str (.normalize ^Path (pt/-path path)))]
    (cond
      (= path "~")
      (pt/-path *home*)

      (.startsWith path (str "~" *sep*))
      (pt/-path (.replace path "~" ^String (.toString ^Object *home*)))

      (not (.startsWith path *sep*))
      (pt/-path (str *cwd* *sep* path))

      :else (pt/-path path))))

(defn join
  "Resolve path on top of an other path."
  ([base path]
   (let [^Path base (pt/-path base)
         ^Path path (pt/-path path)]
     (-> (.resolve base path)
         (.normalize))))
  ([base path & more]
   (reduce join base (cons path more))))

(defn relativize
  "Returns the relationship between two paths."
  [base other]
  (.relativize ^Path (pt/-path other)
               ^Path (pt/-path base)))

(defn file
  "Converts the path to a java.io.File instance."
  [path]
  (.toFile ^Path (pt/-path path)))

(defn- list-dir-lazy-seq
  ([stream] (list-dir-lazy-seq stream (seq stream)))
  ([stream s]
   (lazy-seq
    (let [p1 (first s)
          p2 (rest s)]
      (if (seq p2)
        (cons p1 (list-dir-lazy-seq stream p2))
        (do
          (.close ^java.lang.AutoCloseable stream)
          (cons p1 nil)))))))

(defn list-dir
  "Return a lazy seq of files and directories found under the provided
  directory. The order of files is not guarrantied.

  NOTE: the seq should be fully realized in order to properly release
  all acquired resources for this operation. Converting it to vector
  is an option for do it."
  ([path]
   (let [path (pt/-path path)
         stream (Files/newDirectoryStream path)]
     (list-dir-lazy-seq stream)))
  ([path ^String glob]
   (let [path   (pt/-path path)
         stream (Files/newDirectoryStream ^Path path glob)]
     (list-dir-lazy-seq stream))))

(defn create-tempdir
  "Creates a temp directory on the filesystem."
  ([]
   (create-tempdir ""))
  ([prefix]
   (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn create-dir
  "Create a new directory."
  [path & {:keys [perms] :or {perms "rwxr-xr-x"}}]
  (let [path (pt/-path path)
        attrs (make-permissions perms)]
    (Files/createDirectories ^Path path attrs)))

(defn- delete-recursive
  [^Path path]
  (->> (proxy [SimpleFileVisitor] []
         (visitFile [file attrs]
           (Files/delete file)
           FileVisitResult/CONTINUE)
         (postVisitDirectory [dir exc]
           (Files/delete dir)
           FileVisitResult/CONTINUE))
       (Files/walkFileTree path)))

(defn delete
  "Delete recursiverly a directory or file."
  [path]
  (let [^Path path (pt/-path path)]
    (if (regular-file? path)
      (Files/deleteIfExists path)
      (delete-recursive path))))

(defn move
  "Move or rename a file to a target file.

  By default, this method attempts to move the file to the target
  file, failing if the target file exists except if the source and
  target are the same file, in which case this method has no
  effect. If the file is a symbolic link then the symbolic link
  itself, not the target of the link, is moved.

  This method may be invoked to move an empty directory. When invoked
  to move a directory that is not empty then the directory is moved if
  it does not require moving the entries in the directory. For
  example, renaming a directory on the same FileStore will usually not
  require moving the entries in the directory. When moving a directory
  requires that its entries be moved then this method fails (by
  throwing an IOException)."
  ([src dst] (move src dst #{:atomic :replace}))
  ([src dst flags]
   (let [^Path src (pt/-path src)
         ^Path dst (pt/-path dst)
         opts (interpret-copy-opts flags)]
    (Files/move src dst opts))))

(defn create-tempfile
  "Create a temporal file."
  [& {:keys [suffix prefix dir perms]}]
  (let [dir   (or (some-> dir path) *tmp-dir*)
        attrs (if (string? perms)
                (make-permissions perms)
                (make-permissions "rwxr--r--"))]
    (Files/createTempFile dir prefix suffix attrs)))

(defn tempfile
  "Retrieves a candidate tempfile (without creating it)."
  [& {:keys [suffix prefix max-retries dir]
      :or {suffix ".tmp"
           dir *tmp-dir*
           max-retries 1000
           prefix "datoteka."}}]
  (loop [i 0]
    (let [candidate (path dir (str prefix (UUID/randomUUID) suffix))]
      (cond
        (and (exists? candidate) (not (> i max-retries)))
        (recur (inc i))

        (> i max-retries)
        (throw (IllegalStateException. "reached max iterations"))

        :else
        candidate))))

;; --- Implementation

(defmethod print-method Path
  [^Path v ^Writer w]
  (.write w (str "#path \"" (.toString v) "\"")))

(defmethod print-dup Path
  [^Path v ^Writer w]
  (print-method v w))

(defmethod print-method File
  [^File v ^Writer w]
  (.write w (str "#file \"" (.toString v) "\"")))

(defmethod print-dup File
  [^File v ^Writer w]
  (print-method v w))

(extend-protocol pt/IUri
  URI
  (-uri [v] v)

  String
  (-uri [v] (URI. v)))

(extend-protocol pt/IPath
  Path
  (-path [v] v)

  java.io.File
  (-path [v] (.toPath v))

  URI
  (-path [v] (Paths/get v))

  URL
  (-path [v] (Paths/get (.toURI v)))

  clojure.lang.Sequential
  (-path [v]
    (reduce #(.resolve ^Path %1 ^Path %2)
            (pt/-path (first v))
            (map pt/-path (rest v)))))

(extend-protocol jio/Coercions
  Path
  (as-file [it] (.toFile it))
  (as-url [it] (jio/as-url (.toFile it))))

(defn path->input-stream
  [^Path path]
  (let [opts (interpret-open-opts #{:read})]
    (Files/newInputStream path opts)))

(defn path->output-stream
  [^Path path]
  (let [opts (interpret-open-opts #{:truncate :create :write})]
    (Files/newOutputStream path opts)))

(extend-type Path
  jio/IOFactory
  (make-reader [path opts]
    (let [^InputStream is (path->input-stream path)]
      (jio/make-reader is opts)))
  (make-writer [path opts]
    (let [^OutputStream os (path->output-stream path)]
      (jio/make-writer os opts)))
  (make-input-stream [path opts]
    (path->input-stream path))
  (make-output-stream [path opts]
    (path->output-stream path)))

;; SPEC

(letfn [(conformer-fn [s]
          (cond
            (path? s)   s
            (string? s) (pt/-path s)
            :else       ::s/invalid))
        (unformer-fn [s]
          (str s))]
  (s/def ::path (s/conformer conformer-fn unformer-fn)))




