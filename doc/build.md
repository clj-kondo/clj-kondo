# Building from source

To build a binary from source:

* Download [GraalVM](https://github.com/oracle/graal/releases)

* Set the `GRAALVM_HOME` variable. E.g.:

      export GRAALVM_HOME=$HOME/Downloads/graalvm-ce-1.0.0-rc15/Contents/Home

* Clone this repo and `cd clj-kondo`

* Optional. To include a built-in cache for Clojure and ClojureScript:

      script/built-in

* Optional. JDK 12 is needed only in this step. To include static method
  information from commonly used Java classes:

      JAVA_HOME=<path to JDK 12> script/extract-java

  where JDK 12 is located in e.g. `~/Downloads/jdk-12.jdk/Contents/Home`.

* Finally:

      script/compile

Place the binary somewhere on your path.
