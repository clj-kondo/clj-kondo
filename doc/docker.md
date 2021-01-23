# Docker

To run with Docker:

    docker run -v $PWD/src:/src --rm cljkondo/clj-kondo clj-kondo --lint src

To lint an entire project including dependencies, you can mount your Maven
dependencies into the container:

    docker run -v $PWD/src:/src -v $HOME/.m2:$HOME/.m2 --rm cljkondo/clj-kondo \
      clj-kondo --lint $(clj -Spath)

To lint stdin:

    echo '(select-keys)' | docker run -i --rm clj-kondo/clj-kondo clj-kondo --lint -

You can use the `latest` tag to get the latest non-SNAPSHOT release. See [tags](https://hub.docker.com/r/clj-kondo/clj-kondo/tags).
