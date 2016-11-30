(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [clojure.walk :refer [macroexpand-all]]
            [clojure.pprint :refer [pprint]]
            [clojure.test :as test]))

;; --- Development Stuff

(defn test-vars
  [& vars]
  (repl/refresh)
  (test/test-vars
   (map (fn [sym]
          (require (symbol (namespace sym)))
          (resolve sym))
        vars)))

(defn test-ns
  [ns]
  (repl/refresh)
  (test/test-ns ns))

(defn test-all
  ([] (test/run-all-tests #"^datoteka.tests.*"))
  ([re] (test/run-all-tests re)))
