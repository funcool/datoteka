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

(ns datoteka.tests.test_storages
  (:require [clojure.test :as t]
            [datoteka.storages :as st]
            [datoteka.storages.local :as local]
            [datoteka.storages.misc :as misc])
  (:import java.io.File
           org.apache.commons.io.FileUtils))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test Fixtures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- clean-temp-directory
  [next]
  (next)
  (let [directory (File. "/tmp/datoteka/")]
    (FileUtils/deleteDirectory directory)))

(t/use-fixtures :each clean-temp-directory)

;; --- Tests: FileSystemStorage

(t/deftest test-localfs-store-and-lookup
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})
        rpath  @(st/save storage "test.txt" "my content")
        fpath @(st/lookup storage rpath)
        fdata (slurp fpath)]
    (t/is (= (str fpath) "/tmp/datoteka/test/test.txt"))
    (t/is (= "my content" fdata))))

(t/deftest test-localfs-store-and-get-public-url
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})
        rpath  @(st/save storage "test.txt" "my content")
        ruri (st/public-url storage rpath)]
    (t/is (= (str ruri) "http://localhost:5050/test.txt"))))

(t/deftest test-localfs-store-and-lookup-with-subdirs
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})
        rpath  @(st/save storage "somepath/test.txt" "my content")
        fpath @(st/lookup storage rpath)
        fdata (slurp fpath)]
    (t/is (= (str fpath) "/tmp/datoteka/test/somepath/test.txt"))
    (t/is (= "my content" fdata))))

(t/deftest test-localfs-store-and-delete-and-check
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})
        rpath  @(st/save storage "test.txt" "my content")]
    (t/is @(st/delete storage rpath))
    (t/is (not @(st/exists? storage rpath)))))

(t/deftest test-localfs-store-duplicate-file-raises-exception
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})]
    (t/is @(st/save storage "test.txt" "my content"))
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(st/save storage "test.txt" "my content")))))

(t/deftest test-localfs-access-unauthorized-path
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})]
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(st/lookup storage "../test.txt")))
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(st/lookup storage "/test.txt")))))

;; --- Tests: ScopedPathStorage

(t/deftest test-localfs-scoped-store-and-lookup
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})
        storage (misc/scoped storage "some/prefix")
        rpath  @(st/save storage "test.txt" "my content")
        fpath @(st/lookup storage rpath)
        fdata (slurp fpath)]
    (t/is (= (str fpath) "/tmp/datoteka/test/some/prefix/test.txt"))
    (t/is (= "my content" fdata))))

(t/deftest test-localfs-scoped-store-and-delete-and-check
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})
        storage (misc/scoped storage "some/prefix")
        rpath  @(st/save storage "test.txt" "my content")]
    (t/is @(st/delete storage rpath))
    (t/is (not @(st/exists? storage rpath)))))

(t/deftest test-localfs-scoped-store-duplicate-file-raises-exception
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})
        storage (misc/scoped storage "some/prefix")]
    (t/is @(st/save storage "test.txt" "my content"))
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(st/save storage "test.txt" "my content")))))

(t/deftest test-localfs-scoped-access-unauthorized-path
  (let [storage (local/localfs {:basedir "/tmp/datoteka/test"
                                :baseuri "http://localhost:5050/"})
        storage (misc/scoped storage "some/prefix")]
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(st/lookup storage "../test.txt")))
    (t/is (thrown? java.util.concurrent.ExecutionException
                   @(st/lookup storage "/test.txt")))))

