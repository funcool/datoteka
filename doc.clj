(require '[codox.main :as codox])

(codox/generate-docs
 {:output-path "doc/dist/latest"
  :metadata {:doc/format :markdown}
  :language :clojure
  :name "funcool/datoteka"
  :themes [:rdash]
  :source-paths ["src"]
  :namespaces [#"^datoteka\."]
  :source-uri "https://github.com/funcool/datoteka/blob/master/{filepath}#L{line}"})
