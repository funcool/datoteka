(defproject funcool/datoteka "1.0.0-SNAPSHOT"
  :description "Filesystem utilities and Storage abstraction for Clojure."
  :url "https://github.com/funcool/datoteka"
  :license {:name "BSD (2-Clause)"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [funcool/promesa "1.7.0"]]

  :deploy-repositories {"releases" :clojars
                        "snapshots" :clojars}
  :source-paths ["src"]
  :test-paths ["test"]
  :jar-exclusions [#"\.swp|\.swo|user.clj"]

  :codeina {:sources ["src"]
            :reader :clojurescript
            :target "doc/dist/latest/api"
            :src-uri "http://github.com/funcool/datoteka/blob/master/"
            :src-uri-prefix "#L"}

  :profiles
  {:dev {:dependencies [[commons-io/commons-io "2.5"]
                        [org.clojure/tools.namespace "0.2.11"]]
         :plugins [[funcool/codeina "0.5.0"]
                   [lein-ancient "0.6.10"]]}})
