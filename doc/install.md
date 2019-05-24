# Installation

## Brew (MacOS and Linux)

On MacOS you can use [brew](https://brew.sh/). On Linux you can use
[Linuxbrew](http://linuxbrew.sh/).

To install with brew:

    brew install borkdude/brew/clj-kondo

To upgrade:

    brew upgrade clj-kondo

## Scoop (Windows)

To install clj-kondo on Windows you can use [scoop](https://scoop.sh):

     scoop bucket add borkdude https://github.com/borkdude/scoop-bucket
     scoop install clj-kondo

To upgrade:

    scoop update clj-kondo

## Installation script (MacOS and Linux)

Ths installation script works for linux and MacOS and can be used for quickly
installing or upgrading to the newest clj-kondo without a package manager,
e.g. in CI. It will install to `/usr/local/bin` by default, but you can override
this location with the `--dir` option.

To download and execute the script:

    curl -sO https://raw.githubusercontent.com/borkdude/clj-kondo/master/script/install-clj-kondo
    chmod +x install-clj-kondo
    ./install-clj-kondo

or

    ./install-clj-kondo --dir /tmp

To upgrade, just run the script again.

## Manual install

Pre-built binaries are available for linux, MacOS and Windows on the
[releases](https://github.com/borkdude/clj-kondo/releases) page.

## [Running on the JVM](doc/jvm.md)

## [Running on Docker](doc/docker.md)
