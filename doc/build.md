# Building from source

To build a binary from source, download [GraalVM](https://github.com/oracle/graal/releases) and set the
`GRAALVM_HOME` variable. E.g.:

    export GRAALVM_HOME=$HOME/Downloads/graalvm-ce-1.0.0-rc15/Contents/Home

Then clone this repo, `cd clj-kondo` and build the native binary:

    clojure -A:native-image

Place the binary somewhere on your path.
