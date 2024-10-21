# Changelog #

## Version 4.0 ##

- Stabilize datoteka.io API:
  - Remove `!` from functions for make it similar to clojure.java.io api
  - Reorder some parameters for make the api consistent
  - Replace syncrhonized Input and Output streams with unsynchronizedd
    version from apache commons-io

## Version 3.1 ##

- Add the ability to pass directory to `create-tempfile` and `tempfile`
- Add print-dup impl for File and Path
- Fix reflection warnings
- Change create-dir call signature, it now accepts named options (only
  affects if you pass perms)
- Remove `tempfile`, you should be using `create-tempfile`
- Add `delete-on-exit!` helper
- Add `io/coercible?` helper for check if something is implementing
  jio/IOFactory protocol
- Add `io/reader` and `io/writer` convencience API.
- Expose `io/bytes-output-stream`, `io/buffered-input-stream` and
  `io/buffered-output-stream` (that internally uses commons-io impl of
  them that are unsynchronized, more friendly for virtual threads)
- Replace `io/write-to-file!` with more generic `io/write!`.
- Make `io/read-as-bytes` more generic and allow to pass size and offset.
- Add the ability to coerce ByteArrayOutputStream to InputStream for
  to be able to read it again.

## Version 3.0.64 ##

Date: 2022-06-22

- Add `size` helper.

## Version 3.0.63 ##

Date: 2022-06-21

- Remove `slurp-bytes`; it is inconsistent and does not make sense to have it.
  Can be replaced with https://github.com/clj-commons/byte-streams:
  Example: `(-> some-path io/input-stream bs/to-byte-array)`
- Return unbuffered input streams on clojure.java.io protocols.
- New helper: `tempfile` returns a tempfile candidate (without creating it).


## Version 2.0.0 ##

Date: 2021-04-27

- Code cleaning.
- Remove storage abstractions.


## Version 1.2.0 ##

Date: 2020-01-10

- Add proper coersions from Path to File.
- Add proper coersions from File to Path.
- Minor tooling update.
- Update deps.


## Version 1.1.0 ##

Date: 2019-07-09

- Dependencies updates.
- Convert to Clojure CLI Tools


## Version 1.0.0 ##

Date: 2017-02-07

- First release
