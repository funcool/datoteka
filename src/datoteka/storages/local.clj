;; Copyright (c) 2015-2017 Andrey Antukh <niwi@niwi.nz>
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

(ns datoteka.storages.local
  "A local filesystem storage implementation."
  (:require [promesa.core :as p]
            [clojure.java.io :as io]
            [datoteka.proto :as pt]
            [datoteka.core :as fs])
  (:import java.io.InputStream
           java.io.OutputStream
           java.net.URI
           java.nio.file.Path
           java.nio.file.Files
           java.nio.file.NoSuchFileException
           java.util.function.Supplier
           java.util.concurrent.CompletableFuture
           java.util.concurrent.ExecutorService
           java.util.concurrent.ForkJoinPool))

(defn normalize-path
  [^Path base ^Path path]
  (if (fs/absolute? path)
    (throw (ex-info "Suspicios operation: absolute path not allowed."
                    {:path (str path)}))
    (let [^Path fullpath (.resolve base path)
          ^Path fullpath (.normalize fullpath)]
      (when-not (.startsWith fullpath base)
        (throw (ex-info "Suspicios operation: go to parent dir is not allowed."
                        {:path (str path)})))
      fullpath)))

(defn- normalize-filename
  [^Path path options]
  (let [transform (:transform-filename options)
        transform (if (fn? transform) transform identity)
        parent (fs/parent path)
        [name ext] (fs/split-ext (fs/name path))]
    (fs/path parent (str (transform name) ext))))

(defn- save
  [this base path content]
  (let [^Path path (pt/-path path)
        ^Path fullpath (-> (normalize-path base path)
                           (normalize-filename (:options this)))]
    (when-not (fs/exists? (.getParent fullpath))
      (fs/create-dir (.getParent fullpath)))
    (loop [iteration nil]
      (let [[basepath ext] (fs/split-ext fullpath)
            candidate (fs/path (str basepath iteration ext))]
        (if (fs/exists? candidate)
          (recur (if (nil? iteration) 1 (inc iteration)))
          (with-open [^InputStream src (pt/-input-stream content)
                      ^OutputStream dst (io/output-stream candidate)]
            (io/copy src dst)
            (fs/relativize candidate base)))))))

(defn- delete
  [this base path]
  (let [path (->> (pt/-path path)
                  (normalize-path base))]
    (try
      (fs/delete path)
      true
      (catch java.nio.file.NoSuchFileException e
        false))))

(defn- submit
  [executor func]
  (let [supplier (reify Supplier (get [_] (func)))]
     (CompletableFuture/supplyAsync supplier executor)))


(defrecord LocalFileSystemBackend [^Path base
                                   ^URI baseuri
                                   ^ExecutorService executor
                                   options]
  pt/IPublicStorage
  (-public-uri [_ path]
    (.resolve baseuri (str path)))

  pt/IStorage
  (-save [this path content]
    (submit executor #(save this base path content)))

  (-delete [this path]
    (submit executor #(delete this base path)))

  (-exists? [this path]
    (try
      (p/resolved
       (let [path (->> (pt/-path path)
                       (normalize-path base))]
         (fs/exists? path)))
      (catch Exception e
        (p/rejected e))))

  (-lookup [_ path']
    (try
      (p/resolved
       (->> (pt/-path path')
            (normalize-path base)))
      (catch Exception e
        (p/rejected e))))

  pt/IClearableStorage
  (-clear [_]
    (fs/delete base)
    (fs/create-dir base)))


(defn localfs
  "Create an instance of local FileSystem storage providing an
  absolute base path.

  If that path does not exists it will be automatically created,
  if it exists but is not a directory, an exception will be
  raised.

  This function expects a map with the following options:
  - `:basedir`: a fisical directory on your local machine
  - `:baseuri`: a base uri used for resolve the files
  - `:executor`: an executor instance (optional)
  - `:transform-filename`: a function responsible to transform filename (optional)
  "
  [{:keys [basedir baseuri executor]
    :or {executor (ForkJoinPool/commonPool)}
    :as options}]
  (let [^Path basepath (pt/-path basedir)
        ^URI baseuri (pt/-uri baseuri)]
    (when (and (fs/exists? basepath)
               (not (fs/directory? basepath)))
      (throw (ex-info "File already exists." {})))

    (when-not (fs/exists? basepath)
      (fs/create-dir basepath))

    (->LocalFileSystemBackend basepath baseuri executor
                              (dissoc options :baseuri :basedir :executor))))
