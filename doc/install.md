# Installation

## Manual install

Pre-built binaries are available for linux, macOS and Windows on the
[releases](https://github.com/borkdude/clj-kondo/releases) page.

Repositories
for various Linux distributions can be found
[here](https://software.opensuse.org//download.html?project=home%3Azilti%3Aclojure&package=clj-kondo).

There is also an
[updateable AppImage](https://download.opensuse.org/repositories/home:/zilti:/clojure/AppImage/clj-kondo-latest-x86_64.AppImage).
If you use the AppImage, simply save the file as "clj-kondo" and make it executable.
It is fully self-contained - without the overhead that comes with Docker!

## Installation script (macOS and Linux)

This installation script works for linux and MacOS and can be used for quickly
installing or upgrading to the newest clj-kondo without a package manager. It
will install to `/usr/local/bin` by default.

To download and execute the script:

    curl -sLO https://raw.githubusercontent.com/borkdude/clj-kondo/master/script/install-clj-kondo
    chmod +x install-clj-kondo
    ./install-clj-kondo

To install to a different directory, append the option `--dir <dir>` to the
above command.  To download to a different directory, append the option
`--download-dir <dir>`. To install a specific version, use `--version <yyyy.mm.dd>`.

To upgrade, just run the script again.

## Brew (MacOS and Linux)

On MacOS you can use [brew](https://brew.sh/).  On Linux you can use
[Linuxbrew](http://linuxbrew.sh/).

To install with brew:

    brew install borkdude/brew/clj-kondo

To upgrade:

    brew upgrade clj-kondo

<!--
## Snap (Linux)

NOTE: using the Snap package, clj-kondo only has access to your home directory.

To install:

    sudo snap install clj-kondo

To give clj-kondo access to your home directory:

    sudo snap connect clj-kondo:home

To upgrade:

    sudo snap refresh clj-kondo
-->

## NPM (Linux, MacOS, Windows)

    npm install -g clj-kondo

## Arch (Linux)

`clj-kondo` is [available](https://aur.archlinux.org/packages/clj-kondo-bin/) in the [Arch User Repository](https://aur.archlinux.org). It can be installed using your favorite [AUR](https://aur.archlinux.org) helper such as
[yay](https://github.com/Jguer/yay), [yaourt](https://github.com/archlinuxfr/yaourt), [apacman](https://github.com/oshazard/apacman) and [pacaur](https://github.com/rmarquis/pacaur). Here is an example using `yay`:

    yay -S clj-kondo-bin

## NixOS

`clj-kondo` is available in the
[Nix Packages collection](https://github.com/NixOS/nixpkgs/blob/master/pkgs/development/tools/clj-kondo/default.nix).
To install it globally, add it to your `systemPackages`. If you just want to try it, you can do it in a Nix shell:

    nix-shell -p clj-kondo

## Scoop (Windows)

A Windows binary version of `clj-kondo.exe` is available via this [scoop bucket](https://github.com/littleli/scoop-clojure) which also has several other Clojure tools for Windows:

    scoop bucket add scoop-clojure https://github.com/littleli/scoop-clojure
    scoop install clj-kondo

To update:

    scoop update clj-kondo

## [Running on the JVM](jvm.md)

## [Running with Docker](docker.md)

<!-- ## Scoop (Windows)

Note: clj-kondo on Windows is considered experimental. Until we sort out [this issue](https://github.com/borkdude/clj-kondo/issues/276), the scoop package will not be updated. You can try the latest binary from [Github](https://github.com/borkdude/clj-kondo/releases).

To install clj-kondo on Windows you can use [scoop](https://scoop.sh):

     scoop bucket add borkdude https://github.com/borkdude/scoop-bucket
     scoop install clj-kondo

To upgrade:

    scoop update clj-kondo
-->
