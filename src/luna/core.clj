(ns luna.core
  (:import (java.util.regex Pattern)
           (clojure.lang PersistentVector Keyword))
  (:require [clojure.set :as s]
            [clojure.string :as string]))

(def ^:private char-classes
  (into {}
        (map (fn [[k v]]
               (vector k (str v)))
             {:digits     #"\d"
              :!digits    #"\D"
              :words      #"\w"
              :!words     #"\W"
              :alpha      #"[a-zA-Z]"
              :upper      #"[A-Z]"
              :lower      #"[a-z]"
              :everything #"[\s\S]"
              :spaces     #"\s"
              :!spaces    #"\S"
              :h-tab      #"\t"
              :v-tab      #"\v"
              :return     #"\r"
              :new-line   #"\n"
              :new-form   #"\f"
              :dot        #"."})))



(def ^:private anchors #{:at-start :at-end})

(def ^:private quantifiers #{:atleast
                             :atmost
                             :between
                             :lazily
                             :exactly
                             :lazily-1
                             :greedily
                             :greedily-1
                             :possessively
                             :0-or-1})


(def ^:private groups #{:before
                        :after
                        :not-before
                        :not-after
                        :between})

(declare regify tokenize-chars token->rtoken)

(defmulti ^:private add-anchor (fn [anchor _] anchor))

(defmethod add-anchor :at-start [_ chars] ["^" chars])

(defmethod add-anchor :at-end [_ chars] [chars "$"])


(defmulti ^:private add-quantifier (fn
                                     ([quantifier _] quantifier)
                                     ([quantifier _ _] quantifier)))

(defmethod add-quantifier :atleast
  [_ lower] (format "{%s,}" lower))

(defmethod add-quantifier :atmost
  [_ upper] (format "{0,%s}" upper))

(defmethod add-quantifier :between
  [_ lower upper] (format "{%s,%s}" lower, upper))

(defmethod add-quantifier :exactly
  [_ exact] (format "{%s}" exact))

(defmethod add-quantifier :lazily
  ([_ char] [char "*?"]))

(defmethod add-quantifier :lazily-1
  ([_ char] [char "+?"]))

(defmethod add-quantifier :greedily
  ([_ char] [char "*"]))

(defmethod add-quantifier :greedily-1
  ([_ char] [char "+"]))

(defmethod add-quantifier :possessively
  ([_ char] [char "*+"]))

(defmethod add-quantifier :0-or-1
  ([_ char] [char "?"]))

(defmulti ^:private add-group (fn
                                ([group & _] group)))

(defmethod add-group :before
  ([_ char before] [char "(?=" before ")"]))

(defmethod add-group :not-before
  ([_ char before] [char "(?!" before ")"]))

(defmethod add-group :after
  ([_ char after] ["(?<=" after ")" char]))

(defmethod add-group :not-after
  ([_ char after] ["(?<!" after ")" char]))

(defmethod add-group :between
  ([_ char remaining]
   (let [after (if (= :not (first remaining))
                 "(?<!"
                 "(?<=")
         after-char (if (string? (first remaining))
                      (first remaining)
                      (second remaining))

         before (if (= :not (last (butlast remaining)))
                  "(?!"
                  "(?=")
         before-char (if (string? (last (butlast remaining)))
                       (last (butlast remaining))
                       (last remaining))]

     [after after-char ")" char before before-char ")"])))


(defmulti ^:private add-set-action (fn [set-action _] set-action))

(defmethod add-set-action :and
  ([_ chars]
   (let [chars-without-:and (s/difference (set chars)
                                          #{:and})]
     (->> chars-without-:and
          (map #(regify [:m %]))
          (interpose "&&")
          (apply str)
          (#(str "[" % "]"))))))

(defmethod add-set-action :not
  ([_ chars]
   (let [chars-without-:not (s/difference (set chars)
                                          #{:not})]
     (->> chars-without-:not
          (map #(regify [:m %]))
          (apply str)
          (#(str "[^" % "]"))))))

(defn- single-token [[t]]
  (cond
    (string? t)
    [t]

    (keyword? t)
    (if (= :or t)
      "|"
      [(get char-classes t)])

    (int? t)
    [(str t)]))


(defn- quant-tokens [t]
  (cond
    (or (some #{:between} t)
        (and (some #{:atleast} t)                           ;TODO find cleaner solution
             (some #{:atmost} t)))
    (vector (first t)
            (add-quantifier :between (nth t 2) (nth t 4)))
    (some #{:atleast :atmost :exactly} t)
    (vector (first t)
            (add-quantifier (nth t 1) (nth t 2)))
    (some quantifiers t)
    (add-quantifier (t 1) (t 0))))


(defn- anchor-tokens
  [[char _ anchor]]
  (vector (apply str (add-anchor anchor char))))

(defn- anchor-and-quant-tokens
  [[char & rem]]
  (let [anchor (first (s/intersection anchors (set rem)))
        quantifier (first (s/intersection quantifiers (set rem)))
        quantifier1 (first (filter int? rem))

        quantifier2 (second (filter int? rem))              ; for between
        ; order is important. quantifiers must be added before anchors
        char-with-quant (if (= :between quantifier)
                          (quant-tokens [char quantifier quantifier1 :and quantifier2])
                          (quant-tokens [char quantifier quantifier1]))
        char-with-anchor (first (anchor-tokens [(apply str char-with-quant) :when anchor]))]
    char-with-anchor))

(defn- common?
  "checks if there are common elements in x and y"
  [x y]
  (let [x (if (seqable? x) x [x])
        y (if (seqable? y) y [y])]
    (seq (s/intersection (set x) (set y)))))

(defn- should-be-match-enclosed? [type chars mods]
  (if (= :match-enc type)
    false
    (if (and (common? quantifiers (flatten chars))
             (common? quantifiers mods))
      (if (some #{:between} mods)
        ; if not int then it is a lookahead or lookbehind and not a quantifier.
        ; since :between is used for that too.
        (if (int? (second mods))
          true
          false)
        true)
      false)))

(defn- tokenize-chars [chars]
  (reduce (fn [res char]
            (cond
              (string? char)
              (conj res [char])

              (instance? Pattern char)
              (conj res [(str char)])

              (= :or char)
              (conj res [char])

              (vector? char)
              (conj res [char])

              (contains? char-classes char)
              (conj res [(char char-classes)])

              (and (int? char)
                   (not (common? #{:atleast :atmost :between :exactly} (last res))))
              ; if the previous token contains a quantifier,
              ; that means this int belongs inside that,
              ; else it is separate token
              (conj res [char])

              :else (conj (vec (butlast res))
                          (conj (last res) char))))
          []
          chars))


(defn- token->rtoken [t]
  ; TODO refactor
  (cond
    (and (common? anchors t) (common? quantifiers t))
    (anchor-and-quant-tokens t)

    (and (common? #{:to} (first t)) (common? quantifiers t))
    (quant-tokens (vec (cons (str "[" (ffirst t) "-" (last (first t)) "]")
                             (rest t))))

    (common? anchors t)
    (anchor-tokens t)

    (common? quantifiers t)
    (quant-tokens t)

    (common? #{:to} (first t))
    ; todo consider moving to a fn for aesthetics?
    [(str "[" (ffirst t) "-" (last (first t)) "]")]

    (and (= (count t) 1)
         (not (vector? (first t))))
    (single-token t)

    (vector? (first t))                                     ; if the char vec is nested for e.g [["a" "b"]] => #"[ab]"
    (->> (first t)
         tokenize-chars
         (map token->rtoken)
         (map (partial apply str))
         (apply str)
         ((fn [char]                                        ;fn to remove nested ranges, since it changes the meaning. [[0-9][A-B]] => [0-9A-b]
            (if (string/includes? char "[")
              (-> char
                  (string/replace "[" "")
                  (string/replace "]" ""))
              char)))
         (#(str "[" % "]")))))


(defmulti ^:private rtokens->str (fn [type _] type))

(defmethod rtokens->str :m [_ chars] (rtokens->str :match chars))

(defmethod rtokens->str :match [_ chars]
  (->> chars
       (apply str)))

(defmethod rtokens->str :c [_ chars] (rtokens->str :capture chars))

(defmethod rtokens->str :capture [_ chars]
  (->> chars
       (apply str)
       (#(str "(" % ")"))))

(defmethod rtokens->str :match-enc [_ chars]
  (->> chars
       (apply str)
       (#(str "(?:" % ")"))))

(defn- add-mods [mods x]
  (if (seq mods)
    (cond
      (#{:lazily :greedily :possessively} (first mods))
      (add-mods (rest mods) (apply str (add-quantifier (first mods) x)))

      (and (common? groups mods)
           (some string? mods))
      (if (some #{:between} mods)
        (add-group (nth mods 0) x (vec (rest mods)))
        (add-group (nth mods 0) x (nth mods 1)))

      (string/includes? x "(?:")
      (token->rtoken (vec (cons x mods)))

      :else
      (let [chars (if (string/includes? x "|")
                    (str "[" (string/replace x "|" "") "]")
                    x)]
        (token->rtoken (vec (cons chars mods)))))
    x))



; TODO find better name
(defn- eval-mods [mods]
  (mapv (fn [mod]
          (if ((some-fn string? vector?
                        #(contains? char-classes %)
                        #(= Pattern (type %)))
               mod)
            (if (vector? mod)
              (if (contains? #{:m :match :c :capture} (first mod))
                (regify mod)
                (regify [:m mod]))
              (regify [:m mod]))
            mod))
        mods))

(defn- handle-set
  [chars]
  (let [set-action (if (some #{:not} chars) :not :and)]
    (add-set-action set-action chars)))

(defmulti ^:private regify
          "Takes in a string, regex.Pattern or a vector with the Luna DSL.
          If a regex.Pattern is passed then it will be converted to a string.
          If a vector is passed, it will be evaluated to form a string.
          Returns a string."
          (fn [x]
            (type x)))

(defmethod regify String [x] x)

(defmethod regify Pattern [x] (str x))

(defmethod regify Keyword [x]
  (if (= :or x)
    "|"
    (throw (IllegalArgumentException. "unrecognized argument.\nDid you mean :or ?"))))


(defmethod regify PersistentVector [[type chars & mods]]
  (cond
    (should-be-match-enclosed? type chars mods)
    (regify (vec (cons :match-enc (cons chars mods))))

    (set? chars)
    (handle-set chars)

    :else
    (->> (if ((some-fn string? keyword?) chars) [chars] chars)
         tokenize-chars
         (map token->rtoken)
         (map (partial apply str))
         (rtokens->str type)
         (add-mods (eval-mods mods))
         (apply str))))

(defn pre
  "Takes in a string, regex.Pattern, or vector (with the Luna DSL),
   converts to a string if not already a string, concatenates the `xs`,
   and returns a regex.Pattern of the concatenated `xs`.

   Example usage
   ```clojure
   user=> (pre \"a\" #\"b\" [:match [\"c\" :when :at-start]])
   #\"ab^c\"
   ```"
  [& xs]
  (->> xs
       (map regify)
       (apply str)
       Pattern/compile))

