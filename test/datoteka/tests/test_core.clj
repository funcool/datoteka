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

(ns datoteka.tests.test-core
  (:require [clojure.test :as t]
            [clojure.java.io :as jio]
            [datoteka.io :as io]
            [datoteka.fs :as fs]))

(t/deftest predicates-test
  (t/is (fs/path? (fs/path ".")))

  (t/is (fs/absolute? (fs/path "/tmp")))
  (t/is (not (fs/absolute? (fs/path "tmp"))))

  (t/is (fs/executable? "/bin/sh"))
  (t/is (not (fs/executable? "/proc/cpuinfo")))

  (t/is (fs/exists? "/tmp"))
  (t/is (not (fs/exists? "/foobar")))

  (t/is (fs/directory? "/tmp"))
  (t/is (not (fs/directory? "/foobar")))

  (t/is (fs/regular-file? "/proc/cpuinfo"))
  (t/is (not (fs/regular-file? "/tmp")))

  (t/is (fs/hidden? ".bashrc"))
  (t/is (not (fs/hidden? "bashrc")))

  (t/is (fs/readable? "/proc/cpuinfo"))
  (t/is (not (fs/readable? "/proc/cpuinfo2")))

  (t/is (fs/writable? "/tmp"))
  (t/is (not (fs/writable? "/proc/cpuinfo")))
  )

(t/deftest coercions
  (t/is (fs/file? (jio/as-file (fs/path "/tmp/foobar.txt"))))
  (t/is (fs/path? (fs/path (jio/as-file "/tmp/foobar.txt")))))

(t/deftest path-manipulation-test
  (t/is (= (fs/path "/foo") (fs/parent "/foo/bar")))
  (t/is (= "bar.txt" (fs/name "/foo/bar.txt")))
  (t/is (= ["/foo/bar" ".txt"] (fs/split-ext "/foo/bar.txt")))
  (t/is (= fs/*home* (fs/normalize "~")))
  (t/is (= fs/*cwd* (fs/normalize ".")))
  (t/is (= (fs/path "/foo/bar") (fs/path "/foo" "bar")))
  (t/is (fs/file? (fs/file "foobar")))
  )

(t/deftest io-copy-test-1
  (let [input  (byte-array [1 2 3 4 5 6 7 8 9 10])
        output (with-open [is (io/bytes-input-stream input)]
                 (with-open [os (io/bytes-output-stream 10)]
                   (io/copy is os :size -1)
                   (with-open [is' (io/input-stream os)]
                     (io/read is'))))]

    (t/is (= (seq input)
             (seq output)))))

(t/deftest io-copy-test-2
  (let [input  (byte-array [1 2 3 4 5 6 7 8 9 10])
        output (with-open [is (io/bytes-input-stream input)]
                 (with-open [os (io/bytes-output-stream 10)]
                   (io/copy is os :offset 2)
                   (with-open [is' (io/input-stream os)]
                     (io/read is'))))]

    (t/is (= (vec output)
             [3 4 5 6 7 8 9 10]))))

(t/deftest read-1
  (let [input  (byte-array [1 2 3 4 5 6 7 8 9 10])
        output (with-open [input (io/bytes-input-stream input)]
                 (io/read input))]

    (t/is (= (vec input)
             (vec output)))))

(t/deftest read-2
  (let [input  (byte-array [1 2 3 4 5 6 7 8 9 10])
        output (with-open [input (io/bytes-input-stream input)]
                 (io/read input :size 4))]

    (t/is (= (vec output)
             [1 2 3 4]))))

(t/deftest read-3
  (let [input  (byte-array [1 2 3 4 5 6 7 8 9 10])
        output (with-open [input (io/bytes-input-stream input)]
                 (io/skip input 4)
                 (io/read input :size 2))]
    (t/is (= (vec output)
             [5 6]))))




