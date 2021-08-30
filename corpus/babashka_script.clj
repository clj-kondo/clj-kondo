#!/usr/bin/env bb

(require '[babashka.process :refer [$ check]])

(def out-page (str "gh-pages/"
                   (or (System/getenv "BABASHKA_BOOK_MAIN")
                       "master")
                   ".html"))

(-> ($ asciidoctor src/book.adoc -o ~out-page -a docinfo=shared)
    check)

(binding [*out* *err*]
  (println "Done writing to" out-page))

(-> ($ ~x) ;; unresolved
    check)

(defn example
  []
  (let [env {"SOME_VAR" "wee"}] ;; not unused
    ;; unused binding env
    (-> ($ {:out :string :env env} echo "hi")
        (check)
        (:out)
        (println))))
