# luna
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.abhinav/luna.svg)](https://clojars.org/org.clojars.abhinav/luna)
[![codecov](https://codecov.io/gh/AbhinavOmprakash/luna/branch/master/graph/badge.svg?token=40ZSIXXSE3)](https://codecov.io/gh/AbhinavOmprakash/luna)


No more regrets, wield the power of regex with the readability of English with luna.

# About

luna is a Domain specific language (DSL) that is readable and translates into a `Regex.Pattern` object. luna is still in
Beta but don't let this discourage you from using it, it has a good test suite and bug reports are key to improving it.

# Why?

Readable code can be hard to maintain. Unreadable code can be impossible to maintain.

# Installing and Using
add this to your project.clj file
```clojure
:dependencies [[org.clojars.abhinav/luna "0.1.0-SNAPSHOT"]]
```
Luna has one function `pre` that does the heavy lifting.
```clojure
;; import it
(ns user.core
  (:require [luna.core :as luna]))

(luna/pre [:match ["x" :digits :atleast 4 :times] :when :at-start])
;;=> #"^x\d{4,}"
```
# Contributing

I welcome contributions, even from first-timers. Feedbacks and suggestions are welcome too.

## If you'd like to contribute but don't know how

### Test cases

The easiest thing you can do to contribute is write a test case, this project can never have too many test cases.

### Documentation

Documentation is very important, more so than the code in the project, so I value these contributions highly. There will
be some parts (hopefully not a lot) of the documentation that may not make sense, or maybe wrong, or can be worded
differently.

### Refactoring

I welcome refactors like

- Variable and function renaming.
- Extracting functions.
- Moving things around to make more sense.

# Grammar/language

the `pre` function is used to parse the dsl and return a `regex.Pattern` object.

the arguments to `pre` can be plain strings, or vectors, or a `Pattern` object.

```clojure
=> (pre "xy")
#"xy"
; pre can take multiple args
=> (pre "a" #"b" [:match "c" :when :at-start])
#"ab^c"
```

The first element in the vector determines how the rest is processed. There are two main and commonly used
keywords `:match` (or `:m`) and `:capture` (or `:c`) that are valid first elements.

```clojure
=> (pre [:match "xy"])
#"xy"
=> (pre [:capture "xy"])
#"(xy)"
```

The next element is either a string or a vector, containing character classes. The valid syntax of the vector depends on
whether `:match` or `:capture` was used.

## character class syntax for `:match`

to be used as

```clojure
[:match ["xy"]]

;;      ----char-class vector----
[:match ["x" :when :at-start "y"]]
```

I will omit `[:match ...]` for brevity.

examples of valid char-class vector

```clojure
;; by default the elements in the char-class vector are evaluate to a string and separated by | in match
["xy"] => #"xy"
["x" "y"] => #"x|y"
["x" "y" "z"] => #"x|y|z"

;; if you would prefer to concatenate them, then use a nested vector
[["x" "y" "z"]] => #"xyz"

;; using ranges in character classes
[[1 :to 7]] => #"[1-7]"
[1 [2 :to 5]] => #"[1[2-5]]"

;; using anchors inside vector
["x" :when :at-start] => #"^x"
["x" :when :at-start "y"] => #"^xy"

;; using quantifiers inside vector
["x" :atleast 5 :times "y"] => #"x{5,}y"

;; the :times can be omitted but helps with readability
["x" :atleast 5 "y"] => #"x{5,}y"

;;combining anchors and quantifiers
["x" :atleast 5 :times :when :at-start "y"] => #"^x{5,}y"
```

# after the character class vector we have modifiers.

```clojure
;;             -modifiers-
[:match ["xy"] :atleast 2] => #"xy{2,}"
;;             ---modifiers---
[:match ["xy"] :when :at-start] => #"^xy"

```

Note! if you're using quantifiers and/or anchors inside the character class vector
**and** outside then the result will be a "match everything enclosed"
here's an example

```clojure
[:match ["x" :atleast 5 "y"] :atleast 2] => #"(?:x{5}y){2}"
```

# :match-enc

`:match-enc[closed]`
by default
`[:match ["x" :atleast 5 "y"] ]` yields `#"x{5}|y"`
instead if you want `#"(?:x{5}|y)`
use :match-enc

```clojure
[:match-enc ["x" :atleast 5 "y"]] => #"(?:x{5}y)"
```

# sets

if you wish to use set constructs like negation `[^abc]` or intersection `[abc&&[ab]]`
you can use clojure's literal set notation

```clojure
; negation
[:match #{:not "abc"}] => #"[^abc]"
[:match #{:not "abc" :upper [1 :to 5]}] => #"[^abcA-Z1-5]"

;; intersection
[:match #{:and "abc" "ab"}] => #"[abc&&[ab]]"
[:match #{:and "abc" :upper [1 :to 4]}] => #"[abc&&[A-Z]&&[1-4]]"

; combining both
[:match #{:and "abc" #{:not "ab" :digits}}] => #"[abc&&[^ab1-9]]"
```

# capture

the syntax of capture is similar to `:match`

## Anchors

### Match

```clojure
[:match "x" :when :at-start] ; #"^x"
[:match "xy" :when :at-start] ; #"^xy"
[:match ["xy"] :when :at-start] ; #"^xy"
[:match ["x" :or "y"] :when :at-start] ; #"^x|y"
[:match ["x" :when :at-start :or "y"]] ; #"^x|y"

[:match [:digits] :when :at-end] ; #"\d$"
```

### Capture

```clojure
[:capture "x" :when :at-start] ; #"^(x)"
[:capture "xy" :when :at-start] ; #"^(xy)"
[:capture ["x" "y"] :when :at-start] ; #"^(xy)"
[:capture [["x" "y"]] :when :at-start] ; #"^([xy])"

[:capture :digits :when :at-end] ; #"$(\d)"
[:capture "x" :when :at-word-start] ; #"\b(x)"
[:capture "x" :when :not :at-word-start] ; #"\B(x)"
```

## Group constructs

### assertions

```clojure
;; lookahead positive and negative
[:match "x" :when :before "y"] ; #"x(?=y)"
[:match "x" :when :not-before "y"] ; #"x(?!y)"

;; lookbehind positive and negative
[:match "x" :when :after "y"] ; #"(?<=y)x"
[:match "x" :when :not-after "y"] ; #"(?<!y)x"

;; both can be combined
[:match "y" :between "x" :and "z"]  ; #"(?<=x)y(?=z)"
[:match "y" :between "x" :and :not "z"]  ; #"(?<=x)y(?!z)"
[:match "y" :between :not "x" :and :not "z"]  ; #"(?<!x)y(?!z)"
```

# Quantifiers

note: the `:times` can be omitted if you want but it helps with readability

```clojure
[:match "xyz" :lazily] ; #"xyz*?"
[:match ["xyz"] :lazily-1] ; #"xyz+?"

[:match ["xyz"] :greedily] ; #"xyz*"
[:match ["xyz"] :greedily-1] ; #"xyz+"

[:match ["xyz"] :possessively] ; #"xyz*+"

[:match ["x"] :atleast 3 :times] ; #"x{3,}"
[:match ["x"] :atleast 3] ; #"x{3,}"

[:match ["x"] :atmost 3 :times] ; #"x{0,3}"

;; Combining both
[:match ["x"] :atleast 3 :atmost 5 :times] ; #"x{3,5}
[:match ["x"] :between 3 :to 5 :times] ;  #"x{3,5}
```

# A few practical examples

```clojure
#"^\S+@\S+$" ;regex

(pre [:m [:!spaces :greedily-1 :when :at-start
          "@" :!spaces :greedily-1 :when :at-end]])
```

```clojure
#"^[A-Z0-9+_.-]+@[A-Z0-9.-]+$"

(pre [:m [[["A" :to "Z"] [0 :to 9] "+_.-"]] :greedily-1 :when :at-start]
     "@"
     [:m [[["A" :to "Z"] [0 :to 9] ".-"]] :greedily-1 :when :at-end])

#"^([0-9]{4})-(1[0-2]|0[1-9])"


(pre [:c [[0 :to 9] :atleast 4] :when :at-start]
     "-"
     [:c [1 [0 :to 2] :or 0 [1 :to 9]]])

#"^([0-9]{4})-W(5[0-3]|[1-4][0-9]|0[1-9])$"

(pre [:c [[0 :to 9] :atleast 4] :when :at-start]
     "-W"
     [:c [5 [0 :to 3] :or [1 :to 4] [0 :to 9] :or 0 [1 :to 9]] :when :at-end])
```

```clojure
#"(?<=>)[\s\S]*?(?=<)"
(pre
  [:match [:everything] :lazily :between ">" :and "<"])


#"^[A-Z]{1,2}[0-9R][0-9A-Z]?[0-9][ABD-HJLNP-UW-Z]{2}$"
(pre [:m :upper :between 1 :and 2 :when :at-start]
     [:m [[[0 :to 9] "R"]]]
     [:m [[[0 :to 9] :upper]] :0-or-1]
     [:m [[[0 :to 9]]]]
     [:m [["AB" ["D" :to "H"] "JLN" ["P" :to "U"] ["W" :to "Z"]]]
      :atleast 2 :when :at-end])

```
