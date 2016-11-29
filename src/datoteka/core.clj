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

(ns datoteka.core
  "File System helpers."
  (:refer-clojure :exclude [name resolve])
  (:require [datoteka.proto :as pt])
  (:import java.nio.file.Path
           java.nio.file.Files
           java.nio.file.LinkOption
           java.nio.file.OpenOption
           java.nio.file.CopyOption
           java.nio.file.StandardOpenOption
           java.nio.file.StandardCopyOption
           java.nio.file.SimpleFileVisitor
           java.nio.file.FileVisitResult
           java.nio.file.attribute.FileAttribute
           java.nio.file.attribute.PosixFilePermissions
           ratpack.form.UploadedFile))

(def ^:dynamic *cwd* (.getCanonicalPath (java.io.File. ".")))
(def ^:dynamic *sep* (System/getProperty "file.separator"))
(def ^:dynamic *home* (System/getProperty "user.home"))
(def ^:dynamic *tmp-dir (System/getProperty "java.io.tmpdir"))
(def ^:dynamic *no-follow* (LinkOption/values))
(def ^:dynamic *os-name* (System/getProperty "os.name"))
(def ^:dynamic *system* (if (.startsWith *os-name* "Windows") :dos :unix))

(defn path
  "Create path from string or more than one string."
  ([fst]
   (pt/-path fst))
  ([fst & more]
   (pt/-path (cons fst more))))

;; (def write-open-opts
;;   (->> [StandardOpenOption/TRUNCATE_EXISTING
;;         StandardOpenOption/CREATE
;;         StandardOpenOption/WRITE]
;;        (into-array OpenOption)))

;; (def read-open-opts
;;   (->> [StandardOpenOption/READ]
;;        (into-array OpenOption)))

;; (def moveo-opts
;;   (->> [StandardCopyOption/ATOMIC_MOVE
;;         StandardCopyOption/REPLACE_EXISTING]
;;        (into-array CopyOption)))

;; (def follow-link-opts
;;   (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn make-permissions
  "Generate a array of `FileAttribute` instances
  generated from `rwxr-xr-x` kind of expressions."
  [^String expr]
  (let [perms (PosixFilePermissions/fromString expr)
        attr (PosixFilePermissions/asFileAttribute perms)]
    (into-array FileAttribute [attr])))

(defn path?
  "Return `true` if provided value is an instance of Path."
  [v]
  (instance? Path v))

(defn absolute?
  "Checks if the provided path is absolute."
  [path]
  (let [^Path path (pt/-path path)]
    (.isAbsolute path)))

(defn executable?
  "Checks if the provided path is executable."
  [path]
  (Files/isExecutable ^Path (pt/-path path)))

(defn exists?
  "Check if the provided path exists."
  [path]
  (let [^Path path (pt/-path path)]
    (Files/exists path follow-link-opts)))

(defn directory?
  "Checks if the provided path is a directory."
  [path]
  (let [^Path path (pt/-path path)]
    (Files/isDirectory path follow-link-opts)))

(defn regular-file?
  "Checks if the provided path is a plain file."
  [path]
  (Files/isRegularFile (pt/-path path) *no-follow*))

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

(defn permissions
  "Returns the string representation of the
  permissions of the provided path."
  [path]
  (let [^Path path (pt/-path path)]
    (->> (Files/getPosixFilePermissions path *no-follow*)
         (PosixFilePermissions/toString))))

(defn parent-directory
  "Get parent path if it exists."
  [path]
  (.getParent ^Path (pt/-path path)))

(defn name
  "Return a path representing the name of the file or
  directory, or null if this path has zero elements."
  [path]
  (str (.getFileName ^Path (pt/-path path))))

(defn split-extension
  "Returns a vector of `[name extension]`."
  [path]
  (let [base (name path)
        i (.lastIndexOf base ".")]
    (if (pos? i)
      [(subs base 0 i) (subs base i)]
      [base nil])))

(defn extension
  "Return the extension part of a file."
  [path]
  (last (split-ext path)))

(defn base-name
  "Return the name part of a file."
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
      (pt/-path (.replace path "~" ^String *home*))

      (not (.startsWith path *sep*))
      (pt/-path (str *cwd* *sep* path))

      :else (pt/-path path))))

(defn resolve
  "Resolve path on top of an other path."
  [base path]
  (let [^Path base (pt/-path base)
        ^Path path (pt/-path path)]
    (-> (.resolve base path)
        (.normalize))))

(defn relativize
  "Returns the relationship between two paths."
  [path1 path2]
  (.relativize ^Path (pt/-path path1)
               ^Path (pt/-path path2)))

(defmethod print-method Path
  [^Path v ^Writer w]
  (.write w (str "#path:\"" (.toString v) "\"")))

(defmethod print-method File
  [^File v ^Writer w]
  (.write w (str "#file:\"" (.toString v) "\"")))

(defn to-file
  "Converts the path to a java.io.File instance."
  [path]
  (.toFile ^Path (pt/-path path)))

(defn list-directory
  ([path]
   (let [path (pt/-path path)]
     (with-open [stream (Files/newDirectoryStream path)]
       (vec stream))))
  ([path glob]
   (let [path (pt/-path path)]
     (with-open [stream (Files/newDirectoryStream path ^String glob)]
       (vec stream)))))

(defn list-files
  [path]
  (filter regular-file? (list-directory path)))


;; --- Side-Effectfull Operations

(defn create-tempdir
  "Creates a temp directory on the filesystem."
  ([]
   (create-tmpdir ""))
  ([prefix]
   (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn create-dir
  "Create a new directory."
  ([path] (create-dir! path "rwxr-xr-x"))
  ([path perms]
   {:pre [(string? perms)]}
   (let [^Path path (pt/-path path)
         perms (make-permissions perms)]
     (Files/createDirectories path attrs))))

(defn delete-single
  "Delete signle file if it exists."
  [path]
  (Files/deleteIfExists ^Path (pt/-path path)))

(defn delete
  "Delete recursiverly a directory or file."
  [path]
  (let [path (pt/-path path)
        visitor (proxy [SimpleFileVisitor] []
                  (visitFile [file attrs]
                    (Files/delete file)
                    FileVisitResult/CONTINUE)
                  (postVisitDirectory [dir exc]
                    (Files/delete dir)
                    FileVisitResult/CONTINUE))]
    (Files/walkFileTree path visitor)))

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
  [src dst]
  (let [^Path src (-path src)
        ^Path dst (-path dst)]
    (Files/move src dst move-opts)))

(defn create-tempfile
  "Create a temporal file."
  [& {:keys [suffix prefix]}]
  (->> (make-file-attrs "rwxr-xr-x")
       (Files/createTempFile prefix suffix)))

(defn slurp-bytes
  [input]
  (with-open [input (io/input-stream input)
              output (java.io.ByteArrayOutputStream. (.available input))]
    (io/copy input output)
    (.toByteArray output)))
