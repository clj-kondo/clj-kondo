#!/usr/bin/env bb

(require '[babashka.process :as p])

(def out-page (str "gh-pages/"
                   (or (System/getenv "BABASHKA_BOOK_MAIN")
                       "master")
                   ".html"))

@(p/$ asciidoctor src/book.adoc -o ~out-page -a docinfo=shared)

(binding [*out* *err*]
  (println "Done writing to" out-page))

(p/$ ~x) ;; unresolved
