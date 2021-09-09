(ns clj-kondo.impl.analyzer.asm
  (:require [clojure.java.io :as io])
  (:import [org.objectweb.asm ClassReader ClassVisitor Opcodes]))

;; see https://github.com/consulo/objectweb-asm/blob/master/asm/src/main/java/org/objectweb/asm/Opcodes.java
(defn opcode->keyword [opcode]
  (get {Opcodes/ACC_PUBLIC :public} opcode opcode))

(def class-visitor
  (proxy [ClassVisitor] [Opcodes/ASM9]
    (visitField [access name desc signature value]
      (when (identical? :public (opcode->keyword access))
        (prn [:field (opcode->keyword access) name desc signature value])))
    (visitMethod [access name desc signature exceptions]
      (when (identical? :public (opcode->keyword access))
        (prn [:method (opcode->keyword access) name desc signature exceptions])))
    (visitModule [name access version]
      (prn [:module name]))
    (visitInnerClass [name outer-name inner-name access]
      (when true #_(identical? :public (opcode->keyword access))
        (prn [:inner-class name outer-name inner-name])))))

(def is (let [class-resource (io/resource "clojure/lang/PersistentVector.class")
              is (io/input-stream class-resource)]
          is))

(def cr (ClassReader. is))
(.accept cr class-visitor ClassReader/SKIP_DEBUG)
