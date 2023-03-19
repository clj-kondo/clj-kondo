# Editor integration

Before setting up your editor, see [Project setup](../README.md#project-setup)
on how to configure `clj-kondo` for your project. TL;DR: this involves creating
a `.clj-kondo` directory in the root of your project.

## Emacs

For integrating with Emacs, see
[flycheck-clj-kondo](https://github.com/borkdude/flycheck-clj-kondo).

For Spacemacs, check [here](#spacemacs) or get [flymake-kondor](https://github.com/turbo-cafe/flymake-kondor) if you are using flymake.

<!-- ### LSP server -->

<!-- Emacs has the [lsp-mode](https://github.com/emacs-lsp/lsp-mode) where you can configure multiple LSP servers for different programming languages. -->
<!-- To use `clj-kondo` as an LSP server, you can configure the `lsp-mode` server command to point to the `clj-kondo` lsp-server jar. Note that the LSP server does not provide features other than diagnostics. -->

<!-- For Spacemacs, see the [clj-kondo via LSP](https://practicalli.github.io/spacemacs/install-spacemacs/clj-kondo-via-lsp.html) article, which includes the use of an external script as the custom lsp command. -->

<!-- For Emacs, use the instructions below. -->

<!-- 1. Download the latest clj-kondo LSP server jar to your system. Go to the -->
<!--    [Github releases](https://github.com/clj-kondo/clj-kondo/releases) and look -->
<!--    for `clj-kondo-lsp-server-<version>-standalone.jar`. The jar is provided -->
<!--    since version `2019.11.23`. -->

<!-- 2. Configure your `lsp-mode` pointing to the clj-kondo lsp server jar that you downloaded, like the example below: -->

<!-- ```lisp -->
<!-- (use-package lsp-mode -->
<!--   :ensure t -->
<!--   :hook ((clojure-mode . lsp)) -->
<!--   :commands lsp -->
<!--   :custom -->
<!--   ((lsp-clojure-server-command '("java" "-jar" "/home/user/clj-kondo/clj-kondo-lsp-server.jar"))) -->
<!--   :config -->
<!--   (dolist (m '(clojure-mode -->
<!--                clojurescript-mode)) -->
<!--     (add-to-list 'lsp-language-id-configuration `(,m . "clojure")))) -->
<!-- ``` -->

### clojure-lsp

The [clojure-lsp](https://github.com/clojure-lsp/clojure-lsp) project bundles
clj-kondo as its analyzer and linter. When using it, there is no need to install
clj-kondo separately, although I found that using
[flycheck-clj-kondo](https://github.com/borkdude/flycheck-clj-kondo) in addition
to clojure-lsp has the following benefits:

- The feedback is less laggy / more instantaneous
- The squiggles are less overwhelming (only the first character of an expression rather than the whole expression)
- Linting still works for files outside of projects
- You can use a newer version of clj-kondo than what is bundled with clojure-lsp

When doing so, it's recommended to disable diagnostics via lsp-mode:

``` elisp
(setq lsp-diagnostics-provider :none)
```

Maybe there is a way to only disable the diagnostics provider for
clojure(script), if so, feel free to submit a PR.

I do recommend using clojure-lsp (with `lsp-mode`) since it provides the following additional features (based on clj-kondo's analysis)

- project initialization (analyze dependencies first, copy library configurations)
- navigation
- refactoring (renaming, etc)
- lens-mode (see the number of references and tests), enable with: `(setq lsp-lens-enable t)`
- call hierarchy

and more.

## Visual Studio Code

### clj-kondo extension

Install the
[clj-kondo](https://marketplace.visualstudio.com/items?itemName=borkdude.clj-kondo)
extension. It requires no additional installation (except Java).

The clj-kondo extension will also be installed together with
[Calva](https://github.com/BetterThanTomorrow/calva).

### clojure-lint extension

If you do not have Java installed you can still get clj-kondo linting using the [Clojure Lint](https://github.com/marcomorain/clojure-lint) extension, by
@marcomorain, which uses the [clj-kondo standalone executable](https://github.com/clj-kondo/clj-kondo/blob/master/doc/install.md).

## Atom

Atom requires clj-kondo to be on your `$PATH`. In Atom, there are a few ways to install:

1. `apm install linter-kondo linter linter-ui-default intentions busy-signal`
2. Install from the [Atom package](https://atom.io/packages/linter-kondo) page.
3. From inside Atom, go to Preferences > Extensions. Search for "linter-kondo" and click "Install" on the extension.

## Vim / Neovim

### ALE

This section is for Vim 8+ or Neovim.

1. Install [ALE](https://github.com/w0rp/ale) using your favorite plugin
   manager. This already has in-built support for clj-kondo.
2. In your `.vimrc`, add:

   ``` viml
   let g:ale_linters = {'clojure': ['clj-kondo']}
   ```

   to only have clj-kondo as the linter.

   To enable both clj-kondo and joker, add:

   ``` viml
   let g:ale_linters = {'clojure': ['clj-kondo', 'joker']}
   ```

3. Reload your `.vimrc` and it should start working.

<img src="../screenshots/vim.png">

### COC.NVIM

Follow instructions to install **COC.NVIM** https://github.com/neoclide/coc.nvim

Follow instructions to install **coc diagnostic**
https://github.com/iamcco/coc-diagnostic

Add the `diagnostic-languageserver.linter` and the
`diagnostic-languageserver.filetypes` to the `coc-settings.json`.  `CocConfig`
command can be used to open the `coc-settings.json` file.

```json
{
    "diagnostic-languageserver.linters": {
        "clj_kondo_lint": {
            "command": "clj-kondo",
            "debounce": 100,
            "args": [ "--lint", "%filepath"],
            "offsetLine": 0,
            "offsetColumn": 0,
            "sourceName": "clj-kondo",
            "formatLines": 1,
            "formatPattern": [
                "^[^:]+:(\\d+):(\\d+):\\s+([^:]+):\\s+(.*)$",
                {
                    "line": 1,
                    "column": 2,
                    "message": 4,
                    "security": 3
                }
            ],
            "securities": {
                    "error": "error",
                    "warning": "warning",
                    "note": "info"
            } 

        }
    },
    "diagnostic-languageserver.filetypes": {"clojure":"clj_kondo_lint"}
}
```

### Neomake

Neomake has built-in support for clj-kondo.
It will be enabled automatically when using neomake, no configuration required.

### Vanilla way

Create this file in `~/.config/nvim/compiler/clj-kondo.vim` or `~/.vim/compiler/clj-kondo.vim`.

``` viml
if exists("current_compiler")
  finish
endif
let current_compiler="clj-kondo"

if exists(":CompilerSet") != 2
  command -nargs=* CompilerSet setlocal <args>
endif

CompilerSet errorformat=%f:%l:%c:\ Parse\ %t%*[^:]:\ %m,%f:%l:%c:\ %t%*[^:]:\ %m
CompilerSet makeprg=clj-kondo\ --lint\ %
```

#### Usage

You can populate the quickfix list like so:

```
:compiler clj-kondo
:make
```

See [romainl's vanilla linting](https://gist.github.com/romainl/ce55ce6fdc1659c5fbc0f4224fd6ad29) for how to automatically execute linting and automatically open the quickfix.

If you have [vim-dispatch](https://github.com/tpope/vim-dispatch/) installed, you can use this command to be both async and more convenient:

```
:Dispatch -compiler=clj-kondo
```

### nvim-lint (Neovim 0.5+ only)

[nvim-lint](https://github.com/mfussenegger/nvim-lint) has built-in support for clj-kondo.

#### Configuration using [packer.nvim](https://github.com/wbthomason/packer.nvim)
```lua
use {
  "mfussenegger/nvim-lint",
  config = function()
    require("lint").linters_by_ft = {
      clojure = {"clj-kondo"},
      -- ... other linters
    }
  end,
}
```

## IntelliJ IDEA

Currently there are three ways to get clj-kondo integration in IntelliJ.
All three methods work well and have equivalent features.
Select your preferred plugin/version management preference between:

* Clojure Extras IntelliJ Plugin
* LSP (Language Server Protocol) plugin to run clj-kondo from a jar
* Cursive or ClojureKit + File Watchers plugin to run an installed binary clj-kondo

### Clojure Extras Plugin

Install the plugin from [IntelliJ IDEA Plugins Marketplace](https://plugins.jetbrains.com/plugin/18108-clojure-extras/).
You can setup a custom binary from the settings screen or just use the built-in version.

### LSP server

The clj-kondo LSP server provides diagnostics via the LSP protocol. To get it
working in IntelliJ follow these steps:

1. Download the latest clj-kondo LSP server jar to your system. Go to the
   [Github releases](https://github.com/clj-kondo/clj-kondo/releases) and look
   for `clj-kondo-lsp-server-<version>-standalone.jar`. The jar is provided
   since version `2019.11.23`.

2. Install the LSP Support plugin by gtache, either from the marketplace of via
   a zipfile downloaded from the a [Github
   release](https://github.com/gtache/intellij-lsp/releases). Version 1.6.0 or
   later is required.

   <img src="../screenshots/intellij-lsp.png" width="50%" align="right">

3. Configure the LSP Support plugin.
   - Go to Preferences / Languages & Frameworks / Language Server Protocol / Server definitions. Select
     `Raw command`.
   - In the `Extension` field enter `clj;cljs;cljc;edn`.
   -  In the command field enter `java -jar
      <path>` where `<path>` matches the downloaded
      jar file,
      e.g. `/Users/clj-kondo/clj-kondo-lsp-server-2019.11.23-standalone.jar`.

Now, when editing a Clojure file, you should get linting feedback.

### File Watchers + installed binary

<img src="../screenshots/intellij-let.png" width="50%" align="right">

Requires a syntax aware plugin such as [Cursive](https://cursive-ide.com) or [ClojureKit](https://github.com/gregsh/Clojure-Kit) installed for best results.

Install the [File Watchers](https://www.jetbrains.com/help/idea/settings-tools-file-watchers.html) plugin. This plugin is available for installation in the Community Edition, even though it is bundled in Ultimate, you don't need Ultimate to install it.

Repeat the below steps for the file types Clojure (`.clj`), ClojureScript (`.cljs`)
and CLJC (`.cljc`)<sup>1</sup>.

1. Under Preferences (File/Settings... in GNU/Linux) / Tools / File Watchers click `+` and choose the `<custom>`
   template
2. Choose a name. E.g. `clj-kondo <filetype>` (where `<filetype>` is one of
   Clojure, ClojureScript or CLJC)
3. In the File type field, choose the correct filetype
4. Scope: `Current file`
5. In the Program field, type `clj-kondo`
6. In the Arguments field, type `--lint $FilePath$`<br>
   You may use a custom config E.g `--lint $FilePath$ --config "{:lint-as {manifold.deferred/let-flow clojure.core/let}}"`
7. In the Working directory field, type `$FileDir$`
8. Enable `Create output file from stdout`
9. Show console: `Never`
10. In output filters put `$FILE_PATH$:$LINE$:$COLUMN$: $MESSAGE$`
    <img src="../screenshots/intellij-fw-config.png">
11. The newly created file-watcher "level" defaults to "Project". Change it to "Global" so that `clj-kondo` is active for all future projects
    <img src="../screenshots/intellij-fw-global.png">

<sup>1</sup> See [Reader Conditionals](https://clojure.org/guides/reader_conditionals) for more information on the `.cljc` extension.
CLJX (`.cljx`) is an extension that was used prior to CLJC but is no longer in wide use.

## Spacemacs

Ensure that:

1. `syntax-checking` is present in `dotspacemacs-configuration-layers`.
2. `clj-kondo` is available on PATH.

In the `.spacemacs` file:

### Installing on master branch

When using the stable `master` branch:

1. In `dotspacemacs-additional-packages` add `flycheck-clj-kondo`.
2. In the `dotspacemacs/user-config` function add the following:

   ``` elisp
   (use-package clojure-mode
    :ensure t
    :config
    (require 'flycheck-clj-kondo))
   ```

To install it alongside joker:

1. In `dotspacemacs-additional-packages` add `flycheck-clj-kondo` and `flycheck-joker`.
2. In the `dotspacemacs/user-config` function add the following:

   ``` elisp
   (use-package clojure-mode
    :ensure t
    :config
    (require 'flycheck-joker)
    (require 'flycheck-clj-kondo)
    (dolist (checker '(clj-kondo-clj clj-kondo-cljs clj-kondo-cljc clj-kondo-edn))
      (setq flycheck-checkers (cons checker (delq checker flycheck-checkers))))
    (dolist (checkers '((clj-kondo-clj . clojure-joker)
                        (clj-kondo-cljs . clojurescript-joker)
                        (clj-kondo-cljc . clojure-joker)
                        (clj-kondo-edn . edn-joker)))
      (flycheck-add-next-checker (car checkers) (cons 'error (cdr checkers)))))
   ```

### Installing on develop branch

If using the `develop` branch, clj-kondo is available as a part of the standard
clojure layer. This will become the way to install in the next stable
release of spacemacs.

To enable it:

1. Ensure the clojure layer is in the `dotspacemacs-configuration-layers`.
2. Add a variable called `clojure-enable-linters` with the value `'clj-kondo`.

It should look like this:

```elisp
dotspacemacs-configuration-layers
'(...
    (clojure :variables
             clojure-enable-linters 'clj-kondo)
 )
```

Reload the config to enable clj-kondo.

## Kakoune

### Manual Linting

Add the following to `~/.config/kak/kakrc`:

```kak
hook global WinSetOption filetype=clojure %{
    set-option window lintcmd 'clj-kondo --lint'
}
```

The `:lint` command will run `clj-kondo` and annotate the buffer with lint
warnings and errors.

### Automatic Linting on Idle

`clj-kondo` is fast enough to lint as you code!  If you want to do this, use
the following configuration:

```kak
hook global WinSetOption filetype=clojure %{
    set-option window lintcmd 'clj-kondo --lint'
    lint-enable
    hook -group lint-diagnostics window NormalIdle .* %{ lint; lint-show }
}
```

This works well, but tends to clear the message line too frequently.
The following work-around prevents linting from displaying the warning
and error counts on the message line:

```kak
define-command -hidden -override lint-show-counters %{}
```

## Sublime Text

Requires Sublime Text 3 or 4. Install [SublimeLinter](https://github.com/SublimeLinter/SublimeLinter) and [SublimeLinter-contrib-clj-kondo](https://github.com/ToxicFrog/SublimeLinter-contrib-clj-kondo) with Package Control. clj-kondo must be available on the `$PATH` to work.
