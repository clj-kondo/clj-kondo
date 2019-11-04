(defproject clj-kondo #=(clojure.string/trim
                         #=(slurp "resources/CLJ_KONDO_VERSION"))
  :description "Tidy your code with clj-kondo."
  :url "https://github.com/borkdude/clj-kondo"
  :scm {:name "git"
        :url "https://github.com/borkdude/clj-kondo"}
  :license {:name "Eclipse Public License 1.0"
            :url "http://opensource.org/licenses/eclipse-1.0.php"}
  :source-paths ["src" "parser" "inlined"]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [io.lambdaforge/datalog-parser "0.1.1"]
                 [cheshire "5.8.1"]]
  :profiles {:clojure-1.9.0 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :clojure-1.10.1 {:dependencies [[org.clojure/clojure "1.10.1"]]}
             :test {:dependencies [[clj-commons/conch "0.9.2"]
                                   [jonase/eastwood "0.3.6"]]
                    :source-paths ["src" "parser" "inlined" "extract"]}
             :uberjar {:global-vars {*assert* false}
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]
                       :main clj-kondo.main
                       :aot :all}}
  :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass
                                    :sign-releases false}]])
