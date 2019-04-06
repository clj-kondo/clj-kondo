FROM ubuntu AS BASE
RUN apt-get update
RUN apt-get install -yy curl wget leiningen build-essential zlib1g-dev
RUN cd /opt && wget https://github.com/oracle/graal/releases/download/vm-1.0.0-rc15/graalvm-ce-1.0.0-rc15-linux-amd64.tar.gz
RUN cd /opt && tar -xzvf graalvm-ce-1.0.0-rc15-linux-amd64.tar.gz
RUN wget https://download.clojure.org/install/linux-install-1.10.0.442.sh
RUN chmod +x linux-install-1.10.0.442.sh
RUN ./linux-install-1.10.0.442.sh
ADD deps.edn .
ADD src src
RUN clojure -Spath
RUN GRAALVM_HOME=/opt/graalvm-ce-1.0.0-rc15 clojure -A:native-image --verbose
CMD ["/clj-kondo"]
