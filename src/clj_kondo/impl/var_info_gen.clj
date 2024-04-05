(ns clj-kondo.impl.var-info-gen
  "GENERATED, DO NOT EDIT."
  {:no-doc true})
  (in-ns 'clj-kondo.impl.var-info)

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
*repl*
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
ArrayManager
EMPTY-NODE
IVecImpl
Inst
NaN?
PrintWriter-on
StackTraceElement->vec
Throwable->map
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
partitionv
partitionv-all
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
splitv-at
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

  (def unused-values '#{clojure.core/*
clojure.core/*'
clojure.core/+
clojure.core/+'
clojure.core/-
clojure.core/-'
clojure.core/->Eduction
clojure.core//
clojure.core/<
clojure.core/<=
clojure.core/=
clojure.core/==
clojure.core/>
clojure.core/>=
clojure.core/Inst
clojure.core/NaN?
clojure.core/PrintWriter-on
clojure.core/StackTraceElement->vec
clojure.core/Throwable->map
clojure.core/abs
clojure.core/aclone
clojure.core/aget
clojure.core/alength
clojure.core/all-ns
clojure.core/any?
clojure.core/array-map
clojure.core/assoc
clojure.core/assoc!
clojure.core/assoc-in
clojure.core/associative?
clojure.core/bigdec
clojure.core/bigint
clojure.core/biginteger
clojure.core/bit-and
clojure.core/bit-and-not
clojure.core/bit-clear
clojure.core/bit-flip
clojure.core/bit-not
clojure.core/bit-or
clojure.core/bit-set
clojure.core/bit-shift-left
clojure.core/bit-shift-right
clojure.core/bit-test
clojure.core/bit-xor
clojure.core/boolean
clojure.core/boolean-array
clojure.core/boolean?
clojure.core/bound-fn*
clojure.core/bound?
clojure.core/bounded-count
clojure.core/butlast
clojure.core/byte
clojure.core/byte-array
clojure.core/bytes?
clojure.core/case-fallthrough-err-impl
clojure.core/char
clojure.core/char-array
clojure.core/char-escape-string
clojure.core/char-name-string
clojure.core/char?
clojure.core/chunk
clojure.core/chunk-buffer
clojure.core/chunk-cons
clojure.core/chunk-first
clojure.core/chunk-next
clojure.core/chunk-rest
clojure.core/chunked-seq?
clojure.core/class
clojure.core/class?
clojure.core/clojure-version
clojure.core/coll?
clojure.core/compare
clojure.core/completing
clojure.core/concat
clojure.core/conj
clojure.core/conj!
clojure.core/cons
clojure.core/contains?
clojure.core/count
clojure.core/counted?
clojure.core/cycle
clojure.core/dec
clojure.core/dec'
clojure.core/decimal?
clojure.core/dedupe
clojure.core/delay?
clojure.core/denominator
clojure.core/disj
clojure.core/dissoc
clojure.core/dissoc!
clojure.core/distinct
clojure.core/distinct?
clojure.core/double
clojure.core/double-array
clojure.core/double?
clojure.core/drop
clojure.core/drop-last
clojure.core/drop-while
clojure.core/empty
clojure.core/empty?
clojure.core/ensure-reduced
clojure.core/enumeration-seq
clojure.core/even?
clojure.core/ex-cause
clojure.core/ex-data
clojure.core/ex-info
clojure.core/ex-message
clojure.core/false?
clojure.core/ffirst
clojure.core/file-seq
clojure.core/filter
clojure.core/find
clojure.core/find-keyword
clojure.core/first
clojure.core/flatten
clojure.core/float
clojure.core/float-array
clojure.core/float?
clojure.core/fn?
clojure.core/fnext
clojure.core/for
clojure.core/format
clojure.core/frequencies
clojure.core/future?
clojure.core/gensym
clojure.core/get
clojure.core/get-in
clojure.core/halt-when
clojure.core/hash
clojure.core/hash-map
clojure.core/hash-ordered-coll
clojure.core/hash-set
clojure.core/hash-unordered-coll
clojure.core/ident?
clojure.core/identical?
clojure.core/identity
clojure.core/ifn?
clojure.core/inc
clojure.core/inc'
clojure.core/indexed?
clojure.core/infinite?
clojure.core/inst-ms
clojure.core/inst-ms*
clojure.core/inst?
clojure.core/int
clojure.core/int-array
clojure.core/int?
clojure.core/integer?
clojure.core/interleave
clojure.core/interpose
clojure.core/into
clojure.core/into-array
clojure.core/iterate
clojure.core/iteration
clojure.core/iterator-seq
clojure.core/keep
clojure.core/keep-indexed
clojure.core/key
clojure.core/keys
clojure.core/keyword
clojure.core/keyword?
clojure.core/last
clojure.core/line-seq
clojure.core/list
clojure.core/list*
clojure.core/list?
clojure.core/long
clojure.core/long-array
clojure.core/macroexpand
clojure.core/macroexpand-1
clojure.core/make-array
clojure.core/map
clojure.core/map-entry?
clojure.core/map-indexed
clojure.core/map?
clojure.core/mapcat
clojure.core/max
clojure.core/merge
clojure.core/meta
clojure.core/min
clojure.core/mix-collection-hash
clojure.core/mod
clojure.core/name
clojure.core/nat-int?
clojure.core/neg-int?
clojure.core/neg?
clojure.core/next
clojure.core/nfirst
clojure.core/nil?
clojure.core/nnext
clojure.core/not
clojure.core/not-empty
clojure.core/not=
clojure.core/nth
clojure.core/nthnext
clojure.core/nthrest
clojure.core/num
clojure.core/number?
clojure.core/numerator
clojure.core/object-array
clojure.core/odd?
clojure.core/parse-boolean
clojure.core/parse-double
clojure.core/parse-long
clojure.core/parse-uuid
clojure.core/partition
clojure.core/partition-all
clojure.core/partition-by
clojure.core/peek
clojure.core/pmap
clojure.core/pop
clojure.core/pop!
clojure.core/pos-int?
clojure.core/pos?
clojure.core/qualified-ident?
clojure.core/qualified-keyword?
clojure.core/qualified-symbol?
clojure.core/quot
clojure.core/rand
clojure.core/rand-int
clojure.core/rand-nth
clojure.core/random-sample
clojure.core/random-uuid
clojure.core/range
clojure.core/ratio?
clojure.core/rational?
clojure.core/rationalize
clojure.core/re-find
clojure.core/re-groups
clojure.core/re-matcher
clojure.core/re-matches
clojure.core/re-pattern
clojure.core/re-seq
clojure.core/read+string
clojure.core/read-string
clojure.core/reader-conditional
clojure.core/reader-conditional?
clojure.core/record?
clojure.core/reduced?
clojure.core/reductions
clojure.core/rem
clojure.core/remove
clojure.core/repeat
clojure.core/repeatedly
clojure.core/replace
clojure.core/replicate
clojure.core/rest
clojure.core/resultset-seq
clojure.core/reverse
clojure.core/reversible?
clojure.core/rseq
clojure.core/rsubseq
clojure.core/second
clojure.core/select-keys
clojure.core/seq
clojure.core/seq-to-map-for-destructuring
clojure.core/seq?
clojure.core/seqable?
clojure.core/sequence
clojure.core/sequential?
clojure.core/set
clojure.core/set?
clojure.core/short
clojure.core/short-array
clojure.core/shuffle
clojure.core/simple-ident?
clojure.core/simple-keyword?
clojure.core/simple-symbol?
clojure.core/some
clojure.core/some?
clojure.core/sorted-map
clojure.core/sorted-set
clojure.core/sorted?
clojure.core/special-symbol?
clojure.core/split-at
clojure.core/split-with
clojure.core/str
clojure.core/string?
clojure.core/struct
clojure.core/struct-map
clojure.core/subs
clojure.core/subseq
clojure.core/subvec
clojure.core/symbol
clojure.core/symbol?
clojure.core/tagged-literal
clojure.core/tagged-literal?
clojure.core/take
clojure.core/take-last
clojure.core/take-nth
clojure.core/take-while
clojure.core/to-array
clojure.core/to-array-2d
clojure.core/transient
clojure.core/tree-seq
clojure.core/true?
clojure.core/type
clojure.core/unchecked-add
clojure.core/unchecked-add-int
clojure.core/unchecked-byte
clojure.core/unchecked-char
clojure.core/unchecked-dec
clojure.core/unchecked-dec-int
clojure.core/unchecked-divide-int
clojure.core/unchecked-double
clojure.core/unchecked-float
clojure.core/unchecked-inc
clojure.core/unchecked-inc-int
clojure.core/unchecked-int
clojure.core/unchecked-long
clojure.core/unchecked-multiply
clojure.core/unchecked-multiply-int
clojure.core/unchecked-negate
clojure.core/unchecked-negate-int
clojure.core/unchecked-remainder-int
clojure.core/unchecked-short
clojure.core/unchecked-subtract
clojure.core/unchecked-subtract-int
clojure.core/unreduced
clojure.core/unsigned-bit-shift-right
clojure.core/update
clojure.core/update-in
clojure.core/uri?
clojure.core/uuid?
clojure.core/val
clojure.core/vals
clojure.core/vec
clojure.core/vector
clojure.core/vector-of
clojure.core/vector?
clojure.core/volatile!
clojure.core/volatile?
clojure.core/with-meta
clojure.core/xml-seq
clojure.core/zero?
clojure.core/zipmap
clojure.core.async.impl.dispatch/in-dispatch-thread?
clojure.core.async.impl.ioc-macros/->Call
clojure.core.async.impl.ioc-macros/->Case
clojure.core.async.impl.ioc-macros/->CatchHandler
clojure.core.async.impl.ioc-macros/->CondBr
clojure.core.async.impl.ioc-macros/->Const
clojure.core.async.impl.ioc-macros/->CustomTerminator
clojure.core.async.impl.ioc-macros/->Dot
clojure.core.async.impl.ioc-macros/->EndFinally
clojure.core.async.impl.ioc-macros/->Fn
clojure.core.async.impl.ioc-macros/->InstanceInterop
clojure.core.async.impl.ioc-macros/->Jmp
clojure.core.async.impl.ioc-macros/->PopTry
clojure.core.async.impl.ioc-macros/->PushTry
clojure.core.async.impl.ioc-macros/->RawCode
clojure.core.async.impl.ioc-macros/->Recur
clojure.core.async.impl.ioc-macros/->Return
clojure.core.async.impl.ioc-macros/->StaticCall
clojure.core.async.impl.ioc-macros/-item-to-ssa
clojure.core.async.impl.ioc-macros/add-block
clojure.core.async.impl.ioc-macros/add-instruction
clojure.core.async.impl.ioc-macros/aget-object
clojure.core.async.impl.ioc-macros/all
clojure.core.async.impl.ioc-macros/aset-all!
clojure.core.async.impl.ioc-macros/assoc-in-plan
clojure.core.async.impl.ioc-macros/block-references
clojure.core.async.impl.ioc-macros/count-persistent-values
clojure.core.async.impl.ioc-macros/emit-hinted
clojure.core.async.impl.ioc-macros/emit-instruction
clojure.core.async.impl.ioc-macros/finished?
clojure.core.async.impl.ioc-macros/gen-plan
clojure.core.async.impl.ioc-macros/get-binding
clojure.core.async.impl.ioc-macros/get-block
clojure.core.async.impl.ioc-macros/get-in-plan
clojure.core.async.impl.ioc-macros/get-plan
clojure.core.async.impl.ioc-macros/index-block
clojure.core.async.impl.ioc-macros/index-instruction
clojure.core.async.impl.ioc-macros/index-state-machine
clojure.core.async.impl.ioc-macros/instruction?
clojure.core.async.impl.ioc-macros/item-to-ssa
clojure.core.async.impl.ioc-macros/let-binding-to-ssa
clojure.core.async.impl.ioc-macros/make-env
clojure.core.async.impl.ioc-macros/map->Call
clojure.core.async.impl.ioc-macros/map->Case
clojure.core.async.impl.ioc-macros/map->CatchHandler
clojure.core.async.impl.ioc-macros/map->CondBr
clojure.core.async.impl.ioc-macros/map->Const
clojure.core.async.impl.ioc-macros/map->CustomTerminator
clojure.core.async.impl.ioc-macros/map->Dot
clojure.core.async.impl.ioc-macros/map->EndFinally
clojure.core.async.impl.ioc-macros/map->Fn
clojure.core.async.impl.ioc-macros/map->InstanceInterop
clojure.core.async.impl.ioc-macros/map->Jmp
clojure.core.async.impl.ioc-macros/map->PopTry
clojure.core.async.impl.ioc-macros/map->PushTry
clojure.core.async.impl.ioc-macros/map->RawCode
clojure.core.async.impl.ioc-macros/map->Recur
clojure.core.async.impl.ioc-macros/map->Return
clojure.core.async.impl.ioc-macros/map->StaticCall
clojure.core.async.impl.ioc-macros/mark-transitions
clojure.core.async.impl.ioc-macros/nested-go?
clojure.core.async.impl.ioc-macros/no-op
clojure.core.async.impl.ioc-macros/parse-to-state-machine
clojure.core.async.impl.ioc-macros/persistent-value?
clojure.core.async.impl.ioc-macros/pop-binding
clojure.core.async.impl.ioc-macros/print-plan
clojure.core.async.impl.ioc-macros/propagate-recur
clojure.core.async.impl.ioc-macros/propagate-transitions
clojure.core.async.impl.ioc-macros/push-alter-binding
clojure.core.async.impl.ioc-macros/push-binding
clojure.core.async.impl.ioc-macros/reads-from
clojure.core.async.impl.ioc-macros/run-passes
clojure.core.async.impl.ioc-macros/set-block
clojure.core.async.impl.ioc-macros/terminate-block
clojure.core.async.impl.ioc-macros/terminator-code
clojure.core.async.impl.ioc-macros/update-in-plan
clojure.core.async.impl.ioc-macros/var-name
clojure.core.async.impl.ioc-macros/writes-to
clojure.core.cache/->BasicCache
clojure.core.cache/->FIFOCache
clojure.core.cache/->FnCache
clojure.core.cache/->LIRSCache
clojure.core.cache/->LRUCache
clojure.core.cache/->LUCache
clojure.core.cache/->SoftCache
clojure.core.cache/->TTLCacheQ
clojure.core.cache/basic-cache-factory
clojure.core.cache/defcache
clojure.core.cache/fifo-cache-factory
clojure.core.cache/lirs-cache-factory
clojure.core.cache/lru-cache-factory
clojure.core.cache/lu-cache-factory
clojure.core.cache/make-reference
clojure.core.cache/soft-cache-factory
clojure.core.cache/ttl-cache-factory
clojure.core.memoize/->PluggableMemoization
clojure.core.memoize/->RetryingDelay
clojure.core.memoize/build-memoizer
clojure.core.memoize/fifo
clojure.core.memoize/lazy-snapshot
clojure.core.memoize/lru
clojure.core.memoize/memo
clojure.core.memoize/memo-fifo
clojure.core.memoize/memo-lru
clojure.core.memoize/memo-lu
clojure.core.memoize/memo-ttl
clojure.core.memoize/memo-unwrap
clojure.core.memoize/memoized?
clojure.core.memoize/memoizer
clojure.core.memoize/snapshot
clojure.core.memoize/ttl
clojure.core.protocols/datafy
clojure.core.protocols/nav
clojure.data/diff
clojure.data.codec.base64/dec-length
clojure.data.codec.base64/decode
clojure.data.codec.base64/enc-length
clojure.data.codec.base64/encode
clojure.data.codec.base64/pad-length
clojure.data.csv/read-csv
clojure.data.csv/read-csv-from
clojure.data.json/invalid-array-exception
clojure.data.json/json-str
clojure.data.json/read
clojure.data.json/read-json
clojure.data.json/read-str
clojure.data.json/write-str
clojure.data.priority-map/->PersistentPriorityMap
clojure.data.priority-map/priority->set-of-items
clojure.data.priority-map/priority-map
clojure.data.priority-map/rsubseq
clojure.data.priority-map/subseq
clojure.java.io/as-file
clojure.java.io/as-relative-path
clojure.java.io/as-url
clojure.java.io/file
clojure.java.io/input-stream
clojure.java.io/output-stream
clojure.java.io/reader
clojure.java.io/resource
clojure.java.io/writer
clojure.java.jdbc/add-connection
clojure.java.jdbc/as-sql-name
clojure.java.jdbc/create-table-ddl
clojure.java.jdbc/db-connection
clojure.java.jdbc/db-find-connection
clojure.java.jdbc/db-is-rollback-only
clojure.java.jdbc/drop-table-ddl
clojure.java.jdbc/get-connection
clojure.java.jdbc/get-isolation-level
clojure.java.jdbc/get-level
clojure.java.jdbc/metadata-query
clojure.java.jdbc/metadata-result
clojure.java.jdbc/prepare-statement
clojure.java.jdbc/quoted
clojure.java.jdbc/reducible-result-set
clojure.java.jdbc/result-set-read-column
clojure.java.jdbc/result-set-seq
clojure.java.jdbc/sql-value
clojure.java.jdbc/string-array
clojure.main/err->msg
clojure.main/ex-str
clojure.main/ex-triage
clojure.math/IEEE-remainder
clojure.math/abs
clojure.math/acos
clojure.math/add-exact
clojure.math/asin
clojure.math/atan
clojure.math/atan2
clojure.math/cbrt
clojure.math/ceil
clojure.math/copy-sign
clojure.math/cos
clojure.math/cosh
clojure.math/decrement-exact
clojure.math/exp
clojure.math/expm1
clojure.math/floor
clojure.math/floor-div
clojure.math/floor-mod
clojure.math/get-exponent
clojure.math/hypot
clojure.math/increment-exact
clojure.math/log
clojure.math/log10
clojure.math/log1p
clojure.math/max
clojure.math/min
clojure.math/multiply-exact
clojure.math/negate-exact
clojure.math/negative-exact
clojure.math/next-after
clojure.math/next-down
clojure.math/next-up
clojure.math/pow
clojure.math/random
clojure.math/rint
clojure.math/round
clojure.math/scalb
clojure.math/signum
clojure.math/sin
clojure.math/sinh
clojure.math/sqrt
clojure.math/subtract-exact
clojure.math/tan
clojure.math/tanh
clojure.math/to-degrees
clojure.math/to-radians
clojure.math/ulp
clojure.math.numeric-tower/abs
clojure.math.numeric-tower/ceil
clojure.math.numeric-tower/exact-integer-sqrt
clojure.math.numeric-tower/expt
clojure.math.numeric-tower/floor
clojure.math.numeric-tower/gcd
clojure.math.numeric-tower/integer-length
clojure.math.numeric-tower/lcm
clojure.math.numeric-tower/round
clojure.math.numeric-tower/sqrt
clojure.set/difference
clojure.set/index
clojure.set/intersection
clojure.set/join
clojure.set/map-invert
clojure.set/project
clojure.set/rename
clojure.set/rename-keys
clojure.set/subset?
clojure.set/superset?
clojure.set/union
clojure.string/blank?
clojure.string/capitalize
clojure.string/ends-with?
clojure.string/escape
clojure.string/includes?
clojure.string/index-of
clojure.string/join
clojure.string/last-index-of
clojure.string/lower-case
clojure.string/re-quote-replacement
clojure.string/reverse
clojure.string/split
clojure.string/split-lines
clojure.string/starts-with?
clojure.string/trim
clojure.string/trim-newline
clojure.string/triml
clojure.string/trimr
clojure.string/upper-case
clojure.tools.reader/map-func
clojure.tools.reader/read
clojure.tools.reader/read-string
clojure.tools.reader/resolve-symbol
clojure.tools.reader/syntax-quote
clojure.tools.reader.edn/read
clojure.tools.trace/clone-throwable
clojure.tools.trace/deftrace
clojure.tools.trace/dotrace
clojure.tools.trace/trace-compose-throwable
clojure.tools.trace/trace-form
clojure.tools.trace/trace-forms
clojure.tools.trace/trace-special-form
clojure.tools.trace/traceable?
clojure.tools.trace/traced?
clojure.xml/sax-parser
clojure.zip/next})
