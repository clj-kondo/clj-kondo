(ns clj-kondo.impl.var-info-gen
  "GENERATED, DO NOT EDIT."
  {:no-doc true})
  (in-ns 'clj-kondo.impl.var-info)
  (def predicates '{clojure.core.memoize #{memoized?}, clojure.java.jdbc #{db-is-rollback-only}, clojure.core.cache #{has?}, clojure.core.async.impl.ioc-macros #{instruction? persistent-value? nested-go? finished?}, clojure.core #{decimal? contains? every? qualified-keyword? satisfies? seq? fn? = vector? thread-bound? any? < NaN? boolean? char? some? inst? future-done? simple-symbol? pos? sequential? neg? reduced? float? set? <= reversible? bound? map? volatile? var? empty? string? uri? double? map-entry? > int? associative? keyword? even? tagged-literal? extends? indexed? counted? future? zero? simple-keyword? not-every? class? future-cancelled? neg-int? sorted? nil? instance? bytes? record? identical? ident? qualified-ident? true? reader-conditional? >= integer? infinite? special-symbol? ratio? delay? ifn? nat-int? chunked-seq? distinct? pos-int? odd? uuid? false? list? simple-ident? == rational? realized? number? not-any? qualified-symbol? seqable? symbol? coll?}, clojure.set #{superset? subset?}, clojure.tools.trace #{traced? traceable?}, clojure.core.async.impl.dispatch #{in-dispatch-thread?}, clojure.string #{ends-with? starts-with? includes? blank?}})


  (def clojure-core-syms '#{*
*'
*1
*2
*3
*agent*
*allow-unresolved-vars*
*assert*
*clojure-version*
*command-line-args*
*compile-files*
*compile-path*
*compiler-options*
*data-readers*
*default-data-reader-fn*
*e
*err*
*file*
*flush-on-newline*
*fn-loader*
*in*
*math-context*
*ns*
*out*
*print-dup*
*print-length*
*print-level*
*print-meta*
*print-namespace-maps*
*print-readably*
*read-eval*
*reader-resolver*
*source-path*
*suppress-read*
*unchecked-math*
*use-context-classloader*
*verbose-defrecords*
*warn-on-reflection*
+
+'
-
-'
->
->>
->ArrayChunk
->Eduction
->Vec
->VecNode
->VecSeq
-cache-protocol-fn
-reset-methods
..
/
<
<=
=
==
>
>=
ArrayChunk
ArrayManager
EMPTY-NODE
Eduction
IVecImpl
Inst
NaN?
PrintWriter-on
StackTraceElement->vec
Throwable->map
Vec
VecNode
VecSeq
abs
accessor
aclone
add-classpath
add-tap
add-watch
agent
agent-error
agent-errors
aget
alength
alias
all-ns
alter
alter-meta!
alter-var-root
amap
ancestors
and
any?
apply
areduce
array-map
as->
aset
aset-boolean
aset-byte
aset-char
aset-double
aset-float
aset-int
aset-long
aset-short
assert
assert-same-protocol
assoc
assoc!
assoc-in
associative?
atom
await
await-for
await1
bases
bean
bigdec
bigint
biginteger
binding
bit-and
bit-and-not
bit-clear
bit-flip
bit-not
bit-or
bit-set
bit-shift-left
bit-shift-right
bit-test
bit-xor
boolean
boolean-array
boolean?
booleans
bound-fn
bound-fn*
bound?
bounded-count
butlast
byte
byte-array
bytes
bytes?
case
case-fallthrough-err-impl
cast
cat
char
char-array
char-escape-string
char-name-string
char?
chars
chunk
chunk-append
chunk-buffer
chunk-cons
chunk-first
chunk-next
chunk-rest
chunked-seq?
class
class?
clear-agent-errors
clojure-version
coll?
comment
commute
comp
comparator
compare
compare-and-set!
compile
complement
completing
concat
cond
cond->
cond->>
condp
conj
conj!
cons
constantly
construct-proxy
contains?
count
counted?
create-ns
create-struct
cycle
dec
dec'
decimal?
declare
dedupe
default-data-readers
definline
definterface
defmacro
defmethod
defmulti
defn
defn-
defonce
defprotocol
defrecord
defstruct
deftype
delay
delay?
deliver
denominator
deref
derive
descendants
destructure
disj
disj!
dissoc
dissoc!
distinct
distinct?
doall
dorun
doseq
dosync
dotimes
doto
double
double-array
double?
doubles
drop
drop-last
drop-while
eduction
empty
empty?
ensure
ensure-reduced
enumeration-seq
error-handler
error-mode
eval
even?
every-pred
every?
ex-cause
ex-data
ex-info
ex-message
extend
extend-protocol
extend-type
extenders
extends?
false?
ffirst
file-seq
filter
filterv
find
find-keyword
find-ns
find-protocol-impl
find-protocol-method
find-var
first
flatten
float
float-array
float?
floats
flush
fn
fn?
fnext
fnil
for
force
format
frequencies
future
future-call
future-cancel
future-cancelled?
future-done?
future?
gen-and-load-class
gen-class
gen-interface
gensym
get
get-in
get-method
get-proxy-class
get-thread-bindings
get-validator
group-by
halt-when
hash
hash-combine
hash-map
hash-ordered-coll
hash-set
hash-unordered-coll
ident?
identical?
identity
if-let
if-not
if-some
ifn?
import
in-ns
inc
inc'
indexed?
infinite?
init-proxy
inst-ms
inst-ms*
inst?
instance?
int
int-array
int?
integer?
interleave
intern
interpose
into
into-array
ints
io!
isa?
iterate
iteration
iterator-seq
juxt
keep
keep-indexed
key
keys
keyword
keyword?
last
lazy-cat
lazy-seq
let
letfn
line-seq
list
list*
list?
load
load-file
load-reader
load-string
loaded-libs
locking
long
long-array
longs
loop
macroexpand
macroexpand-1
make-array
make-hierarchy
map
map-entry?
map-indexed
map?
mapcat
mapv
max
max-key
memfn
memoize
merge
merge-with
meta
method-sig
methods
min
min-key
mix-collection-hash
mod
munge
name
namespace
namespace-munge
nat-int?
neg-int?
neg?
newline
next
nfirst
nil?
nnext
not
not-any?
not-empty
not-every?
not=
ns
ns-aliases
ns-imports
ns-interns
ns-map
ns-name
ns-publics
ns-refers
ns-resolve
ns-unalias
ns-unmap
nth
nthnext
nthrest
num
number?
numerator
object-array
odd?
or
parents
parse-boolean
parse-double
parse-long
parse-uuid
partial
partition
partition-all
partition-by
pcalls
peek
persistent!
pmap
pop
pop!
pop-thread-bindings
pos-int?
pos?
pr
pr-str
prefer-method
prefers
primitives-classnames
print
print-ctor
print-dup
print-method
print-simple
print-str
printf
println
println-str
prn
prn-str
promise
proxy
proxy-call-with-super
proxy-mappings
proxy-name
proxy-super
push-thread-bindings
pvalues
qualified-ident?
qualified-keyword?
qualified-symbol?
quot
rand
rand-int
rand-nth
random-sample
random-uuid
range
ratio?
rational?
rationalize
re-find
re-groups
re-matcher
re-matches
re-pattern
re-seq
read
read+string
read-line
read-string
reader-conditional
reader-conditional?
realized?
record?
reduce
reduce-kv
reduced
reduced?
reductions
ref
ref-history-count
ref-max-history
ref-min-history
ref-set
refer
refer-clojure
reify
release-pending-sends
rem
remove
remove-all-methods
remove-method
remove-ns
remove-tap
remove-watch
repeat
repeatedly
replace
replicate
require
requiring-resolve
reset!
reset-meta!
reset-vals!
resolve
rest
restart-agent
resultset-seq
reverse
reversible?
rseq
rsubseq
run!
satisfies?
second
select-keys
send
send-off
send-via
seq
seq-to-map-for-destructuring
seq?
seqable?
seque
sequence
sequential?
set
set-agent-send-executor!
set-agent-send-off-executor!
set-error-handler!
set-error-mode!
set-validator!
set?
short
short-array
shorts
shuffle
shutdown-agents
simple-ident?
simple-keyword?
simple-symbol?
slurp
some
some->
some->>
some-fn
some?
sort
sort-by
sorted-map
sorted-map-by
sorted-set
sorted-set-by
sorted?
special-symbol?
spit
split-at
split-with
str
string?
struct
struct-map
subs
subseq
subvec
supers
swap!
swap-vals!
symbol
symbol?
sync
tagged-literal
tagged-literal?
take
take-last
take-nth
take-while
tap>
test
the-ns
thread-bound?
time
to-array
to-array-2d
trampoline
transduce
transient
tree-seq
true?
type
unchecked-add
unchecked-add-int
unchecked-byte
unchecked-char
unchecked-dec
unchecked-dec-int
unchecked-divide-int
unchecked-double
unchecked-float
unchecked-inc
unchecked-inc-int
unchecked-int
unchecked-long
unchecked-multiply
unchecked-multiply-int
unchecked-negate
unchecked-negate-int
unchecked-remainder-int
unchecked-short
unchecked-subtract
unchecked-subtract-int
underive
unquote
unquote-splicing
unreduced
unsigned-bit-shift-right
update
update-in
update-keys
update-proxy
update-vals
uri?
use
uuid?
val
vals
var-get
var-set
var?
vary-meta
vec
vector
vector-of
vector?
volatile!
volatile?
vreset!
vswap!
when
when-first
when-let
when-not
when-some
while
with-bindings
with-bindings*
with-in-str
with-loading-context
with-local-vars
with-meta
with-open
with-out-str
with-precision
with-redefs
with-redefs-fn
xml-seq
zero?
zipmap})

  (def cljs-core-syms '#{*
*1
*2
*3
*assert*
*clojurescript-version*
*command-line-args*
*e
*eval*
*exec-tap-fn*
*flush-on-newline*
*global*
*loaded-libs*
*main-cli-fn*
*ns*
*out*
*print-dup*
*print-err-fn*
*print-fn*
*print-fn-bodies*
*print-length*
*print-level*
*print-meta*
*print-namespace-maps*
*print-newline*
*print-readably*
*target*
*unchecked-arrays*
*unchecked-if*
*warn-on-infer*
+
-
--destructure-map
->
->>
->ArrayChunk
->ArrayIter
->ArrayList
->ArrayNode
->ArrayNodeIterator
->ArrayNodeSeq
->Atom
->BitmapIndexedNode
->BlackNode
->Box
->ChunkBuffer
->ChunkedCons
->ChunkedSeq
->Cons
->Cycle
->Delay
->ES6EntriesIterator
->ES6Iterator
->ES6IteratorSeq
->ES6SetEntriesIterator
->Eduction
->Empty
->EmptyList
->HashCollisionNode
->HashMapIter
->HashSetIter
->IndexedSeq
->IndexedSeqIterator
->IntegerRange
->IntegerRangeChunk
->Iterate
->KeySeq
->Keyword
->LazySeq
->List
->Many
->MapEntry
->MetaFn
->MultiFn
->MultiIterator
->Namespace
->NeverEquiv
->NodeIterator
->NodeSeq
->ObjMap
->PersistentArrayMap
->PersistentArrayMapIterator
->PersistentArrayMapSeq
->PersistentHashMap
->PersistentHashSet
->PersistentQueue
->PersistentQueueIter
->PersistentQueueSeq
->PersistentTreeMap
->PersistentTreeMapSeq
->PersistentTreeSet
->PersistentVector
->RSeq
->Range
->RangeIterator
->RangedIterator
->RecordIter
->RedNode
->Reduced
->Repeat
->SeqIter
->Single
->StringBufferWriter
->StringIter
->Subvec
->Symbol
->TaggedLiteral
->TransformerIterator
->TransientArrayMap
->TransientHashMap
->TransientHashSet
->TransientVector
->UUID
->ValSeq
->Var
->VectorNode
->Volatile
-add-method
-add-watch
-as-transient
-assoc
-assoc!
-assoc-n
-assoc-n!
-chunked-first
-chunked-next
-chunked-rest
-clj->js
-clone
-comparator
-compare
-conj
-conj!
-contains-key?
-count
-default-dispatch-val
-deref
-deref-with-timeout
-disjoin
-disjoin!
-dispatch-fn
-dissoc
-dissoc!
-drop-first
-empty
-entry-key
-equiv
-find
-first
-flush
-get-method
-hash
-invoke
-iterator
-js->clj
-key
-key->js
-kv-reduce
-lookup
-meta
-methods
-name
-namespace
-next
-notify-watches
-nth
-peek
-persistent!
-pop
-pop!
-pr-writer
-prefer-method
-prefers
-realized?
-reduce
-remove-method
-remove-watch
-reset
-reset!
-rest
-rseq
-seq
-sorted-seq
-sorted-seq-from
-swap!
-val
-vreset!
-with-meta
-write
..
/
<
<=
=
==
>
>=
APersistentVector
ASeq
ArrayChunk
ArrayIter
ArrayList
ArrayNode
ArrayNodeIterator
ArrayNodeSeq
Atom
BitmapIndexedNode
BlackNode
Box
CHAR_MAP
ChunkBuffer
ChunkedCons
ChunkedSeq
Cons
Cycle
DEMUNGE_MAP
DEMUNGE_PATTERN
Delay
ES6EntriesIterator
ES6Iterator
ES6IteratorSeq
ES6SetEntriesIterator
Eduction
Empty
EmptyList
ExceptionInfo
Fn
HashCollisionNode
HashMapIter
HashSetIter
IAssociative
IAtom
IChunk
IChunkedNext
IChunkedSeq
ICloneable
ICollection
IComparable
ICounted
IDeref
IDerefWithTimeout
IEditableCollection
IEmptyableCollection
IEncodeClojure
IEncodeJS
IEquiv
IFind
IFn
IHash
IIndexed
IIterable
IKVReduce
IList
ILookup
IMap
IMapEntry
IMeta
IMultiFn
INIT
INamed
INext
IPending
IPrintWithWriter
IRecord
IReduce
IReset
IReversible
ISeq
ISeqable
ISequential
ISet
ISorted
IStack
ISwap
ITER_SYMBOL
ITransientAssociative
ITransientCollection
ITransientMap
ITransientSet
ITransientVector
IUUID
IVector
IVolatile
IWatchable
IWithMeta
IWriter
IndexedSeq
IndexedSeqIterator
Inst
IntegerRange
IntegerRangeChunk
Iterate
KeySeq
Keyword
LazySeq
List
LongImpl
MODULE_INFOS
MODULE_URIS
Many
MapEntry
MetaFn
MultiFn
MultiIterator
NS_CACHE
NaN?
Namespace
NeverEquiv
NodeIterator
NodeSeq
ObjMap
PROTOCOL_SENTINEL
PersistentArrayMap
PersistentArrayMapIterator
PersistentArrayMapSeq
PersistentHashMap
PersistentHashSet
PersistentQueue
PersistentQueueIter
PersistentQueueSeq
PersistentTreeMap
PersistentTreeMapSeq
PersistentTreeSet
PersistentVector
RSeq
Range
RangeIterator
RangedIterator
RecordIter
RedNode
Reduced
Repeat
START
SeqIter
Single
StringBufferWriter
StringIter
Subvec
Symbol
TaggedLiteral
TransformerIterator
TransientArrayMap
TransientHashMap
TransientHashSet
TransientVector
UUID
ValSeq
Var
VectorNode
Volatile
abs
aclone
add-tap
add-to-string-hash-cache
add-watch
aget
alength
alter-meta!
amap
ancestors
and
any?
apply
areduce
array
array-chunk
array-index-of
array-iter
array-list
array-map
array-seq
array?
as->
aset
assert
assoc
assoc!
assoc-in
associative?
atom
binding
bit-and
bit-and-not
bit-clear
bit-count
bit-flip
bit-not
bit-or
bit-set
bit-shift-left
bit-shift-right
bit-shift-right-zero-fill
bit-test
bit-xor
bitpos
boolean
boolean?
booleans
bounded-count
butlast
byte
bytes
caching-hash
case
cat
char
char?
chars
chunk
chunk-append
chunk-buffer
chunk-cons
chunk-first
chunk-next
chunk-rest
chunked-seq
chunked-seq?
clj->js
clone
cloneable?
coercive-=
coercive-boolean
coercive-not
coercive-not=
coll?
comment
comp
comparator
compare
compare-and-set!
complement
completing
concat
cond
cond->
cond->>
condp
conj
conj!
cons
constantly
contains?
copy-arguments
count
counted?
create-ns
cycle
dec
declare
dedupe
default-dispatch-val
defmacro
defmethod
defmulti
defn
defn-
defonce
defprotocol
defrecord
deftype
delay
delay?
demunge
deref
derive
descendants
destructure
disj
disj!
dispatch-fn
dissoc
dissoc!
distinct
distinct?
divide
doall
dorun
doseq
dotimes
doto
double
double-array
double?
doubles
drop
drop-last
drop-while
dt->et
eduction
empty
empty?
enable-console-print!
ensure-reduced
equiv-map
es6-entries-iterator
es6-iterable
es6-iterator
es6-iterator-seq
es6-set-entries-iterator
eval
even?
every-pred
every?
ex-cause
ex-data
ex-info
ex-message
exists?
extend-protocol
extend-type
false?
fast-path-protocol-partitions-count
fast-path-protocols
ffirst
filter
filterv
find
find-macros-ns
find-ns
find-ns-obj
first
flatten
float
float?
floats
flush
fn
fn?
fnext
fnil
for
force
frequencies
gen-apply-to
gen-apply-to-simple
gensym
gensym_counter
get
get-in
get-method
get-validator
goog-define
goog.DEBUG
goog.global
group-by
halt-when
hash
hash-combine
hash-keyword
hash-map
hash-ordered-coll
hash-set
hash-string
hash-string*
hash-unordered-coll
ident?
identical?
identity
if-let
if-not
if-some
ifind?
ifn?
implements?
import
import-macros
imul
inc
indexed?
infinite?
inst-ms
inst-ms*
inst?
instance?
int
int-array
int-rotate-left
int?
integer?
interleave
interpose
into
into-array
ints
is_proto_
isa?
iter
iterable?
iterate
iteration
js*
js->clj
js-arguments
js-comment
js-debugger
js-delete
js-fn?
js-in
js-inline-comment
js-invoke
js-iterable?
js-keys
js-mod
js-obj
js-reserved
js-str
js-symbol?
juxt
keep
keep-indexed
key
key->js
key-test
keys
keyword
keyword-identical?
keyword?
last
lazy-cat
lazy-seq
let
letfn
list
list*
list?
load-file
load-file*
locking
long
long-array
longs
loop
m3-C1
m3-C2
m3-fmix
m3-hash-int
m3-hash-unencoded-chars
m3-mix-H1
m3-mix-K1
m3-seed
macroexpand
macroexpand-1
make-array
make-hierarchy
map
map-entry?
map-indexed
map?
mapcat
mapv
mask
max
max-key
memfn
memoize
merge
merge-with
meta
methods
min
min-key
missing-protocol
mix-collection-hash
mk-bound-fn
mod
munge
name
namespace
nat-int?
native-satisfies?
neg-int?
neg?
newline
next
nfirst
nil-iter
nil?
nnext
not
not-any?
not-empty
not-every?
not-native
not=
ns
ns-imports
ns-interns
ns-interns*
ns-name
ns-publics
ns-unmap
nth
nthnext
nthrest
number?
obj-map
object-array
object?
odd?
or
parents
parse-boolean
parse-double
parse-long
parse-uuid
partial
partition
partition-all
partition-by
peek
persistent!
persistent-array-map-seq
pop
pop!
pos-int?
pos?
pr
pr-seq-writer
pr-sequential-writer
pr-str
pr-str*
pr-str-with-opts
prefer-method
prefers
prim-seq
print
print-map
print-meta?
print-prefix-map
print-str
println
println-str
prn
prn-str
prn-str-with-opts
qualified-ident?
qualified-keyword?
qualified-symbol?
quot
rand
rand-int
rand-nth
random-sample
random-uuid
range
ranged-iterator
re-find
re-matches
re-pattern
re-seq
realized?
record?
reduce
reduce-kv
reduceable?
reduced
reduced?
reductions
refer-clojure
regexp?
reify
rem
remove
remove-all-methods
remove-method
remove-tap
remove-watch
repeat
repeatedly
replace
replicate
require
require-macros
reset!
reset-meta!
reset-vals!
resolve
rest
reverse
reversible?
rseq
rsubseq
run!
satisfies?
second
select-keys
seq
seq-iter
seq-to-map-for-destructuring
seq?
seqable?
sequence
sequential?
set
set-from-indexed-seq
set-print-err-fn!
set-print-fn!
set-validator!
set?
short
shorts
shuffle
simple-benchmark
simple-ident?
simple-keyword?
simple-symbol?
some
some->
some->>
some-fn
some?
sort
sort-by
sorted-map
sorted-map-by
sorted-set
sorted-set-by
sorted?
special-symbol?
specify
specify!
split-at
split-with
spread
str
string-hash-cache
string-hash-cache-count
string-iter
string-print
string?
subs
subseq
subvec
swap!
swap-vals!
symbol
symbol-identical?
symbol?
system-time
tagged-literal
tagged-literal?
take
take-last
take-nth
take-while
tap>
test
this-as
time
to-array
to-array-2d
trampoline
transduce
transformer-iterator
transient
tree-seq
true?
truth_
type
type->str
unchecked-add
unchecked-add-int
unchecked-byte
unchecked-char
unchecked-dec
unchecked-dec-int
unchecked-divide-int
unchecked-double
unchecked-float
unchecked-get
unchecked-inc
unchecked-inc-int
unchecked-int
unchecked-long
unchecked-multiply
unchecked-multiply-int
unchecked-negate
unchecked-negate-int
unchecked-remainder-int
unchecked-set
unchecked-short
unchecked-subtract
unchecked-subtract-int
undefined?
underive
unreduced
unsafe-bit-and
unsafe-cast
unsigned-bit-shift-right
update
update-in
update-keys
update-vals
uri?
use
use-macros
uuid
uuid?
val
vals
var?
vary-meta
vec
vector
vector?
volatile!
volatile?
vreset!
vswap!
when
when-first
when-let
when-not
when-some
while
with-meta
with-out-str
with-redefs
write-all
zero?
zipmap})

  (def default-import->qname '{AbstractMethodError java.lang.AbstractMethodError
Appendable java.lang.Appendable
ArithmeticException java.lang.ArithmeticException
ArrayIndexOutOfBoundsException java.lang.ArrayIndexOutOfBoundsException
ArrayStoreException java.lang.ArrayStoreException
AssertionError java.lang.AssertionError
BigDecimal java.math.BigDecimal
BigInteger java.math.BigInteger
Boolean java.lang.Boolean
Byte java.lang.Byte
Callable java.util.concurrent.Callable
CharSequence java.lang.CharSequence
Character java.lang.Character
Class java.lang.Class
ClassCastException java.lang.ClassCastException
ClassCircularityError java.lang.ClassCircularityError
ClassFormatError java.lang.ClassFormatError
ClassLoader java.lang.ClassLoader
ClassNotFoundException java.lang.ClassNotFoundException
CloneNotSupportedException java.lang.CloneNotSupportedException
Cloneable java.lang.Cloneable
Comparable java.lang.Comparable
Compiler clojure.lang.Compiler
Deprecated java.lang.Deprecated
Double java.lang.Double
Enum java.lang.Enum
EnumConstantNotPresentException java.lang.EnumConstantNotPresentException
Error java.lang.Error
Exception java.lang.Exception
ExceptionInInitializerError java.lang.ExceptionInInitializerError
Float java.lang.Float
IllegalAccessError java.lang.IllegalAccessError
IllegalAccessException java.lang.IllegalAccessException
IllegalArgumentException java.lang.IllegalArgumentException
IllegalMonitorStateException java.lang.IllegalMonitorStateException
IllegalStateException java.lang.IllegalStateException
IllegalThreadStateException java.lang.IllegalThreadStateException
IncompatibleClassChangeError java.lang.IncompatibleClassChangeError
IndexOutOfBoundsException java.lang.IndexOutOfBoundsException
InheritableThreadLocal java.lang.InheritableThreadLocal
InstantiationError java.lang.InstantiationError
InstantiationException java.lang.InstantiationException
Integer java.lang.Integer
InternalError java.lang.InternalError
InterruptedException java.lang.InterruptedException
Iterable java.lang.Iterable
LinkageError java.lang.LinkageError
Long java.lang.Long
Math java.lang.Math
NegativeArraySizeException java.lang.NegativeArraySizeException
NoClassDefFoundError java.lang.NoClassDefFoundError
NoSuchFieldError java.lang.NoSuchFieldError
NoSuchFieldException java.lang.NoSuchFieldException
NoSuchMethodError java.lang.NoSuchMethodError
NoSuchMethodException java.lang.NoSuchMethodException
NullPointerException java.lang.NullPointerException
Number java.lang.Number
NumberFormatException java.lang.NumberFormatException
Object java.lang.Object
OutOfMemoryError java.lang.OutOfMemoryError
Override java.lang.Override
Package java.lang.Package
Process java.lang.Process
ProcessBuilder java.lang.ProcessBuilder
Readable java.lang.Readable
Runnable java.lang.Runnable
Runtime java.lang.Runtime
RuntimeException java.lang.RuntimeException
RuntimePermission java.lang.RuntimePermission
SecurityException java.lang.SecurityException
SecurityManager java.lang.SecurityManager
Short java.lang.Short
StackOverflowError java.lang.StackOverflowError
StackTraceElement java.lang.StackTraceElement
StrictMath java.lang.StrictMath
String java.lang.String
StringBuffer java.lang.StringBuffer
StringBuilder java.lang.StringBuilder
StringIndexOutOfBoundsException java.lang.StringIndexOutOfBoundsException
SuppressWarnings java.lang.SuppressWarnings
System java.lang.System
Thread java.lang.Thread
Thread$State java.lang.Thread$State
Thread$UncaughtExceptionHandler java.lang.Thread$UncaughtExceptionHandler
ThreadDeath java.lang.ThreadDeath
ThreadGroup java.lang.ThreadGroup
ThreadLocal java.lang.ThreadLocal
Throwable java.lang.Throwable
TypeNotPresentException java.lang.TypeNotPresentException
UnknownError java.lang.UnknownError
UnsatisfiedLinkError java.lang.UnsatisfiedLinkError
UnsupportedClassVersionError java.lang.UnsupportedClassVersionError
UnsupportedOperationException java.lang.UnsupportedOperationException
VerifyError java.lang.VerifyError
VirtualMachineError java.lang.VirtualMachineError
Void java.lang.Void})

  (def default-fq-imports '#{clojure.lang.Compiler
java.lang.AbstractMethodError
java.lang.Appendable
java.lang.ArithmeticException
java.lang.ArrayIndexOutOfBoundsException
java.lang.ArrayStoreException
java.lang.AssertionError
java.lang.Boolean
java.lang.Byte
java.lang.CharSequence
java.lang.Character
java.lang.Class
java.lang.ClassCastException
java.lang.ClassCircularityError
java.lang.ClassFormatError
java.lang.ClassLoader
java.lang.ClassNotFoundException
java.lang.CloneNotSupportedException
java.lang.Cloneable
java.lang.Comparable
java.lang.Deprecated
java.lang.Double
java.lang.Enum
java.lang.EnumConstantNotPresentException
java.lang.Error
java.lang.Exception
java.lang.ExceptionInInitializerError
java.lang.Float
java.lang.IllegalAccessError
java.lang.IllegalAccessException
java.lang.IllegalArgumentException
java.lang.IllegalMonitorStateException
java.lang.IllegalStateException
java.lang.IllegalThreadStateException
java.lang.IncompatibleClassChangeError
java.lang.IndexOutOfBoundsException
java.lang.InheritableThreadLocal
java.lang.InstantiationError
java.lang.InstantiationException
java.lang.Integer
java.lang.InternalError
java.lang.InterruptedException
java.lang.Iterable
java.lang.LinkageError
java.lang.Long
java.lang.Math
java.lang.NegativeArraySizeException
java.lang.NoClassDefFoundError
java.lang.NoSuchFieldError
java.lang.NoSuchFieldException
java.lang.NoSuchMethodError
java.lang.NoSuchMethodException
java.lang.NullPointerException
java.lang.Number
java.lang.NumberFormatException
java.lang.Object
java.lang.OutOfMemoryError
java.lang.Override
java.lang.Package
java.lang.Process
java.lang.ProcessBuilder
java.lang.Readable
java.lang.Runnable
java.lang.Runtime
java.lang.RuntimeException
java.lang.RuntimePermission
java.lang.SecurityException
java.lang.SecurityManager
java.lang.Short
java.lang.StackOverflowError
java.lang.StackTraceElement
java.lang.StrictMath
java.lang.String
java.lang.StringBuffer
java.lang.StringBuilder
java.lang.StringIndexOutOfBoundsException
java.lang.SuppressWarnings
java.lang.System
java.lang.Thread
java.lang.Thread$State
java.lang.Thread$UncaughtExceptionHandler
java.lang.ThreadDeath
java.lang.ThreadGroup
java.lang.ThreadLocal
java.lang.Throwable
java.lang.TypeNotPresentException
java.lang.UnknownError
java.lang.UnsatisfiedLinkError
java.lang.UnsupportedClassVersionError
java.lang.UnsupportedOperationException
java.lang.VerifyError
java.lang.VirtualMachineError
java.lang.Void
java.math.BigDecimal
java.math.BigInteger
java.util.concurrent.Callable})

  (def unused-values '#{[clojure.core/* {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/*' {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/*1 {:var-kind nil, :macro nil}]
[clojure.core/*2 {:var-kind nil, :macro nil}]
[clojure.core/*3 {:var-kind nil, :macro nil}]
[clojure.core/*agent* {:var-kind nil, :macro nil}]
[clojure.core/*allow-unresolved-vars* {:var-kind nil, :macro nil}]
[clojure.core/*assert* {:var-kind nil, :macro nil}]
[clojure.core/*clojure-version* {:var-kind nil, :macro nil}]
[clojure.core/*command-line-args* {:var-kind nil, :macro nil}]
[clojure.core/*compile-files* {:var-kind nil, :macro nil}]
[clojure.core/*compile-path* {:var-kind nil, :macro nil}]
[clojure.core/*compiler-options* {:var-kind nil, :macro nil}]
[clojure.core/*data-readers* {:var-kind nil, :macro nil}]
[clojure.core/*default-data-reader-fn* {:var-kind nil, :macro nil}]
[clojure.core/*e {:var-kind nil, :macro nil}]
[clojure.core/*err* {:var-kind nil, :macro nil}]
[clojure.core/*file* {:var-kind nil, :macro nil}]
[clojure.core/*flush-on-newline* {:var-kind nil, :macro nil}]
[clojure.core/*fn-loader* {:var-kind nil, :macro nil}]
[clojure.core/*in* {:var-kind nil, :macro nil}]
[clojure.core/*math-context* {:var-kind nil, :macro nil}]
[clojure.core/*ns* {:var-kind nil, :macro nil}]
[clojure.core/*out* {:var-kind nil, :macro nil}]
[clojure.core/*print-dup* {:var-kind nil, :macro nil}]
[clojure.core/*print-length* {:var-kind nil, :macro nil}]
[clojure.core/*print-level* {:var-kind nil, :macro nil}]
[clojure.core/*print-meta* {:var-kind nil, :macro nil}]
[clojure.core/*print-namespace-maps* {:var-kind nil, :macro nil}]
[clojure.core/*print-readably* {:var-kind nil, :macro nil}]
[clojure.core/*read-eval* {:var-kind nil, :macro nil}]
[clojure.core/*reader-resolver* {:var-kind nil, :macro nil}]
[clojure.core/*source-path* {:var-kind nil, :macro nil}]
[clojure.core/*suppress-read* {:var-kind nil, :macro nil}]
[clojure.core/*unchecked-math* {:var-kind nil, :macro nil}]
[clojure.core/*use-context-classloader* {:var-kind nil, :macro nil}]
[clojure.core/*verbose-defrecords* {:var-kind nil, :macro nil}]
[clojure.core/*warn-on-reflection* {:var-kind nil, :macro nil}]
[clojure.core/+ {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/+' {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/- {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/-' {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/-> {:var-kind nil, :macro true}]
[clojure.core/->> {:var-kind nil, :macro true}]
[clojure.core/->ArrayChunk {:var-kind nil, :macro nil}]
[clojure.core/->Eduction {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/->Vec {:var-kind nil, :macro nil}]
[clojure.core/->VecNode {:var-kind nil, :macro nil}]
[clojure.core/->VecSeq {:var-kind nil, :macro nil}]
[clojure.core/-cache-protocol-fn {:var-kind nil, :macro nil}]
[clojure.core/-reset-methods {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/.. {:var-kind nil, :macro true}]
[clojure.core// {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/< {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/<= {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/= {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/== {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/> {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/>= {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/EMPTY-NODE {:var-kind nil, :macro nil}]
[clojure.core/Inst {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/NaN? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/PrintWriter-on {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/StackTraceElement->vec {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/Throwable->map {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/abs {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/accessor {:var-kind nil, :macro nil}]
[clojure.core/aclone {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/add-classpath {:var-kind nil, :macro nil}]
[clojure.core/add-tap {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/add-watch {:var-kind nil, :macro nil}]
[clojure.core/agent {:var-kind nil, :macro nil}]
[clojure.core/agent-error {:var-kind nil, :macro nil}]
[clojure.core/agent-errors {:var-kind nil, :macro nil}]
[clojure.core/aget {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/alength {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/alias {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/all-ns {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/alter {:var-kind nil, :macro nil}]
[clojure.core/alter-meta! {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/alter-var-root {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/amap {:var-kind nil, :macro true}]
[clojure.core/ancestors {:var-kind nil, :macro nil}]
[clojure.core/and {:var-kind nil, :macro true}]
[clojure.core/any? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/apply {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/areduce {:var-kind nil, :macro true}]
[clojure.core/array-map {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/as-> {:var-kind nil, :macro true}]
[clojure.core/aset {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/aset-boolean {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/aset-byte {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/aset-char {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/aset-double {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/aset-float {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/aset-int {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/aset-long {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/aset-short {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/assert {:var-kind nil, :macro true}]
[clojure.core/assert-same-protocol {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/assoc {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/assoc! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/assoc-in {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/associative? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/atom {:var-kind nil, :macro nil}]
[clojure.core/await {:var-kind nil, :macro nil}]
[clojure.core/await-for {:var-kind nil, :macro nil}]
[clojure.core/await1 {:var-kind nil, :macro nil}]
[clojure.core/bases {:var-kind nil, :macro nil}]
[clojure.core/bean {:var-kind nil, :macro nil}]
[clojure.core/bigdec {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bigint {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/biginteger {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/binding {:var-kind nil, :macro true}]
[clojure.core/bit-and {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-and-not {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-clear {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-flip {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-not {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-or {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-set {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-shift-left {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-shift-right {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-test {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bit-xor {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/boolean {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/boolean-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/boolean? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/booleans {:var-kind nil, :macro nil}]
[clojure.core/bound-fn {:var-kind nil, :macro true}]
[clojure.core/bound-fn* {:var-kind nil, :macro nil, :warn-if-ret-val-unused true}]
[clojure.core/bound? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/bounded-count {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/butlast {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/byte {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/byte-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/bytes {:var-kind nil, :macro nil}]
[clojure.core/bytes? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/case {:var-kind nil, :macro true}]
[clojure.core/case-fallthrough-err-impl {:warn-if-ret-val-unused false, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/cast {:var-kind nil, :macro nil}]
[clojure.core/cat {:var-kind nil, :macro nil}]
[clojure.core/char {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/char-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/char-escape-string {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/char-name-string {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/char? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/chars {:var-kind nil, :macro nil}]
[clojure.core/chunk {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/chunk-append {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/chunk-buffer {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/chunk-cons {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/chunk-first {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/chunk-next {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/chunk-rest {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/chunked-seq? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/class {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/class? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/clear-agent-errors {:var-kind nil, :macro nil}]
[clojure.core/clojure-version {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/coll? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/comment {:var-kind nil, :macro true}]
[clojure.core/commute {:var-kind nil, :macro nil}]
[clojure.core/comp {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/comparator {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/compare {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/compare-and-set! {:var-kind nil, :macro nil}]
[clojure.core/compile {:var-kind nil, :macro nil}]
[clojure.core/complement {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/completing {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/concat {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/cond {:var-kind nil, :macro true}]
[clojure.core/cond-> {:var-kind nil, :macro true}]
[clojure.core/cond->> {:var-kind nil, :macro true}]
[clojure.core/condp {:var-kind nil, :macro true}]
[clojure.core/conj {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/conj! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/cons {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/constantly {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/construct-proxy {:var-kind nil, :macro nil}]
[clojure.core/contains? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/count {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/counted? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/create-ns {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/create-struct {:var-kind nil, :macro nil}]
[clojure.core/cycle {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/dec {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/dec' {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/decimal? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/declare {:var-kind nil, :macro true}]
[clojure.core/dedupe {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy true, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/default-data-readers {:var-kind nil, :macro nil}]
[clojure.core/definline {:var-kind nil, :macro true}]
[clojure.core/definterface {:var-kind nil, :macro true}]
[clojure.core/defmacro {:var-kind nil, :macro true}]
[clojure.core/defmethod {:var-kind nil, :macro true}]
[clojure.core/defmulti {:var-kind nil, :macro true}]
[clojure.core/defn {:var-kind nil, :macro true}]
[clojure.core/defn- {:var-kind nil, :macro true}]
[clojure.core/defonce {:var-kind nil, :macro true}]
[clojure.core/defprotocol {:var-kind nil, :macro true}]
[clojure.core/defrecord {:var-kind nil, :macro true}]
[clojure.core/defstruct {:var-kind nil, :macro true}]
[clojure.core/deftype {:var-kind nil, :macro true}]
[clojure.core/delay {:var-kind nil, :macro true}]
[clojure.core/delay? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/deliver {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/denominator {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/deref {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/derive {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/descendants {:var-kind nil, :macro nil}]
[clojure.core/destructure {:var-kind nil, :macro nil}]
[clojure.core/disj {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/disj! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/dissoc {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/dissoc! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/distinct {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/distinct? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/doall {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/dorun {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused false}]
[clojure.core/doseq {:var-kind nil, :macro true}]
[clojure.core/dosync {:var-kind nil, :macro true}]
[clojure.core/dotimes {:var-kind nil, :macro true}]
[clojure.core/doto {:var-kind nil, :macro true}]
[clojure.core/double {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/double-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/double? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/doubles {:var-kind nil, :macro nil}]
[clojure.core/drop {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/drop-last {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/drop-while {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/eduction {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/empty {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/empty? {:var-kind nil, :macro nil, :lazy true, :pure-fn true, :predicate true}]
[clojure.core/ensure {:var-kind nil, :macro nil}]
[clojure.core/ensure-reduced {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/enumeration-seq {:var-kind nil, :macro nil, :pure-fn true}]
[clojure.core/error-handler {:var-kind nil, :macro nil}]
[clojure.core/error-mode {:var-kind nil, :macro nil}]
[clojure.core/eval {:var-kind nil, :macro nil, :evals-exprs true, :side-effect true}]
[clojure.core/even? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/every-pred {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/every? {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true, :predicate true}]
[clojure.core/ex-cause {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/ex-data {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/ex-info {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/ex-message {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/extend {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/extend-protocol {:var-kind nil, :macro true}]
[clojure.core/extend-type {:var-kind nil, :macro true}]
[clojure.core/extenders {:var-kind nil, :macro nil}]
[clojure.core/extends? {:var-kind nil, :macro nil, :predicate true}]
[clojure.core/false? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/ffirst {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/file-seq {:var-kind nil, :macro nil, :lazy true, :warn-if-ret-val-unused true}]
[clojure.core/filter {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/filterv {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/find {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/find-keyword {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/find-ns {:var-kind nil, :macro nil}]
[clojure.core/find-protocol-impl {:var-kind nil, :macro nil}]
[clojure.core/find-protocol-method {:var-kind nil, :macro nil}]
[clojure.core/find-var {:var-kind nil, :macro nil}]
[clojure.core/first {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/flatten {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/float {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/float-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/float? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/floats {:var-kind nil, :macro nil}]
[clojure.core/flush {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.core/fn {:var-kind nil, :macro true}]
[clojure.core/fn? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/fnext {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/fnil {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/for {:var-kind nil, :macro true}]
[clojure.core/force {:var-kind nil, :macro nil}]
[clojure.core/format {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/frequencies {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/future {:var-kind nil, :macro true}]
[clojure.core/future-call {:var-kind nil, :macro nil}]
[clojure.core/future-cancel {:var-kind nil, :macro nil}]
[clojure.core/future-cancelled? {:var-kind nil, :macro nil, :predicate true}]
[clojure.core/future-done? {:var-kind nil, :macro nil, :predicate true}]
[clojure.core/future? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/gen-class {:var-kind nil, :macro true}]
[clojure.core/gen-interface {:var-kind nil, :macro true}]
[clojure.core/gensym {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/get {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/get-in {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/get-method {:var-kind nil, :macro nil}]
[clojure.core/get-proxy-class {:var-kind nil, :macro nil}]
[clojure.core/get-thread-bindings {:var-kind nil, :macro nil}]
[clojure.core/get-validator {:var-kind nil, :macro nil}]
[clojure.core/group-by {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/halt-when {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/hash {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/hash-combine {:var-kind nil, :macro nil}]
[clojure.core/hash-map {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/hash-ordered-coll {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/hash-set {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/hash-unordered-coll {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/ident? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/identical? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/identity {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/if-let {:var-kind nil, :macro true}]
[clojure.core/if-not {:var-kind nil, :macro true}]
[clojure.core/if-some {:var-kind nil, :macro true}]
[clojure.core/ifn? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/import {:var-kind nil, :macro true}]
[clojure.core/in-ns {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/inc {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/inc' {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/indexed? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/infinite? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/init-proxy {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/inst-ms {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/inst-ms* {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/inst? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/instance? {:var-kind nil, :macro nil, :predicate true}]
[clojure.core/int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/int-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/int? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/integer? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/interleave {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/intern {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/interpose {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/into {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/into-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/ints {:var-kind nil, :macro nil}]
[clojure.core/io! {:var-kind nil, :macro true}]
[clojure.core/isa? {:var-kind nil, :macro nil}]
[clojure.core/iterate {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/iteration {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/iterator-seq {:var-kind nil, :macro nil, :warn-if-ret-val-unused true}]
[clojure.core/juxt {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/keep {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/keep-indexed {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/key {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/keys {:var-kind nil, :macro nil, :pure-fn true}]
[clojure.core/keyword {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/keyword? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/last {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/lazy-cat {:var-kind nil, :macro true}]
[clojure.core/lazy-seq {:var-kind nil, :macro true}]
[clojure.core/let {:var-kind nil, :macro true}]
[clojure.core/letfn {:var-kind nil, :macro true}]
[clojure.core/line-seq {:var-kind nil, :macro nil, :lazy true, :warn-if-ret-val-unused true}]
[clojure.core/list {:var-kind nil, :macro nil, :pure-fn true}]
[clojure.core/list* {:var-kind nil, :macro nil, :pure-fn true}]
[clojure.core/list? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/load {:var-kind nil, :macro nil, :io-fn true, :evals-exprs true, :side-effect true}]
[clojure.core/load-file {:var-kind nil, :macro nil, :io-fn true, :evals-exprs true, :side-effect true}]
[clojure.core/load-reader {:var-kind nil, :macro nil, :io-fn true, :evals-exprs true, :side-effect true}]
[clojure.core/load-string {:var-kind nil, :macro nil, :evals-exprs true}]
[clojure.core/loaded-libs {:var-kind nil, :macro nil}]
[clojure.core/locking {:var-kind nil, :macro true}]
[clojure.core/long {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/long-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/longs {:var-kind nil, :macro nil}]
[clojure.core/loop {:var-kind nil, :macro true}]
[clojure.core/macroexpand {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/macroexpand-1 {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/make-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/make-hierarchy {:var-kind nil, :macro nil}]
[clojure.core/map {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/map-entry? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/map-indexed {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/map? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/mapcat {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/mapv {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/max {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/max-key {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/memfn {:var-kind nil, :macro true}]
[clojure.core/memoize {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/merge {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/merge-with {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/meta {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/method-sig {:var-kind nil, :macro nil}]
[clojure.core/methods {:var-kind nil, :macro nil}]
[clojure.core/min {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/min-key {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/mix-collection-hash {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/mod {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/munge {:var-kind nil, :macro nil}]
[clojure.core/name {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/namespace {:var-kind nil, :macro nil}]
[clojure.core/namespace-munge {:var-kind nil, :macro nil}]
[clojure.core/nat-int? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/neg-int? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/neg? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/newline {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.core/next {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/nfirst {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/nil? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/nnext {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/not {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/not-any? {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true, :predicate true}]
[clojure.core/not-empty {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/not-every? {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true, :predicate true}]
[clojure.core/not= {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/ns {:var-kind nil, :macro true}]
[clojure.core/ns-aliases {:var-kind nil, :macro nil}]
[clojure.core/ns-imports {:var-kind nil, :macro nil}]
[clojure.core/ns-interns {:var-kind nil, :macro nil}]
[clojure.core/ns-map {:var-kind nil, :macro nil}]
[clojure.core/ns-name {:var-kind nil, :macro nil}]
[clojure.core/ns-publics {:var-kind nil, :macro nil}]
[clojure.core/ns-refers {:var-kind nil, :macro nil}]
[clojure.core/ns-resolve {:var-kind nil, :macro nil}]
[clojure.core/ns-unalias {:var-kind nil, :macro nil}]
[clojure.core/ns-unmap {:var-kind nil, :macro nil}]
[clojure.core/nth {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/nthnext {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/nthrest {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/num {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/number? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/numerator {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/object-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/odd? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/or {:var-kind nil, :macro true}]
[clojure.core/parents {:var-kind nil, :macro nil}]
[clojure.core/parse-boolean {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/parse-double {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/parse-long {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/parse-uuid {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/partial {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/partition {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/partition-all {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/partition-by {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/pcalls {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/peek {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/persistent! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/pmap {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/pop {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/pop! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/pop-thread-bindings {:var-kind nil, :macro nil}]
[clojure.core/pos-int? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/pos? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/pr {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.core/pr-str {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/prefer-method {:var-kind nil, :macro nil}]
[clojure.core/prefers {:var-kind nil, :macro nil}]
[clojure.core/primitives-classnames {:var-kind nil, :macro nil}]
[clojure.core/print {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.core/print-ctor {:var-kind nil, :macro nil}]
[clojure.core/print-dup {:var-kind nil, :macro nil}]
[clojure.core/print-method {:var-kind nil, :macro nil}]
[clojure.core/print-simple {:var-kind nil, :macro nil}]
[clojure.core/print-str {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/printf {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.core/println {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.core/println-str {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/prn {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.core/prn-str {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/promise {:var-kind nil, :macro nil}]
[clojure.core/proxy {:var-kind nil, :macro true}]
[clojure.core/proxy-call-with-super {:var-kind nil, :macro nil}]
[clojure.core/proxy-mappings {:var-kind nil, :macro nil}]
[clojure.core/proxy-name {:var-kind nil, :macro nil}]
[clojure.core/proxy-super {:var-kind nil, :macro true}]
[clojure.core/push-thread-bindings {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/pvalues {:var-kind nil, :macro true}]
[clojure.core/qualified-ident? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/qualified-keyword? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/qualified-symbol? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/quot {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/rand {:var-kind nil, :macro nil, :side-effect :update-prng, :pure-fn false, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/rand-int {:var-kind nil, :macro nil, :side-effect :update-prng, :pure-fn false, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/rand-nth {:var-kind nil, :macro nil, :side-effect :update-prng, :pure-fn false, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/random-sample {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy true, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect :update-prng, :predicate false}]
[clojure.core/random-uuid {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect :update-prng, :predicate false}]
[clojure.core/range {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/ratio? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/rational? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/rationalize {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/re-find {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/re-groups {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/re-matcher {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/re-matches {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/re-pattern {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/re-seq {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/read {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs true, :side-effect true, :predicate false}]
[clojure.core/read+string {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs true, :side-effect true, :predicate false}]
[clojure.core/read-line {:var-kind nil, :macro nil, :io-fn true, :evals-exprs false, :lazy false, :warn-if-ret-val-unused false}]
[clojure.core/read-string {:var-kind nil, :macro nil, :evals-exprs true, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/reader-conditional {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/reader-conditional? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/realized? {:var-kind nil, :macro nil, :lazy false, :pure-fn false, :predicate true}]
[clojure.core/record? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/reduce {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/reduce-kv {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/reduced {:var-kind nil, :macro nil}]
[clojure.core/reduced? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/reductions {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/ref {:var-kind nil, :macro nil}]
[clojure.core/ref-history-count {:var-kind nil, :macro nil}]
[clojure.core/ref-max-history {:var-kind nil, :macro nil}]
[clojure.core/ref-min-history {:var-kind nil, :macro nil}]
[clojure.core/ref-set {:var-kind nil, :macro nil}]
[clojure.core/refer {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/refer-clojure {:var-kind nil, :macro true}]
[clojure.core/reify {:var-kind nil, :macro true}]
[clojure.core/release-pending-sends {:var-kind nil, :macro nil}]
[clojure.core/rem {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/remove {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/remove-all-methods {:var-kind nil, :macro nil}]
[clojure.core/remove-method {:var-kind nil, :macro nil}]
[clojure.core/remove-ns {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/remove-tap {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/remove-watch {:var-kind nil, :macro nil}]
[clojure.core/repeat {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/repeatedly {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/replace {:var-kind nil, :macro nil, :pure-fn true}]
[clojure.core/replicate {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/require {:var-kind nil, :macro nil, :io-fn true, :evals-exprs true, :side-effect true}]
[clojure.core/requiring-resolve {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs true, :side-effect true, :predicate false}]
[clojure.core/reset! {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/reset-meta! {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/reset-vals! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/resolve {:var-kind nil, :macro nil}]
[clojure.core/rest {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/restart-agent {:var-kind nil, :macro nil}]
[clojure.core/resultset-seq {:var-kind nil, :macro nil, :lazy true, :warn-if-ret-val-unused true}]
[clojure.core/reverse {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/reversible? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/rseq {:var-kind nil, :macro nil, :pure-fn true}]
[clojure.core/rsubseq {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/run! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/satisfies? {:var-kind nil, :macro nil, :predicate true}]
[clojure.core/second {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/select-keys {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/send {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/send-off {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/send-via {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/seq {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/seq-to-map-for-destructuring {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/seq? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/seqable? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/seque {:var-kind nil, :macro nil}]
[clojure.core/sequence {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/sequential? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/set {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/set-agent-send-executor! {:var-kind nil, :macro nil}]
[clojure.core/set-agent-send-off-executor! {:var-kind nil, :macro nil}]
[clojure.core/set-error-handler! {:var-kind nil, :macro nil}]
[clojure.core/set-error-mode! {:var-kind nil, :macro nil}]
[clojure.core/set-validator! {:var-kind nil, :macro nil}]
[clojure.core/set? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/short {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/short-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/shorts {:var-kind nil, :macro nil}]
[clojure.core/shuffle {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.core/shutdown-agents {:var-kind nil, :macro nil}]
[clojure.core/simple-ident? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/simple-keyword? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/simple-symbol? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/slurp {:var-kind nil, :macro nil, :io-fn true, :lazy false, :warn-if-ret-val-unused false}]
[clojure.core/some {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/some-> {:var-kind nil, :macro true}]
[clojure.core/some->> {:var-kind nil, :macro true}]
[clojure.core/some-fn {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.core/some? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/sort {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/sort-by {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/sorted-map {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/sorted-map-by {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/sorted-set {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/sorted-set-by {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/sorted? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/special-symbol? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/spit {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.core/split-at {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/split-with {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/str {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/string? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/struct {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/struct-map {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/subs {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/subseq {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/subvec {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/supers {:var-kind nil, :macro nil}]
[clojure.core/swap! {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/swap-vals! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/symbol {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/symbol? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/sync {:var-kind nil, :macro true}]
[clojure.core/tagged-literal {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/tagged-literal? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/take {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/take-last {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/take-nth {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/take-while {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/tap> {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core/test {:var-kind nil, :macro nil}]
[clojure.core/the-ns {:var-kind nil, :macro nil}]
[clojure.core/thread-bound? {:var-kind nil, :macro nil, :predicate true}]
[clojure.core/time {:var-kind nil, :macro true}]
[clojure.core/to-array {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/to-array-2d {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/trampoline {:var-kind nil, :macro nil}]
[clojure.core/transduce {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure true, :lazy false, :macro nil, :var-kind nil, :evals-exprs false, io-fn false, :side-effect true, :predicate false}]
[clojure.core/transient {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/tree-seq {:var-kind nil, :macro nil, :lazy true, :pure-fn-if-fn-args-pure true}]
[clojure.core/true? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/type {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-add {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-add-int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-byte {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-char {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-dec {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-dec-int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-divide-int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-double {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-float {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-inc {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-inc-int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-long {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-multiply {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-multiply-int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-negate {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-negate-int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-remainder-int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-short {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-subtract {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/unchecked-subtract-int {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/underive {:var-kind nil, :macro nil}]
[clojure.core/unquote {:var-kind nil, :macro nil}]
[clojure.core/unquote-splicing {:var-kind nil, :macro nil}]
[clojure.core/unreduced {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/unsigned-bit-shift-right {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/update {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/update-in {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/update-keys {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/update-proxy {:var-kind nil, :macro nil}]
[clojure.core/update-vals {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/uri? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/use {:var-kind nil, :macro nil, :io-fn true, :evals-exprs true, :side-effect true}]
[clojure.core/uuid? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core/val {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/vals {:var-kind nil, :macro nil, :pure-fn true}]
[clojure.core/var-get {:var-kind nil, :macro nil}]
[clojure.core/var-set {:var-kind nil, :macro nil}]
[clojure.core/var? {:var-kind nil, :macro nil, :predicate true}]
[clojure.core/vary-meta {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.core/vec {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/vector {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/vector-of {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/vector? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/volatile! {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core/volatile? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/vreset! {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/vswap! {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core/when {:var-kind nil, :macro true}]
[clojure.core/when-first {:var-kind nil, :macro true}]
[clojure.core/when-let {:var-kind nil, :macro true}]
[clojure.core/when-not {:var-kind nil, :macro true}]
[clojure.core/when-some {:var-kind nil, :macro true}]
[clojure.core/while {:var-kind nil, :macro true}]
[clojure.core/with-bindings {:var-kind nil, :macro true}]
[clojure.core/with-bindings* {:var-kind nil, :macro nil}]
[clojure.core/with-in-str {:var-kind nil, :macro true}]
[clojure.core/with-loading-context {:var-kind nil, :macro true}]
[clojure.core/with-local-vars {:var-kind nil, :macro true}]
[clojure.core/with-meta {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core/with-open {:var-kind nil, :macro true}]
[clojure.core/with-out-str {:var-kind nil, :macro true}]
[clojure.core/with-precision {:var-kind nil, :macro true}]
[clojure.core/with-redefs {:var-kind nil, :macro true}]
[clojure.core/with-redefs-fn {:var-kind nil, :macro nil}]
[clojure.core/xml-seq {:var-kind nil, :macro nil, :lazy true, :pure-fn true}]
[clojure.core/zero? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.core/zipmap {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.core.async.impl.dispatch/check-blocking-in-dispatch {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.dispatch/executor {:var-kind nil, :macro nil}]
[clojure.core.async.impl.dispatch/in-dispatch-thread? {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core.async.impl.dispatch/run {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core.async.impl.ioc-macros/->Call {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->Case {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->CatchHandler {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->CondBr {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->Const {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->CustomTerminator {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->Dot {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->EndFinally {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->Fn {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->InstanceInterop {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->Jmp {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->PopTry {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->PushTry {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->RawCode {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->Recur {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->Return {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/->StaticCall {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/-item-to-ssa {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/BINDINGS-IDX {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/EXCEPTION-FRAMES {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/FN-IDX {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/IEmittableInstruction {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/IInstruction {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/ITerminator {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/STATE-IDX {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/USER-START-IDX {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/VALUE-IDX {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/add-block {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/add-instruction {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/aget-object {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/all {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/aset-all! {:warn-if-ret-val-unused false, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/aset-object {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core.async.impl.ioc-macros/assoc-in-plan {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/async-custom-terminators {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/block-references {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/count-persistent-values {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/debug {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.async.impl.ioc-macros/emit-hinted {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/emit-instruction {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/finished? {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core.async.impl.ioc-macros/gen-plan {:warn-if-ret-val-unused false, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/get-binding {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/get-block {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/get-in-plan {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/get-plan {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/id-for-inst {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.async.impl.ioc-macros/index-block {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/index-instruction {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/index-state-machine {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/instruction? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core.async.impl.ioc-macros/item-to-ssa {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/let-binding-to-ssa {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/make-env {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->Call {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->Case {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->CatchHandler {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->CondBr {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->Const {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->CustomTerminator {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->Dot {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->EndFinally {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->Fn {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->InstanceInterop {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->Jmp {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->PopTry {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->PushTry {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->RawCode {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->Recur {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->Return {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/map->StaticCall {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/mark-transitions {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/nested-go? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core.async.impl.ioc-macros/no-op {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/parse-to-state-machine {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/passes {:var-kind nil, :macro nil}]
[clojure.core.async.impl.ioc-macros/pdebug {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.async.impl.ioc-macros/persistent-value? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core.async.impl.ioc-macros/pop-binding {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/print-plan {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/process-exception {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core.async.impl.ioc-macros/propagate-recur {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/propagate-transitions {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/push-alter-binding {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/push-binding {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/put! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.async.impl.ioc-macros/reads-from {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/return-chan {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/run-passes {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/run-state-machine {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.async.impl.ioc-macros/run-state-machine-wrapped {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.async.impl.ioc-macros/set-block {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/state-machine {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs true, :side-effect true, :predicate false}]
[clojure.core.async.impl.ioc-macros/take! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.async.impl.ioc-macros/terminate-block {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/terminator-code {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/update-in-plan {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/var-name {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.async.impl.ioc-macros/writes-to {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/->BasicCache {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/->FIFOCache {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/->FnCache {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/->LIRSCache {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/->LRUCache {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/->LUCache {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/->SoftCache {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/->TTLCacheQ {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/CacheProtocol {:var-kind nil, :macro nil}]
[clojure.core.cache/basic-cache-factory {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/clear-soft-cache! {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core.cache/defcache {:warn-if-ret-val-unused false, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs true, :side-effect false, :predicate false}]
[clojure.core.cache/evict {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.cache/fifo-cache-factory {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/has? {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core.cache/hit {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.cache/lirs-cache-factory {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/lookup {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.cache/lru-cache-factory {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/lu-cache-factory {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/make-reference {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/miss {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.cache/seed {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.cache/soft-cache-factory {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache/through {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.cache/through-cache {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.cache/ttl-cache-factory {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.cache.tests/do-assoc {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core.cache.tests/do-dissoc {:var-kind nil, :macro nil, :side-effect true}]
[clojure.core.memoize/->PluggableMemoization {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/->RetryingDelay {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/build-memoizer {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/fifo {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/lazy-snapshot {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy true, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/lru {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/lu {:var-kind nil, :macro nil, :pure-fn false}]
[clojure.core.memoize/memo {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/memo-clear! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.memoize/memo-fifo {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/memo-lru {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/memo-lu {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/memo-reset! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.memoize/memo-swap! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.core.memoize/memo-ttl {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/memo-unwrap {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/memoized? {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.core.memoize/memoizer {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/snapshot {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.memoize/ttl {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.protocols/CollReduce {:var-kind nil, :macro nil}]
[clojure.core.protocols/Datafiable {:var-kind nil, :macro nil}]
[clojure.core.protocols/IKVReduce {:var-kind nil, :macro nil}]
[clojure.core.protocols/InternalReduce {:var-kind nil, :macro nil}]
[clojure.core.protocols/Navigable {:var-kind nil, :macro nil}]
[clojure.core.protocols/arr-impl {:var-kind nil, :macro nil}]
[clojure.core.protocols/coll-reduce {:var-kind nil, :macro nil}]
[clojure.core.protocols/datafy {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.protocols/emit-array-impls {:var-kind nil, :macro true}]
[clojure.core.protocols/internal-reduce {:var-kind nil, :macro nil}]
[clojure.core.protocols/kv-reduce {:var-kind nil, :macro nil}]
[clojure.core.protocols/nav {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.core.reducers/->Cat {:var-kind nil, :macro nil}]
[clojure.core.reducers/CollFold {:var-kind nil, :macro nil}]
[clojure.core.reducers/append! {:var-kind nil, :macro nil}]
[clojure.core.reducers/cat {:var-kind nil, :macro nil}]
[clojure.core.reducers/coll-fold {:var-kind nil, :macro nil}]
[clojure.core.reducers/drop {:var-kind nil, :macro nil}]
[clojure.core.reducers/filter {:var-kind nil, :macro nil}]
[clojure.core.reducers/fjtask {:var-kind nil, :macro nil}]
[clojure.core.reducers/flatten {:var-kind nil, :macro nil}]
[clojure.core.reducers/fold {:var-kind nil, :macro nil}]
[clojure.core.reducers/foldcat {:var-kind nil, :macro nil}]
[clojure.core.reducers/folder {:var-kind nil, :macro nil}]
[clojure.core.reducers/map {:var-kind nil, :macro nil}]
[clojure.core.reducers/mapcat {:var-kind nil, :macro nil}]
[clojure.core.reducers/monoid {:var-kind nil, :macro nil}]
[clojure.core.reducers/pool {:var-kind nil, :macro nil}]
[clojure.core.reducers/reduce {:var-kind nil, :macro nil}]
[clojure.core.reducers/reducer {:var-kind nil, :macro nil}]
[clojure.core.reducers/remove {:var-kind nil, :macro nil}]
[clojure.core.reducers/take {:var-kind nil, :macro nil}]
[clojure.core.reducers/take-while {:var-kind nil, :macro nil}]
[clojure.core.typed/load-if-needed {:var-kind nil, :macro nil, :side-effect true}]
[clojure.data/Diff {:var-kind nil, :macro nil}]
[clojure.data/EqualityPartition {:var-kind nil, :macro nil}]
[clojure.data/diff {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.data/diff-similar {:var-kind nil, :macro nil}]
[clojure.data/equality-partition {:var-kind nil, :macro nil}]
[clojure.data.codec.base64/dec-length {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.data.codec.base64/decode {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.data.codec.base64/decode! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.data.codec.base64/decoding-transfer {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.data.codec.base64/enc-length {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.data.codec.base64/encode {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.data.codec.base64/encode! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.data.codec.base64/encoding-transfer {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.data.codec.base64/pad-length {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.data.csv/Read-CSV-From {:var-kind nil, :macro nil}]
[clojure.data.csv/read-csv {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy true, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.data.csv/read-csv-from {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy true, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.data.csv/write-cell {:var-kind nil, :macro nil, :lazy false, :io-fn true, :side-effect true}]
[clojure.data.csv/write-csv {:var-kind nil, :macro nil, :lazy false, :io-fn true, :side-effect true}]
[clojure.data.csv/write-record {:var-kind nil, :macro nil, :lazy false, :io-fn true, :side-effect true}]
[clojure.data.json/-write {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.data.json/JSONWriter {:var-kind nil, :macro nil}]
[clojure.data.json/codepoint-decoder {:var-kind nil, :macro nil}]
[clojure.data.json/default-read-options {:var-kind nil, :macro nil}]
[clojure.data.json/default-write-options {:var-kind nil, :macro nil}]
[clojure.data.json/invalid-array-exception {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.data.json/json-str {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.data.json/pprint {:var-kind nil, :macro nil, :lazy false, :io-fn true, :side-effect true}]
[clojure.data.json/pprint-json {:var-kind nil, :macro nil, :lazy false, :io-fn true, :side-effect true}]
[clojure.data.json/print-json {:var-kind nil, :macro nil, :lazy false, :io-fn true, :side-effect true}]
[clojure.data.json/read {:var-kind nil, :macro nil, :io-fn true, :evals-exprs false, :lazy false, :warn-if-ret-val-unused true}]
[clojure.data.json/read-json {:var-kind nil, :macro nil, :lazy false, :io-fn true, :warn-if-ret-val-unused true}]
[clojure.data.json/read-str {:var-kind nil, :macro nil, :evals-exprs false, :lazy false, :warn-if-ret-val-unused true}]
[clojure.data.json/write {:var-kind nil, :macro nil, :lazy false, :io-fn true, :side-effect true}]
[clojure.data.json/write-json {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.data.json/write-str {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.data.json/write-string {:var-kind nil, :macro nil, :lazy false, :io-fn true, :side-effect true}]
[clojure.data.priority-map/->PersistentPriorityMap {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.data.priority-map/apply-keyfn {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :side-effect false, :predicate false}]
[clojure.data.priority-map/priority->set-of-items {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.data.priority-map/priority-map {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.data.priority-map/priority-map-by {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.data.priority-map/priority-map-keyfn {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.data.priority-map/priority-map-keyfn-by {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.data.priority-map/rsubseq {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy true, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.data.priority-map/subseq {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy true, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.edn/read {:var-kind nil, :macro nil, :io-fn true, :lazy false, :warn-if-ret-val-unused false}]
[clojure.edn/read-string {:var-kind nil, :macro nil, :pure-if-fn-args-pure true}]
[clojure.inspector/atom? {:var-kind nil, :macro nil}]
[clojure.inspector/collection-tag {:var-kind nil, :macro nil}]
[clojure.inspector/get-child {:var-kind nil, :macro nil}]
[clojure.inspector/get-child-count {:var-kind nil, :macro nil}]
[clojure.inspector/inspect {:var-kind nil, :macro nil}]
[clojure.inspector/inspect-table {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.inspector/inspect-tree {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.inspector/is-leaf {:var-kind nil, :macro nil}]
[clojure.inspector/list-model {:var-kind nil, :macro nil}]
[clojure.inspector/list-provider {:var-kind nil, :macro nil}]
[clojure.inspector/old-table-model {:var-kind nil, :macro nil}]
[clojure.inspector/table-model {:var-kind nil, :macro nil}]
[clojure.inspector/tree-model {:var-kind nil, :macro nil}]
[clojure.instant/parse-timestamp {:var-kind nil, :macro nil}]
[clojure.instant/read-instant-calendar {:var-kind nil, :macro nil}]
[clojure.instant/read-instant-date {:var-kind nil, :macro nil}]
[clojure.instant/read-instant-timestamp {:var-kind nil, :macro nil}]
[clojure.instant/validated {:var-kind nil, :macro nil}]
[clojure.java.browse/*open-url-script* {:var-kind nil, :macro nil}]
[clojure.java.browse/browse-url {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.java.io/Coercions {:var-kind nil, :macro nil}]
[clojure.java.io/IOFactory {:var-kind nil, :macro nil}]
[clojure.java.io/as-file {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.java.io/as-relative-path {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.java.io/as-url {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.java.io/copy {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.java.io/default-streams-impl {:var-kind nil, :macro nil}]
[clojure.java.io/delete-file {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.java.io/file {:var-kind nil, :macro nil, :lazy false, :warn-if-ret-val-unused true}]
[clojure.java.io/input-stream {:var-kind nil, :macro nil, :io-fn true, :lazy false, :warn-if-ret-val-unused true}]
[clojure.java.io/make-input-stream {:var-kind nil, :macro nil}]
[clojure.java.io/make-output-stream {:var-kind nil, :macro nil}]
[clojure.java.io/make-parents {:var-kind nil, :macro nil}]
[clojure.java.io/make-reader {:var-kind nil, :macro nil}]
[clojure.java.io/make-writer {:var-kind nil, :macro nil}]
[clojure.java.io/output-stream {:var-kind nil, :macro nil, :io-fn true, :lazy false, :warn-if-ret-val-unused true}]
[clojure.java.io/reader {:var-kind nil, :macro nil, :io-fn true, :lazy false, :warn-if-ret-val-unused true}]
[clojure.java.io/resource {:var-kind nil, :macro nil, :io-fn true, :lazy false, :warn-if-ret-val-unused true}]
[clojure.java.io/writer {:var-kind nil, :macro nil, :io-fn true, :lazy false, :warn-if-ret-val-unused true}]
[clojure.java.javadoc/*core-java-api* {:var-kind nil, :macro nil}]
[clojure.java.javadoc/*feeling-lucky* {:var-kind nil, :macro nil}]
[clojure.java.javadoc/*feeling-lucky-url* {:var-kind nil, :macro nil}]
[clojure.java.javadoc/*local-javadocs* {:var-kind nil, :macro nil}]
[clojure.java.javadoc/*remote-javadocs* {:var-kind nil, :macro nil}]
[clojure.java.javadoc/add-local-javadoc {:var-kind nil, :macro nil}]
[clojure.java.javadoc/add-remote-javadoc {:var-kind nil, :macro nil}]
[clojure.java.javadoc/javadoc {:var-kind nil, :macro nil}]
[clojure.java.jdbc/Connectable {:var-kind nil, :macro nil}]
[clojure.java.jdbc/IResultSetReadColumn {:var-kind nil, :macro nil}]
[clojure.java.jdbc/ISQLParameter {:var-kind nil, :macro nil}]
[clojure.java.jdbc/ISQLValue {:var-kind nil, :macro nil}]
[clojure.java.jdbc/add-connection {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/as-sql-name {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/create-table-ddl {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/db-connection {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/db-do-commands {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/db-do-prepared {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/db-do-prepared-return-keys {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/db-find-connection {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/db-is-rollback-only {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.java.jdbc/db-query-with-resultset {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/db-set-rollback-only! {:var-kind nil, :macro nil, :lazy false, :side-effect true}]
[clojure.java.jdbc/db-transaction* {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/db-unset-rollback-only! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.java.jdbc/delete! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/drop-table-ddl {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/execute! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/find-by-keys {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/get-by-id {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/get-connection {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.java.jdbc/get-isolation-level {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/get-level {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/insert! {:var-kind nil, :macro nil, :lazy false, :side-effect true}]
[clojure.java.jdbc/insert-multi! {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/metadata-query {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure false, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/metadata-result {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/prepare-statement {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/print-sql-exception {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.java.jdbc/print-sql-exception-chain {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.java.jdbc/print-update-counts {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.java.jdbc/query {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/quoted {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/reducible-query {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/reducible-result-set {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/result-set-read-column {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/result-set-seq {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy true, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/set-parameter {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.java.jdbc/sql-value {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/string-array {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/update! {:var-kind nil, :macro nil, :lazy false, :side-effect true}]
[clojure.java.jdbc/when-available {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/with-db-connection {:warn-if-ret-val-unused false, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/with-db-metadata {:warn-if-ret-val-unused false, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.jdbc/with-db-transaction {:warn-if-ret-val-unused false, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.java.shell/*sh-dir* {:var-kind nil, :macro nil}]
[clojure.java.shell/*sh-env* {:var-kind nil, :macro nil}]
[clojure.java.shell/launch {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.java.shell/sh {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.java.shell/with-sh-dir {:var-kind nil, :macro true}]
[clojure.java.shell/with-sh-env {:var-kind nil, :macro true}]
[clojure.java.test-jdbc/create-test-table {:var-kind nil, :macro nil, :lazy false, :side-effect true}]
[clojure.java.test-jdbc/update-or-insert-values {:var-kind nil, :macro nil, :lazy false, :side-effect true}]
[clojure.main/demunge {:var-kind nil, :macro nil}]
[clojure.main/err->msg {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.main/ex-str {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.main/ex-triage {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.main/initialize {:var-kind nil, :macro nil, :side-effect true}]
[clojure.main/load-script {:var-kind nil, :macro nil}]
[clojure.main/main {:var-kind nil, :macro nil}]
[clojure.main/renumbering-read {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.main/repl {:var-kind nil, :macro nil, :side-effect true}]
[clojure.main/repl-caught {:var-kind nil, :macro nil}]
[clojure.main/repl-exception {:var-kind nil, :macro nil}]
[clojure.main/repl-prompt {:var-kind nil, :macro nil}]
[clojure.main/repl-read {:var-kind nil, :macro nil}]
[clojure.main/repl-requires {:var-kind nil, :macro nil}]
[clojure.main/report-error {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.main/root-cause {:var-kind nil, :macro nil}]
[clojure.main/skip-if-eol {:var-kind nil, :macro nil, :side-effect true}]
[clojure.main/skip-whitespace {:var-kind nil, :macro nil}]
[clojure.main/stack-element-str {:var-kind nil, :macro nil}]
[clojure.main/with-bindings {:var-kind nil, :macro true}]
[clojure.main/with-read-known {:var-kind nil, :macro true}]
[clojure.math/E {:var-kind nil, :macro nil}]
[clojure.math/IEEE-remainder {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/PI {:var-kind nil, :macro nil}]
[clojure.math/abs {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/acos {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/add-exact {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/asin {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/atan {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/atan2 {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/cbrt {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/ceil {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/copy-sign {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/cos {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/cosh {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/decrement-exact {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/exp {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/expm1 {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/floor {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/floor-div {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/floor-mod {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/get-exponent {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/hypot {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/increment-exact {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/log {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/log10 {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/log1p {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/max {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/min {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/multiply-exact {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/negate-exact {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/negative-exact {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/next-after {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/next-down {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/next-up {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/pow {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/random {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect :update-prng, :predicate false}]
[clojure.math/rint {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/round {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/scalb {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/signum {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/sin {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/sinh {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/sqrt {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/subtract-exact {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/tan {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/tanh {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/to-degrees {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/to-radians {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math/ulp {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math.numeric-tower/MathFunctions {:var-kind nil, :macro nil}]
[clojure.math.numeric-tower/abs {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.math.numeric-tower/ceil {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math.numeric-tower/exact-integer-sqrt {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.math.numeric-tower/expt {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.math.numeric-tower/floor {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math.numeric-tower/gcd {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.math.numeric-tower/integer-length {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math.numeric-tower/lcm {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.math.numeric-tower/round {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math.numeric-tower/sqrt {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.math.numeric-tower/when-available {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.pprint/*print-base* {:var-kind nil, :macro nil}]
[clojure.pprint/*print-miser-width* {:var-kind nil, :macro nil}]
[clojure.pprint/*print-pprint-dispatch* {:var-kind nil, :macro nil}]
[clojure.pprint/*print-pretty* {:var-kind nil, :macro nil}]
[clojure.pprint/*print-radix* {:var-kind nil, :macro nil}]
[clojure.pprint/*print-right-margin* {:var-kind nil, :macro nil}]
[clojure.pprint/*print-suppress-namespaces* {:var-kind nil, :macro nil}]
[clojure.pprint/cl-format {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.pprint/code-dispatch {:var-kind nil, :macro nil}]
[clojure.pprint/formatter {:var-kind nil, :macro true}]
[clojure.pprint/formatter-out {:var-kind nil, :macro true}]
[clojure.pprint/fresh-line {:var-kind nil, :macro nil}]
[clojure.pprint/get-pretty-writer {:var-kind nil, :macro nil}]
[clojure.pprint/pp {:var-kind nil, :macro true}]
[clojure.pprint/pprint {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.pprint/pprint-indent {:var-kind nil, :macro nil}]
[clojure.pprint/pprint-logical-block {:var-kind nil, :macro true}]
[clojure.pprint/pprint-newline {:var-kind nil, :macro nil}]
[clojure.pprint/pprint-tab {:var-kind nil, :macro nil}]
[clojure.pprint/print-length-loop {:var-kind nil, :macro true}]
[clojure.pprint/print-table {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.pprint/set-pprint-dispatch {:var-kind nil, :macro nil}]
[clojure.pprint/simple-dispatch {:var-kind nil, :macro nil}]
[clojure.pprint/with-pprint-dispatch {:var-kind nil, :macro true}]
[clojure.pprint/write {:var-kind nil, :macro nil}]
[clojure.pprint/write-out {:var-kind nil, :macro nil}]
[clojure.reflect/->AsmReflector {:var-kind nil, :macro nil}]
[clojure.reflect/->Constructor {:var-kind nil, :macro nil}]
[clojure.reflect/->Field {:var-kind nil, :macro nil}]
[clojure.reflect/->JavaReflector {:var-kind nil, :macro nil}]
[clojure.reflect/->Method {:var-kind nil, :macro nil}]
[clojure.reflect/ClassResolver {:var-kind nil, :macro nil}]
[clojure.reflect/Reflector {:var-kind nil, :macro nil}]
[clojure.reflect/TypeReference {:var-kind nil, :macro nil}]
[clojure.reflect/do-reflect {:var-kind nil, :macro nil}]
[clojure.reflect/flag-descriptors {:var-kind nil, :macro nil}]
[clojure.reflect/map->Constructor {:var-kind nil, :macro nil}]
[clojure.reflect/map->Field {:var-kind nil, :macro nil}]
[clojure.reflect/map->Method {:var-kind nil, :macro nil}]
[clojure.reflect/reflect {:var-kind nil, :macro nil}]
[clojure.reflect/resolve-class {:var-kind nil, :macro nil}]
[clojure.reflect/type-reflect {:var-kind nil, :macro nil}]
[clojure.reflect/typename {:var-kind nil, :macro nil}]
[clojure.repl/apropos {:var-kind nil, :macro nil}]
[clojure.repl/demunge {:var-kind nil, :macro nil}]
[clojure.repl/dir {:var-kind nil, :macro true}]
[clojure.repl/dir-fn {:var-kind nil, :macro nil}]
[clojure.repl/doc {:var-kind nil, :macro true}]
[clojure.repl/find-doc {:var-kind nil, :macro nil}]
[clojure.repl/print-doc {:var-kind nil, :macro nil, :side-effect true}]
[clojure.repl/pst {:var-kind nil, :macro nil}]
[clojure.repl/root-cause {:var-kind nil, :macro nil}]
[clojure.repl/set-break-handler! {:var-kind nil, :macro nil}]
[clojure.repl/source {:var-kind nil, :macro true}]
[clojure.repl/source-fn {:var-kind nil, :macro nil}]
[clojure.repl/stack-element-str {:var-kind nil, :macro nil}]
[clojure.repl/thread-stopper {:var-kind nil, :macro nil}]
[clojure.set/difference {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.set/index {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.set/intersection {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.set/join {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.set/map-invert {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.set/project {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.set/rename {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.set/rename-keys {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.set/select {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.set/subset? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.set/superset? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.set/union {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.stacktrace/e {:var-kind nil, :macro nil}]
[clojure.stacktrace/print-cause-trace {:var-kind nil, :macro nil, :side-effect true}]
[clojure.stacktrace/print-stack-trace {:var-kind nil, :macro nil, :side-effect true}]
[clojure.stacktrace/print-throwable {:var-kind nil, :macro nil, :side-effect true}]
[clojure.stacktrace/print-trace-element {:var-kind nil, :macro nil, :side-effect true}]
[clojure.stacktrace/root-cause {:var-kind nil, :macro nil}]
[clojure.string/blank? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.string/capitalize {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/ends-with? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.string/escape {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/includes? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.string/index-of {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/join {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/last-index-of {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/lower-case {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/re-quote-replacement {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/replace {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.string/replace-first {:var-kind nil, :macro nil, :lazy false, :pure-fn-if-fn-args-pure true}]
[clojure.string/reverse {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/split {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/split-lines {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/starts-with? {:var-kind nil, :macro nil, :lazy false, :pure-fn true, :predicate true}]
[clojure.string/trim {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/trim-newline {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/triml {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/trimr {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.string/upper-case {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.template/apply-template {:var-kind nil, :macro nil}]
[clojure.template/do-template {:var-kind nil, :macro true}]
[clojure.test/*initial-report-counters* {:var-kind nil, :macro nil}]
[clojure.test/*load-tests* {:var-kind nil, :macro nil}]
[clojure.test/*report-counters* {:var-kind nil, :macro nil}]
[clojure.test/*stack-trace-depth* {:var-kind nil, :macro nil}]
[clojure.test/*test-out* {:var-kind nil, :macro nil}]
[clojure.test/*testing-contexts* {:var-kind nil, :macro nil}]
[clojure.test/*testing-vars* {:var-kind nil, :macro nil}]
[clojure.test/are {:var-kind nil, :macro true}]
[clojure.test/assert-any {:var-kind nil, :macro nil}]
[clojure.test/assert-expr {:var-kind nil, :macro nil}]
[clojure.test/assert-predicate {:var-kind nil, :macro nil}]
[clojure.test/compose-fixtures {:var-kind nil, :macro nil}]
[clojure.test/deftest {:var-kind nil, :macro true}]
[clojure.test/deftest- {:var-kind nil, :macro true}]
[clojure.test/do-report {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test/file-position {:var-kind nil, :macro nil}]
[clojure.test/function? {:var-kind nil, :macro nil}]
[clojure.test/get-possibly-unbound-var {:var-kind nil, :macro nil}]
[clojure.test/inc-report-counter {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test/is {:var-kind nil, :macro true}]
[clojure.test/join-fixtures {:var-kind nil, :macro nil}]
[clojure.test/report {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test/run-all-tests {:var-kind nil, :macro nil}]
[clojure.test/run-test {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.test/run-test-var {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.test/run-tests {:var-kind nil, :macro nil}]
[clojure.test/set-test {:var-kind nil, :macro true}]
[clojure.test/successful? {:var-kind nil, :macro nil}]
[clojure.test/test-all-vars {:var-kind nil, :macro nil}]
[clojure.test/test-ns {:var-kind nil, :macro nil}]
[clojure.test/test-var {:var-kind nil, :macro nil}]
[clojure.test/test-vars {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.test/testing {:var-kind nil, :macro true}]
[clojure.test/testing-contexts-str {:var-kind nil, :macro nil}]
[clojure.test/testing-vars-str {:var-kind nil, :macro nil}]
[clojure.test/try-expr {:var-kind nil, :macro true}]
[clojure.test/use-fixtures {:var-kind nil, :macro nil}]
[clojure.test/with-test {:var-kind nil, :macro true}]
[clojure.test/with-test-out {:var-kind nil, :macro true}]
[clojure.test.junit/*depth* {:var-kind nil, :macro nil}]
[clojure.test.junit/*var-context* {:var-kind nil, :macro nil}]
[clojure.test.junit/element-content {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test.junit/error-el {:var-kind nil, :macro nil}]
[clojure.test.junit/failure-el {:var-kind nil, :macro nil}]
[clojure.test.junit/finish-case {:var-kind nil, :macro nil}]
[clojure.test.junit/finish-element {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test.junit/finish-suite {:var-kind nil, :macro nil}]
[clojure.test.junit/indent {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test.junit/junit-report {:var-kind nil, :macro nil}]
[clojure.test.junit/message-el {:var-kind nil, :macro nil}]
[clojure.test.junit/package-class {:var-kind nil, :macro nil}]
[clojure.test.junit/start-case {:var-kind nil, :macro nil}]
[clojure.test.junit/start-element {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test.junit/start-suite {:var-kind nil, :macro nil}]
[clojure.test.junit/suite-attrs {:var-kind nil, :macro nil}]
[clojure.test.junit/test-name {:var-kind nil, :macro nil}]
[clojure.test.junit/with-junit-output {:var-kind nil, :macro true}]
[clojure.test.tap/print-diagnostics {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test.tap/print-tap-diagnostic {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test.tap/print-tap-fail {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test.tap/print-tap-pass {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test.tap/print-tap-plan {:var-kind nil, :macro nil, :side-effect true}]
[clojure.test.tap/tap-report {:var-kind nil, :macro nil}]
[clojure.test.tap/with-tap-output {:var-kind nil, :macro true}]
[clojure.tools.reader/*alias-map* {:var-kind nil, :macro nil}]
[clojure.tools.reader/*data-readers* {:var-kind nil, :macro nil}]
[clojure.tools.reader/*default-data-reader-fn* {:var-kind nil, :macro nil}]
[clojure.tools.reader/*read-delim* {:var-kind nil, :macro nil}]
[clojure.tools.reader/*read-eval* {:var-kind nil, :macro nil}]
[clojure.tools.reader/*suppress-read* {:var-kind nil, :macro nil}]
[clojure.tools.reader/default-data-readers {:var-kind nil, :macro nil}]
[clojure.tools.reader/map-func {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.reader/read {:var-kind nil, :macro nil, :io-fn true, :evals-exprs true, :lazy false, :warn-if-ret-val-unused true}]
[clojure.tools.reader/read+string {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs true, :side-effect true, :predicate false}]
[clojure.tools.reader/read-regex {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.tools.reader/read-string {:var-kind nil, :macro nil, :evals-exprs true, :lazy false, :warn-if-ret-val-unused true}]
[clojure.tools.reader/read-symbol {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.tools.reader/resolve-symbol {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro false, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.reader/syntax-quote {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.reader.edn/read {:var-kind nil, :macro nil, :io-fn true, :lazy false, :warn-if-ret-val-unused true}]
[clojure.tools.reader.edn/read-string {:var-kind nil, :macro nil, :lazy false, :pure-if-fn-args-pure true}]
[clojure.tools.trace/ThrowableRecompose {:var-kind nil, :macro nil}]
[clojure.tools.trace/clone-throwable {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.trace/deftrace {:warn-if-ret-val-unused false, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.trace/dotrace {:warn-if-ret-val-unused false, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.trace/trace {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.tools.trace/trace-compose-throwable {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.trace/trace-fn-call {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn true, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.tools.trace/trace-form {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.trace/trace-forms {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.trace/trace-ns {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.tools.trace/trace-ns* {:var-kind nil, :macro nil, :side-effect true}]
[clojure.tools.trace/trace-special-form {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.tools.trace/trace-var* {:var-kind nil, :macro nil, :side-effect true}]
[clojure.tools.trace/trace-vars {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.tools.trace/traceable? {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.tools.trace/traced? {:warn-if-ret-val-unused true, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate true}]
[clojure.tools.trace/tracer {:var-kind nil, :macro nil, :io-fn true, :side-effect true}]
[clojure.tools.trace/untrace-ns {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.tools.trace/untrace-ns* {:var-kind nil, :macro nil, :side-effect true}]
[clojure.tools.trace/untrace-var* {:var-kind nil, :macro nil, :side-effect true}]
[clojure.tools.trace/untrace-vars {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro true, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.walk/keywordize-keys {:var-kind nil, :macro nil}]
[clojure.walk/macroexpand-all {:var-kind nil, :macro nil}]
[clojure.walk/postwalk {:var-kind nil, :macro nil}]
[clojure.walk/postwalk-demo {:var-kind nil, :macro nil}]
[clojure.walk/postwalk-replace {:var-kind nil, :macro nil}]
[clojure.walk/prewalk {:var-kind nil, :macro nil}]
[clojure.walk/prewalk-demo {:var-kind nil, :macro nil}]
[clojure.walk/prewalk-replace {:var-kind nil, :macro nil}]
[clojure.walk/stringify-keys {:var-kind nil, :macro nil}]
[clojure.walk/walk {:var-kind nil, :macro nil}]
[clojure.xml/*current* {:var-kind nil, :macro nil}]
[clojure.xml/*sb* {:var-kind nil, :macro nil}]
[clojure.xml/*stack* {:var-kind nil, :macro nil}]
[clojure.xml/*state* {:var-kind nil, :macro nil}]
[clojure.xml/attrs {:var-kind nil, :macro nil}]
[clojure.xml/content {:var-kind nil, :macro nil}]
[clojure.xml/content-handler {:var-kind nil, :macro nil}]
[clojure.xml/disable-external-entities {:warn-if-ret-val-unused false, :pure-fn false, :pure-if-fn-args-pure false, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect true, :predicate false}]
[clojure.xml/element {:var-kind nil, :macro nil}]
[clojure.xml/emit {:var-kind nil, :macro nil}]
[clojure.xml/emit-element {:var-kind nil, :macro nil, :side-effect true}]
[clojure.xml/parse {:var-kind nil, :macro nil}]
[clojure.xml/sax-parser {:warn-if-ret-val-unused true, :pure-fn true, :pure-if-fn-args-pure true, :lazy false, :macro nil, :io-fn false, :var-kind nil, :evals-exprs false, :side-effect false, :predicate false}]
[clojure.xml/startparse-sax {:var-kind nil, :macro nil}]
[clojure.xml/startparse-sax-safe {:var-kind nil, :macro nil}]
[clojure.xml/tag {:var-kind nil, :macro nil}]
[clojure.zip/append-child {:var-kind nil, :macro nil}]
[clojure.zip/branch? {:var-kind nil, :macro nil}]
[clojure.zip/children {:var-kind nil, :macro nil}]
[clojure.zip/down {:var-kind nil, :macro nil}]
[clojure.zip/edit {:var-kind nil, :macro nil}]
[clojure.zip/end? {:var-kind nil, :macro nil}]
[clojure.zip/insert-child {:var-kind nil, :macro nil}]
[clojure.zip/insert-left {:var-kind nil, :macro nil}]
[clojure.zip/insert-right {:var-kind nil, :macro nil}]
[clojure.zip/left {:var-kind nil, :macro nil}]
[clojure.zip/leftmost {:var-kind nil, :macro nil}]
[clojure.zip/lefts {:var-kind nil, :macro nil}]
[clojure.zip/make-node {:var-kind nil, :macro nil}]
[clojure.zip/next {:var-kind nil, :macro nil, :lazy false, :pure-fn true}]
[clojure.zip/node {:var-kind nil, :macro nil}]
[clojure.zip/path {:var-kind nil, :macro nil}]
[clojure.zip/prev {:var-kind nil, :macro nil}]
[clojure.zip/remove {:var-kind nil, :macro nil}]
[clojure.zip/replace {:var-kind nil, :macro nil}]
[clojure.zip/right {:var-kind nil, :macro nil}]
[clojure.zip/rightmost {:var-kind nil, :macro nil}]
[clojure.zip/rights {:var-kind nil, :macro nil}]
[clojure.zip/root {:var-kind nil, :macro nil}]
[clojure.zip/seq-zip {:var-kind nil, :macro nil}]
[clojure.zip/up {:var-kind nil, :macro nil}]
[clojure.zip/vector-zip {:var-kind nil, :macro nil}]
[clojure.zip/xml-zip {:var-kind nil, :macro nil}]
[clojure.zip/zipper {:var-kind nil, :macro nil}]})
