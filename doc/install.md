# Installation

## Brew (MacOS and Linux)

On MacOS you can use [brew](https://brew.sh/). On Linux you can use
[Linuxbrew](http://linuxbrew.sh/).

To install with brew:

    brew install borkdude/brew/clj-kondo

To upgrade:

    brew upgrade clj-kondo

## Snap (Linux)

To install:

    sudo snap install clj-kondo

To give clj-kondo access to your home directory:

    sudo snap connect clj-kondo:home

With snap, directories starting with a dot in the root of the home directory, e.g. `~/.clj-kondo`, are treated specially. To give clj-kondo access to `~/.clj-kondo`:

    sudo snap connect clj-kondo:clj-kondo-dir

To upgrade:

    sudo snap refresh clj-kondo

## Arch Linux

`clj-kondo` is [available](https://aur.archlinux.org/packages/clj-kondo-bin/) in the [Arch User Repository](https://aur.archlinux.org). It can be installed using your favorite [AUR](https://aur.archlinux.org) helper such as
[yay](https://github.com/Jguer/yay), [yaourt](https://github.com/archlinuxfr/yaourt), [apacman](https://github.com/oshazard/apacman) and [pacaur](https://github.com/rmarquis/pacaur). Here is an example using `yay`:

    yay -S clj-kondo-bin

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

## Scoop (Windows)

Note: clj-kondo on Windows is considered experimental. Until we sort out [this issue](https://github.com/borkdude/clj-kondo/issues/276), the scoop package will not be updated. You can try the latest binary from [Github](https://github.com/borkdude/clj-kondo/releases).

To install clj-kondo on Windows you can use [scoop](https://scoop.sh):

     scoop bucket add borkdude https://github.com/borkdude/scoop-bucket
     scoop install clj-kondo

To upgrade:

    scoop update clj-kondo

## Manual install

Pre-built binaries are available for linux, MacOS and Windows on the
[releases](https://github.com/borkdude/clj-kondo/releases) page.

## [Running on the JVM](jvm.md)

## [Running with Docker](docker.md)
