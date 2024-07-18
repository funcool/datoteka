(ns build
  (:refer-clojure :exclude [compile])
  (:require
   [clojure.tools.build.api :as b]))

(def lib 'nz.niwi/datoteka)
(def version (str "3.1.0-" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom
   {:class-dir class-dir
    :lib lib
    :version version
    :basis @basis
    :src-dirs ["src"]})

  (b/copy-dir
   {:src-dirs ["src"]
    :target-dir class-dir})

  (b/jar
   {:class-dir class-dir
    :jar-file jar-file}))

(defn clojars [_]
  (b/process
   {:command-args ["mvn"
                   "deploy:deploy-file"
                   (str "-Dfile=" jar-file)
                   "-DpomFile=target/classes/META-INF/maven/nz.niwi/datoteka/pom.xml"
                   "-DrepositoryId=clojars"
                   "-Durl=https://clojars.org/repo/"]}))
