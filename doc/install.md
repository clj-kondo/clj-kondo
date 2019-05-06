# Installation

## Brew

On MacOS you can use [brew](https://brew.sh/). On Linux you can use
[Linuxbrew](http://linuxbrew.sh/).

To install with brew:

    brew install borkdude/brew/clj-kondo

To upgrade:

    brew upgrade clj-kondo

## Installation script

The installation script will download the latest version and install it to `/usr/local/bin` by default. You can override this location with the `--dir` option.

To download and execute the script:

    curl -sO https://raw.githubusercontent.com/borkdude/clj-kondo/master/script/install-clj-kondo
    chmod +x install-clj-kondo
    ./install-clj-kondo

or

    ./install-clj-kondo --dir /tmp

To upgrade, just run the script again.

## Manual install

Pre-built binaries are available for linux and MacOS on the
[releases](https://github.com/borkdude/clj-kondo/releases) page.
