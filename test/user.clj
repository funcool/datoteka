(ns user
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :as repl]
   [clojure.walk :refer [macroexpand-all]]
   [datoteka.fs :as fs]))

;; --- Development Stuff

(defn run-test
  ([] (run-test #"^datoteka.tests.*"))
  ([o]
   (repl/refresh)
   (cond
     (instance? java.util.regex.Pattern o)
     (test/run-all-tests o)

     (symbol? o)
     (if-let [sns (namespace o)]
       (do (require (symbol sns))
           (test/test-vars [(resolve o)]))
       (test/test-ns o)))))

(defn -main
  [& args]
  (require 'datoteka.tests.test-core)
  (let [{:keys [fail]} (test/run-all-tests #"^datoteka.tests.*")]
    (if (pos? fail)
      (System/exit fail)
      (System/exit 0))))
