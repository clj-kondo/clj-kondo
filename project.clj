(defproject clj-kondo #=(clojure.string/trim
                         #=(slurp "resources/CLJ_KONDO_VERSION"))
  :description "Tidy your code with clj-kondo."
  :url "https://github.com/borkdude/clj-kondo"
  :scm {:name "git"
        :url "https://github.com/borkdude/clj-kondo"}
  :license {:name "Eclipse Public License 1.0"
            :url "http://opensource.org/licenses/eclipse-1.0.php"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [rewrite-clj "0.6.1"]
                 [com.cognitect/transit-clj "0.8.313"]]
  :plugins [[io.taylorwood/lein-native-image "0.3.0"]]
  :profiles {:uberjar {:global-vars {*assert* false}
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :main clj-kondo.main
                       :aot :all}}
  :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]}
  :native-image {:name     "clj-kondo"
                 :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                 :opts     ["-H:Name=clj-kondo"
                            "-H:+ReportExceptionStackTraces"
                            "-J-Dclojure.spec.skip-macros=true"
                            "-J-Dclojure.compiler.direct-linking=true"
                            "-H:IncludeResources=clj_kondo/impl/cache/built_in/.*/.*transit.json$"
                            "-H:ReflectionConfigurationFiles=reflection.json"
                            "--initialize-at-build-time"
                            "-H:Log=registerResource:"
                            "--verbose"
                            "--initialize-at-build-time"
                            "-J-Xmx3g"
                            "--no-server" ;;avoid spawning build server
                            ]}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass
                                    :sign-releases false}]])
