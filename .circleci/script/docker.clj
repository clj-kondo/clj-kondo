(require '[babashka.fs :as fs]
         '[babashka.process :as proc]
         '[clojure.string :as str])
(import '[java.time Instant])

(defn read-env
  ([k]
   (read-env k nil))
  ([k default]
   (or (System/getenv k)
       default)))

(def image-name "cljkondo/clj-kondo")

(def ghcr-image-name "ghcr.io/clj-kondo/clj-kondo")

(def image-tag (str/trim (slurp "resources/CLJ_KONDO_VERSION")))

(def latest-tag "latest")

(def platforms (read-env "PLATFORMS" "linux/amd64"))

(def circle-repository-url (read-env "CIRCLE_REPOSITORY_URL"))

(def label-args
  ["--label" "'org.opencontainers.image.description=Clj-kondo, a Clojure linter that sparks joy'"
   "--label" "org.opencontainers.image.title=Clj-kondo"
   "--label" (str "org.opencontainers.image.created=" (Instant/now))
   "--label" (str "org.opencontainers.image.url=" circle-repository-url)
   "--label" (str "org.opencontainers.image.documentation=" circle-repository-url)
   "--label" (str "org.opencontainers.image.source=" circle-repository-url)
   "--label" (str "org.opencontainers.image.revision=" (read-env "CIRCLE_SHA1"))
   "--label"
   (format "org.opencontainers.image.ref.name=%s:%s"
           (read-env "CIRCLE_TAG")
           (read-env "CIRCLE_BRANCH"))
   "--label" (str "org.opencontainers.image.version=" image-tag)])

(def snapshot? (str/includes? image-tag "SNAPSHOT"))

(defn exec
  [cmd]
  (-> cmd
      (proc/process {:out :inherit :err :inherit})
      (proc/check)))

(defn docker-login
  [username password]
  (exec ["docker" "login" "-u" username "-p" password]))

(defn docker-login-ghcr
  [username password]
  (exec ["docker" "login" "ghcr.io" "-u" username "-p" password]))

;; TODO: Remove this when Dockerhub goes off
(defn build-push
  [image-tag platform docker-file]
  (println (format "Building and pushing %s Docker image(s) %s:%s"
                   platform
                   image-name
                   image-tag))
  (let [base-cmd ["docker" "buildx" "build"
                  "-t" (str image-name ":" image-tag)
                  "--platform" platform
                  "--push"
                  "-f" docker-file]]
    (exec (concat base-cmd label-args ["."]))))

(defn build-push-ghcr
  [image-tag platform docker-file]
  (println (format "Building and pushing %s Docker image(s) %s:%s to GHCR"
                   platform
                   ghcr-image-name
                   image-tag))
  (let [base-cmd ["docker" "buildx" "build"
                  "-t" (str ghcr-image-name ":" image-tag)
                  "--platform" platform
                  "--push"
                  "-f" docker-file]]
    (exec (concat base-cmd label-args ["."]))))

(defn build-push-images
  []
  (doseq [platform (str/split platforms #",")]
    (let [tarball-platform (str/replace platform #"\/" "-")
          tarball-platform (if (= "linux-arm64" tarball-platform)
                             "linux-aarch64"
                             tarball-platform)
          tarball-path     (format "/tmp/release/clj-kondo-%s-%s.zip"
                                   image-tag
                                   tarball-platform)]
      (fs/create-dirs platform)
      (exec ["unzip" tarball-path "-d" platform])
      ; this overwrites, but this is to work around having built the uberjar/metabom multiple times
      #_(fs/copy (format "/tmp/release/%s-metabom.jar" tarball-platform) "metabom.jar" {:replace-existing true})))
  (build-push image-tag platforms "Dockerfile.ci")
  (build-push-ghcr image-tag platforms "Dockerfile.ci")
  (when-not snapshot?
    (build-push latest-tag platforms "Dockerfile.ci")
    (build-push-ghcr latest-tag platforms "Dockerfile.ci")))

(defn build-push-alpine-images
  "Build alpine image for linux-amd64 only (no upstream arm64 support yet)"
  []
  (exec ["unzip" (str "/tmp/release/clj-kondo-" image-tag "-linux-static-amd64.zip")])
  (build-push (str image-tag "-alpine") "linux/amd64" "Dockerfile.alpine")
  (build-push-ghcr (str image-tag "-alpine") "linux/amd64" "Dockerfile.alpine")
  (when-not snapshot?
    (build-push "alpine" "linux/amd64" "Dockerfile.alpine")
    (build-push-ghcr "alpine" "linux/amd64" "Dockerfile.alpine")))

(when (= *file* (System/getProperty "babashka.file"))
  (if true #_(and (nil? (read-env "CIRCLE_PULL_REQUEST"))
           (= "master" (read-env "CIRCLE_BRANCH")))
    (do
      (if snapshot?
        (println "This is a snapshot version")
        (println "This is a non-snapshot version"))
      (docker-login (read-env "DOCKERHUB_USER") (read-env "DOCKERHUB_PASS"))
      (docker-login-ghcr (read-env "CONTAINER_REGISTRY_USER") (read-env "GHCR_TOKEN"))
      (build-push-images)
      (build-push-alpine-images))
    (println "Not publishing docker image(s).")))
