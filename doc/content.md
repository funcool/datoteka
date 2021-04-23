# User Guide #


## Introduction ##

A filesystem toolset for Clojure:

- work with paths (creating and manipulating).
- work with the filesystem (files and directories crud and many predicates).
- IO (clojure.java.io implementation for paths).


## Install ##

Add the following dependency to your project.clj file:

```clojure
funcool/datoteka {:mvn/version "RELEASE"}
```


## Getting Started ##

The path and filesystem helper functions are all exposed under the `datoteka.core` namespace, so let's import it:

```clojure
(require '[datoteka.core :as fs])
```

This library uses JVM NIO, so under the hood, the `java.nio.file.Path` is used instead of classical `java.io.File`.
You have many ways to create a *path* instance. The basic one is just using the `path` function:

```clojure
(fs/path "/tmp")
;; => #path "/tmp"
```

As you can observe, the path properly prints with a *data reader*, so once you have imported the library, you can
use the `#path "/tmp"` syntax to create paths.

The paths also can be created from a various kind of objects (such as URI, URL, String and seq's):

```clojure
(fs/path (java.net.URI. "file:///tmp"))
;; => #path "/tmp"

(fs/path (java.net.URL. "file:///tmp"))
;; => #path "/tmp"

(fs/path ["/tmp" "foo"])
;; => #path "/tmp/foo"
```

The `path` function is also variadic, so you can pass multiple arguments to it:

```clojure
(fs/path "/tmp" "foo")
;; => #path "/tmp/foo"

(fs/path (java.net.URI. "file:///tmp") "foo")
;; => #path "/tmp/foo"
```

And for convenience, you can use the `clojure.java.io` api with paths in the same way as you
have done it with `java.io.File`:

```clojure
(require '[clojure.java.io :as io])

(io/reader (fs/path "/etc/inputrc"))
;; => #object[java.io.BufferedReader 0x203dc326 "java.io.BufferedReader@203dc326"]

(subs (io/slurp (fs/path "/etc/inputrc")) 0 20)
;; => "# do not bell on tab"
```


## License

_datoteka_ is licensed under BSD (2-Clause) license:

----
Copyright (c) Andrey Antukh <niwi@niwi.nz>

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
----
