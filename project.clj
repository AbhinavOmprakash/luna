(defproject org.clojars.abhinav/luna "0.1.0-SNAPSHOT"
  :description "A Domain Specific Language (DSL) that translates to regex."
  :url "https://github.com/AbhinavOmprakash/luna"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :plugins [[lein-auto "0.1.3"]]
  :repl-options {:init-ns luna.core})
