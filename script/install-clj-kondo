#!/usr/bin/env bash

# install script inspired by scripts for clojure and CircleCI CLI tool
# install latest version of clj-kondo or upgrades existing one

set -euo pipefail

default_install_dir="/usr/local/bin"
install_dir=$default_install_dir
default_download_dir="/tmp"
download_dir=$default_download_dir

print_help() {
    echo "Installs latest version of clj-kondo."
    echo -e
    echo "Usage:"
    echo "install [--dir <dir>] [--download-dir <download-dir>]"
    echo -e
    echo "Defaults:"
    echo " * Installation directory: ${default_install_dir}"
    echo " * Download directory: ${default_download_dir}"
    exit 1
}

while [[ $# -gt 0 ]]
do
    key="$1"
    if [[ -z "${2:-}" ]]; then
        print_help
    fi

    case $key in
        --dir)
            install_dir="$2"
            shift
            shift
            ;;
        --download-dir)
            download_dir="$2"
            shift
            shift
            ;;
        *)    # unknown option
            print_help
            shift
            ;;
    esac
done

latest_release="$(curl -s https://raw.githubusercontent.com/borkdude/clj-kondo/master/resources/CLJ_KONDO_RELEASED_VERSION)"

case "$(uname -s)" in
    Linux*)     platform=linux;;
    Darwin*)    platform=macos;;
esac

download_url="https://github.com/borkdude/clj-kondo/releases/download/v$latest_release/clj-kondo-$latest_release-$platform-amd64.zip"

cd "$download_dir"
echo -e "Downloading $download_url to $download_dir"
curl -o "clj-kondo-$latest_release-$platform-amd64.zip" -sL "https://github.com/borkdude/clj-kondo/releases/download/v$latest_release/clj-kondo-$latest_release-$platform-amd64.zip"
unzip -qqo "clj-kondo-$latest_release-$platform-amd64.zip"
rm "clj-kondo-$latest_release-$platform-amd64.zip"

cd "$install_dir"
if [ -f clj-kondo ]; then
    echo "Moving $install_dir/clj-kondo to $install_dir/clj-kondo.old"
fi

mv -f "$download_dir/clj-kondo" "$PWD/clj-kondo"

echo "Successfully installed clj-kondo in $install_dir"
