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
  :main clj-kondo.main
  :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass
                                    :sign-releases false}]])
