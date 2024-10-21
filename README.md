# datoteka #

A filesystem & path toolset for Clojure

The purpose of this library:

- work with paths (creating and manipulating).
- work with the filesystem (files and directories crud and many predicates).
- IO (clojure.java.io implementation for paths and other utilities,
  and reexports all existing API for convencience).

See the [API documentation](https://funcool.github.io/datoteka/latest/) for
more detailed information.


## Install

```clojure
funcool/datoteka
{:git/tag "4.0.0"
 :git/sha "3372f3a"
 :git/url "https://github.com/funcool/datoteka.git"}
```

## Getting Started

The path and filesystem helper functions are all exposed under the
`datoteka.fs` namespace, so let's import it:

```clojure
(require '[datoteka.fs :as fs])
```

This library uses JVM NIO, so under the hood, the `java.nio.file.Path`
is used instead of classical `java.io.File`.  You have many ways to
create a *path* instance. The basic one is just using the `path`
function:

```clojure
(fs/path "/tmp")
;; => #java.nio/path "/tmp"
```

As you can observe, the path properly prints with a *data reader*, so
once you have imported the library, you can use the `#java.nio/path "/tmp"`
syntax to create paths.

The paths also can be created from a various kind of objects (such as
URI, URL, String and seq's):

```clojure
(fs/path (java.net.URI. "file:///tmp"))
;; => #java.nio/path "/tmp"

(fs/path (java.net.URL. "file:///tmp"))
;; => #java.nio/path "/tmp"

(fs/path ["/tmp" "foo"])
;; => #java.nio/path "/tmp/foo"
```

The `path` function is also variadic, so you can pass multiple
arguments to it:

```clojure
(fs/path "/tmp" "foo")
;; => #java.nio/path "/tmp/foo"

(fs/path (java.net.URI. "file:///tmp") "foo")
;; => #java.nio/path "/tmp/foo"
```

And for convenience, you can use the `clojure.java.io` api with paths
in the same way as you have done it with `java.io.File`:

```clojure
(require '[clojure.java.io :as io])

(io/reader (fs/path "/etc/inputrc"))
;; => #object[java.io.BufferedReader 0x203dc326 "java.io.BufferedReader@203dc326"]

(subs (slurp (fs/path "/etc/inputrc")) 0 20)
;; => "# do not bell on tab"
```

