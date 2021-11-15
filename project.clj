(defproject org.clojars.abhinav/luna "0.1.1"
  :description "A Domain Specific Language (DSL) that translates to regex."
  :url "https://github.com/AbhinavOmprakash/luna"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :plugins [[lein-auto "0.1.3"]
            [lein-cloverage "1.0.9"]]
  :repl-options {:init-ns luna.core})
