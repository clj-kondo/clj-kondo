---
name: Bug report
about: Create a report to help us improve
title: ''
labels: ''
assignees: ''

---

**version**

[ Please specify which version of clj-kondo you're using. You can find this with `clj-kondo --version`.]

**macro usage**

[ Is your bug related to macro usage? Consider using a configuration. If not, ignore this section. ]

See [config.md](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#unrecognized-macros) for related configuration options.

**platform**

[ Please specify which platform you are using clj-kondo on. Are you using the native version of clj-kondo, or are you running it on the JVM? ]

**editor**

[ If applicable, please specify which editor you are using clj-kondo with and which editor plugin you are using. ]

**problem**

[ Please provide a short and to the point description of the problem ]

**repro**

[ Please provide a minimal and complete reproduction of the problem, including a namespace form, which can be pasted in a repro.clj file. Please include the output of the invocation of `clj-kondo --lint repro.clj` (or `.cljs/.cljc`) from the command line. ]

**config**

[ Is your bug related to `.clj-kondo/config.edn`? Paste your entire
configuration here. Also check if the bug still occurs without using a
configuration, as the bug may be a result of malformed configuration. ]

**expected behavior**

[ What is the behavior you expected to see from clj-kondo? ]


[ Optional: if you are interested in doing a PR yourself, please leave a note. ]

[ Optional: if you or your organization is sponsoring this project, please write "**Sponsor**" in this ticket for higher priority. ]
