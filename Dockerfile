FROM clojure:lein-2.9.1 AS BASE

RUN apt-get update
RUN apt-get install --no-install-recommends -yy curl unzip build-essential zlib1g-dev sudo
WORKDIR "/opt"
RUN curl -sLO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.0.0/graalvm-ce-java11-linux-amd64-21.0.0.tar.gz
RUN tar -xzf graalvm-ce-java11-linux-amd64-21.0.0.tar.gz
ENV GRAALVM_HOME="/opt/graalvm-ce-java11-21.0.0"
ENV JAVA_HOME="/opt/graalvm-ce-java11-21.0.0/bin"
ENV PATH="$JAVA_HOME:$PATH"
COPY . .

ARG CLJ_KONDO_STATIC=
ARG CLJ_KONDO_MUSL=
ENV CLJ_KONDO_STATIC=$CLJ_KONDO_STATIC
ENV CLJ_KONDO_MUSL=$CLJ_KONDO_MUSL

RUN ./script/setup-musl
RUN ./script/compile

FROM ubuntu:latest
COPY --from=BASE /opt/clj-kondo /bin/clj-kondo
RUN mkdir -p /usr/local/bin && ln -s /bin/clj-kondo /usr/local/bin/clj-kondo
CMD ["clj-kondo"]
