(ns luna.core-test
  (:require [clojure.test :refer :all]
            [luna.core :refer :all]))

(defn- r-eq
  "converts a and b to str and tests equality"
  [a b]
  (= (str a) (str b)))

(def regify #'luna.core/regify)

(deftest test-regify
  (is (= (regify "x")
         "x"))
  (is (= (regify #"x")
         "x"))
  (is (= (regify :or)
         "|"))
  #_(is (thrown? IllegalArgumentException (regify :x)))
  (is (= (regify [:match "x"])
         "x")))

(deftest test-pre
  (testing "plain strings passed to pre render to regex.Patterns")
  (is (r-eq #"a" (pre "a")))
  (is (r-eq #"[1-9]" (pre "[1-9]")))
  (is (r-eq #"[$!@#$!@#$!@]" (pre "[$!@#$!@#$!@]")))

  (testing "regexes passed to pre are returned as regexes")
  (is (r-eq #"^a" (pre #"^a")))
  (is (r-eq #"[1-9]?" (pre #"[1-9]?")))
  (is (r-eq #"!@#$!@#$!@#$!@#$" (pre #"!@#$!@#$!@#$!@#$")))

  (testing "pre can take multiple args")
  (is (r-eq #"abc" (pre "a" "b" "c")))
  (is (r-eq #"abc" (pre #"a" #"b" #"c")))
  (is (r-eq #"abc" (pre [:match ["ab"]] "c")))

  (testing "pre can take multiple args")
  (is (r-eq #"abc" (pre "a" "b" "c")))
  (is (r-eq #"abc" (pre "a" #"b" [:m "c"])))

  (testing ":or can be used in pre")
  (is (r-eq #"a|b|c" (pre "a" :or "b" :or "c")))
  (is (r-eq #"a|bc" (pre "a" :or "b" "c"))))


(deftest match
  (testing "match with only char vecs")
  (is (r-eq #"a" (pre [:match ["a"]])))
  (is (r-eq #"ab" (pre [:match ["a" "b"]])))
  (is (r-eq #"[ab]" (pre [:match [["a" "b"]]])))
  (is (r-eq #"a" (pre [:match "a"])))
  (is (r-eq #"\d" (pre [:match [:digits]])))

  (testing "ints work in char class vector")
  (is (r-eq #"123" (pre [:match [1 2 3]])))
  (is (r-eq #"[123]" (pre [:match [[1 2 3]]])))

  (testing "match can handle ranges")
  (is (r-eq #"[1-7]" (pre [:match [[1 :to 7]]])))
  (is (r-eq #"[1-7]{4,}" (pre [:match [[1 :to 7] :atleast 4]])))

  (testing "match with anchors")
  (is (r-eq #"^a" (pre [:match ["a" :when :at-start]])))
  (is (r-eq #"a$" (pre [:match ["a" :when :at-end]])))
  (is (r-eq #"^ab$" (pre [:match ["a" :when :at-start "b" :when :at-end]])))
  (is (r-eq #"^ab$^\d" (pre [:match ["a" :when :at-start "b" :when :at-end :digits :when :at-start]])))

  (testing "match with quantifiers"
    (is (r-eq #"a{5}" (pre [:match ["a" :exactly 5 :times]])))
    (is (r-eq #"a{5,}" (pre [:match ["a" :atleast 5 :times]])))
    (is (r-eq #"a{0,5}" (pre [:match ["a" :atmost 5 :times]])))
    (is (r-eq #"a{5,}b" (pre [:match ["a" :atleast 5 :times "b"]])))
    (is (r-eq #"a{0,5}b" (pre [:match ["a" :atmost 5 :times "b"]])))
    (is (r-eq #"a{2,5}" (pre [:match ["a" :between 2 :and 5 :times]]))))

  (testing "match with :or in char class vectors")
  (is (r-eq #"ab|c" (pre [:match ["a" "b" :or "c"]])))
  (is (r-eq #"a|b|c" (pre [:match ["a" :or "b" :or "c"]])))

  (testing "match with quantifiers and anchors")
  (is (r-eq #"^a{0,5}" (pre [:match ["a" :atmost 5 :times :when :at-start]])))
  (is (r-eq #"a{0,5}^b\d" (pre [:match ["a" :atmost 5 :times "b" :when :at-start :digits]])))

  (testing "match with :lazily in char class vector")
  (is (r-eq #"x*?" (pre [:match ["x" :lazily]])))
  (is (r-eq #"x+?" (pre [:match ["x" :lazily-1]])))
  (is (r-eq #"[abcx]*?" (pre [:match [["a" "b" "c" "x"]] :lazily])))

  (testing "match with :greedily in char class vector")
  (is (r-eq #"x*" (pre [:match ["x" :greedily]])))
  (is (r-eq #"x+" (pre [:match ["x" :greedily-1]])))
  (is (r-eq #"[abcx]*" (pre [:match [["a" "b" "c" "x"]] :greedily])))

  (testing "match with :possesively in char class vector")
  (is (r-eq #"x*+" (pre [:match ["x" :possessively]])))
  (is (r-eq #"[abcx]*+" (pre [:match [["a" "b" "c" "x"]] :possessively])))

  (testing "match with group constructs")
  (is (r-eq #"x(?=y)" (pre [:match "x" :before "y"])))
  (is (r-eq #"x(?!y)" (pre [:match "x" :not-before "y"])))
  (is (r-eq #"(?<=y)x" (pre [:match "x" :after "y"])))
  (is (r-eq #"(?<!y)x" (pre [:match "x" :not-after "y"])))
  (is (r-eq #"(?<=x)y(?=z)" (pre [:match "y" :between "x" :and "z"])))
  (is (r-eq #"(?<!x)y(?=z)" (pre [:match "y" :between :not "x" :and "z"])))
  (is (r-eq #"(?<=x)y(?!z)" (pre [:match "y" :between "x" :and :not "z"])))
  (is (r-eq #"(?<!x)y(?!z)" (pre [:match "y" :between :not "x" :and :not "z"])))

  (testing "match with group constructs with non-string characters")
  (is (r-eq #"(?<=\d)y(?=z)" (pre [:match "y" :between [:digits] :and "z"])))
  (is (r-eq #"(?<=x)y(?=z)" (pre [:match "y" :between ["x"] :and ["z"]])))
  (is (r-eq #"(?<=\d)y(?=z)" (pre [:match "y" :between :digits :and ["z"]])))
  (is (r-eq #"(?<=\d)y(?=z)" (pre [:match "y" :between [:digits] :and ["z"]])))

  (testing "match with anchors in mods")
  (is (r-eq #"^xy" (pre [:match ["xy"] :when :at-start])))
  (is (r-eq #"xy$" (pre [:match ["xy"] :when :at-end])))
  (is (r-eq #"xy$" (pre [:match ["x" "y"] :when :at-end])))

  (testing "match with quantifiers in mods")
  (is (r-eq #"xy{3,}" (pre [:match ["x" "y"] :atleast 3])))
  (is (r-eq #"xy{0,5}" (pre [:match ["x" "y"] :atmost 5])))
  (is (r-eq #"xy{2,5}" (pre [:match ["x" "y"] :between 2 :and 5])))
  (is (r-eq #"a{1,2}" (pre [:match ["a"] :between 1 :and 2])))
  (is (r-eq #"[A-Z]{1,2}" (pre [:match :upper :between 1 :and 2])))
  (is (r-eq #"^[A-Z]{1,2}" (pre [:match :upper :when :at-start :between 1 :and 2])))
  (is (r-eq #"(?<=<)[\s\S]*?(?=>)" (pre [:match [:everything] :lazily :between "<" :and ">"]))))

(deftest capture
  (is (r-eq #"(x)" (pre [:capture "x"])))
  (is (r-eq #"(x)" (pre [:capture ["x"]])))
  (is (r-eq #"([xy])" (pre [:capture [["x" "y"]]])))
  (is (r-eq #"(xy)" (pre [:capture ["x" "y"]])))
  (is (r-eq #"(^x)" (pre [:capture ["x" :when :at-start]])))
  (is (r-eq #"^(x)" (pre [:capture ["x"] :when :at-start])))

  (is (r-eq #"(?<=x)(y)(?=z)" (pre [:capture "y" :between "x" :and "z"])))
  (is (r-eq #"(?<=x)(yb)(?=z)" (pre [:capture ["y" "b"] :between "x" :and "z"])))
  (is (r-eq #"(?<=x)([yb])(?=z)" (pre [:capture [["y" "b"]] :between "x" :and "z"])))

  (is (r-eq #"(^ab$^\d)" (pre [:capture ["a" :when :at-start "b" :when :at-end :digits :when :at-start]])))
  (is (r-eq #"([^ab$^\d])" (pre [:capture [["a" :when :at-start "b" :when :at-end :digits :when :at-start]]]))))


(deftest match-enclosed
  (is (r-eq #"(?:xy)" (pre [:match-enc ["x" "y"]])))
  (is (r-eq #"(?:[xy])" (pre [:match-enc [["x" "y"]]])))

  (testing "match becomes match-enclosed if quantifiers are used inside and outside the character class vector")
  (is (r-eq #"(?:x{5,}y){2,}" (pre [:match ["x" :atleast 5 "y"] :atleast 2])))
  (is (r-eq #"(?:[x{5,}y]){2,}" (pre [:match [["x" :atleast 5 "y"]] :atleast 2]))))


(deftest set-constructs
  (testing "negation in sets")
  (is (r-eq #"[^abc]" (pre [:match #{:not "abc"}])))
  (is (r-eq #"[^abc]" (pre [:match #{:not "a" "b" "c"}])))
  (is (r-eq #"[^\da]" (pre [:match #{:not "a" :digits}])))  ; \d before a because sets change the order

  (testing "intersection in sets")
  (is (r-eq #"[123&&abc]" (pre [:match #{:and "123" "abc"}])))

  (testing "intersection and negation in sets")
  (is (r-eq #"[[^ab\d]&&abc]" (pre [:match #{:and "abc" #{:not "ab" :digits}}]))))

(deftest recipes
  (is (r-eq #"^\S+@\S+$" (pre [:m [:!spaces :greedily-1 :when :at-start
                                   "@" :!spaces :greedily-1 :when :at-end]])))

  (is (r-eq #"^([0-9]{4,})-(1[0-2]|0[1-9])"
            (pre [:c [[0 :to 9] :atleast 4] :when :at-start]
                 "-"
                 [:c [1 [0 :to 2] :or 0 [1 :to 9]]])))

  ;; expected failure
  ;; (is (r-eq #"^([0-9]{4,})-W(5[0-3]|[1-4][0-9]|0[1-9])$"
            ;; (pre [:c [[0 :to 9] :atleast 4] :when :at-start]
                ;;  "-W"
                ;;  [:c [5 [0 :to 3] :or [1 :to 4] [0 :to 9] :or 0 [1 :to 9]] :when :at-end])))

  (is (r-eq #"^[A-Z0-9+_.-]+@[A-Z0-9.-]+$"
            (pre [:m [[["A" :to "Z"] [0 :to 9] "+_.-"]] :greedily-1 :when :at-start]
                 "@"
                 [:m [[["A" :to "Z"] [0 :to 9] ".-"]] :greedily-1 :when :at-end])))

  (is (r-eq #"^[A-Z]{1,2}[0-9R][0-9A-Z]?[0-9][ABD-HJLNP-UW-Z]{2,}$"
            (pre [:m :upper :between 1 :and 2 :when :at-start]
                 [:m [[[0 :to 9] "R"]]]
                 [:m [[[0 :to 9] :upper]] :0-or-1]
                 [:m [[[0 :to 9]]]]
                 [:m [["AB" ["D" :to "H"] "JLN" ["P" :to "U"] ["W" :to "Z"]]]
                  :atleast 2 :when :at-end]))))


(deftest test-pre-can-handle-recursively-defined-regexes
  (let [digits-4 [:match [:digits :exactly 4 :times]]
        alphabets-5 [:match [:alpha :exactly 5 :times]]]
    (is (r-eq #"\d{4}[a-zA-Z]{5}"
              (pre [:match [digits-4 alphabets-5]])))
    (is (r-eq #"(\d{4}[a-zA-Z]{5})"
              (pre [:capture [digits-4 alphabets-5]])))))
