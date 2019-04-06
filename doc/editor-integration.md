# Editor integration

Before setting up your editor, read [Project setup](../README.md#project-setup)
to configure the cache for your project.

## Emacs

For integrating with Emacs, see
[flycheck-clj-kondo](https://github.com/borkdude/flycheck-clj-kondo).

## IntelliJ IDEA

1. Install the [File
Watchers](https://www.jetbrains.com/help/idea/settings-tools-file-watchers.html)
plugin.
2. Under Preferences / Tools / File Watchers click `+` and choose the `<custom>` template.
3. Choose a name. E.g. `clj-kondo .clj`.
4. Choose the filetype Clojure.
5. In the Program field, type `clj-kondo`.
6. In the Arguments field, type `--lint $FilePath$ --cache`.
7. In the Working directory field, type `$ContentRoot$` which is the root of this project, the same directory in which your `.clj-kondo` directory resides.
8. Enable `Create output file from stdout`
9. In output filters put `$FILE_PATH$:$LINE$:$COLUMN$: .*: $MESSAGE$`.

Repeat steps 2-9 for the file types ClojureScript and CLJC.

<img src="../screenshots/intellij-fw-config.png" align="right">
