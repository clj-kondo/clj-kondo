# Building from source

## Linux and MacOS

To build the `clj-kondo` binary from source:

* Download [GraalVM](https://github.com/graalvm/graalvm-ce-builds/releases) java11 22.3.0.
  Currently `clj-kondo` can only be built with java11 based graal VMs, so using JDK 8 builds will
  not work.

* Set the `GRAALVM_HOME` variable. E.g.:

        export GRAALVM_HOME=$HOME/Downloads/graalvm-ce-java11-22.3.0/Contents/Home

* git clone this repo and `cd clj-kondo`

* Finally:

        script/compile

Place the binary somewhere on your path.

### Optional steps

These steps are only necessary to update resources that are shipped with clj-kondo.

* To update the built-in cache for Clojure and ClojureScript:

        script/built-in

* JDK 12 is needed only in this step. To update static method
  information from commonly used Java classes:

      JAVA_HOME=<path to JDK 12> script/extract-java

  where JDK 12 is located in e.g. `~/Downloads/jdk-12.jdk/Contents/Home`.

## Windows

These steps assume a Windows 10 installation with [Git for Windows](https://gitforwindows.org/) and [leiningen](https://leiningen.org). We will be using the `java` version that comes with GraalVM, so there is no need to install it separately.

* Download [GraalVM](https://github.com/oracle/graal/releases) 22.3.0.

* Install the [Windows SDK 7.1](https://www.microsoft.com/en-us/download/details.aspx?id=8442). See [this link](https://stackoverflow.com/questions/20115186/what-sdk-version-to-download/22987999#22987999) for an explanation of which image is which.

  You might first have to:
    - Uninstall any Visual C++ 2010 Redistributables from the control panel. See this [SO answer](https://stackoverflow.com/a/32534158/6264).

  After installation of Windows SDK 7.1, you may also have to:
    - Install [Microsoft Visual C++ 2010 Service Pack 1 Compiler Update for the Windows SDK 7.1"](http://www.microsoft.com/en-us/download/details.aspx?displaylang=en&id=4422), because it may not have been installed with the Windows SDK 7.1 due to [an unfortunate situation](https://stackoverflow.com/questions/32091593/cannot-install-windows-sdk-7-1-on-windows-10).

The following steps need to be executed from the Windows SDK 7.1 command prompt.

*  set the `GRAALVM_HOME` environment variable:

         set GRAALVM_HOME=C:\Users\IEUSer\Downloads\graalvm-ce-22.3.0

* git clone this repo and `cd clj-kondo`

* Run `script\compile.bat`

If the script finished successfully, there is now a `clj-kondo.exe` in the
current directory. Place the binary somewhere on your path.
