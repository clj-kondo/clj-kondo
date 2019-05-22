FROM ubuntu AS BASE
RUN apt-get update
RUN apt-get install -yy curl unzip wget build-essential zlib1g-dev
COPY script/install-clj-kondo .
RUN ./install-clj-kondo
CMD ["/usr/local/bin/clj-kondo"]
