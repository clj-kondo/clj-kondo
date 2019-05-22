FROM ubuntu AS BASE
RUN apt-get update
RUN apt-get install -yy curl unzip wget build-essential zlib1g-dev
WORKDIR "/opt"
RUN wget https://github.com/oracle/graal/releases/download/vm-19.0.0/graalvm-ce-linux-amd64-19.0.0.tar.gz
RUN tar -xzf graalvm-ce-linux-amd64-19.0.0.tar.gz
ENV GRAALVM_HOME="/opt/graalvm-ce-19.0.0"
ENV JAVA_HOME="/opt/graalvm-ce-19.0.0/bin"
ENV PATH="$PATH:$JAVA_HOME"
RUN wget https://download.clojure.org/install/linux-install-1.10.0.442.sh
RUN chmod +x linux-install-1.10.0.442.sh
RUN ./linux-install-1.10.0.442.sh
COPY deps.edn .
COPY src src
COPY resources resources
COPY reflection.json .
RUN clojure -Spath
COPY script/compile .
RUN ./compile
RUN cp clj-kondo /usr/local/bin
FROM alpine:3.9
# See https://github.com/sgerrand/alpine-pkg-glibc
RUN apk --no-cache add ca-certificates wget
RUN wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub
RUN wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.29-r0/glibc-2.29-r0.apk
RUN apk add glibc-2.29-r0.apk
COPY --from=BASE /usr/local/bin/clj-kondo /usr/local/bin
ENV LD_LIBRARY_PATH /lib
CMD ["clj-kondo"]
