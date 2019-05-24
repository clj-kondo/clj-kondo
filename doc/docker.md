# Docker

To run with Docker:

    docker run -v $PWD/src:/src borkdude/clj-kondo clj-kondo --lint src

To lint an entire project including dependencies, you can mount your Maven
dependencies into the container:

    docker run -v $PWD/src:/src -v $HOME/.m2:$HOME/.m2 borkdude/clj-kondo \
      clj-kondo --lint $(clj -Spath)
