(ns luna.core-test
  (:require [clojure.test :refer :all]
            [luna.core :refer :all]))

(defn- r-eq
  "converts a and b to str and tests equality"
  [a b]
  (= (str a) (str b)))

(deftest plain-strings
  (testing "plain strings passed to pre render to regex.Patterns"
    (is (r-eq #"a" (pre "a")))
    (is (r-eq #"[1-9]" (pre "[1-9]")))
    (is (r-eq #"[$!@#$!@#$!@]" (pre "[$!@#$!@#$!@]")))))


(deftest regexes
  (testing "regexes passed to pre are returned as regexes")
  (is (r-eq #"^a" (pre #"^a")))
  (is (r-eq #"[1-9]?" (pre #"[1-9]?")))
  (is (r-eq #"!@#$!@#$!@#$!@#$" (pre #"!@#$!@#$!@#$!@#$"))))


(deftest pre-can-take-multiple-args
  (is (r-eq #"abc" (pre "a" "b" "c")))
  (is (r-eq #"abc" (pre #"a" #"b" #"c")))
  (is (r-eq #"abc" (pre [:match ["ab"]] "c"))))

(deftest match
  (testing "match with only char vecs")
  (is (r-eq #"a" (pre [:match ["a"]])))
  (is (r-eq #"ab" (pre [:match [["a" "b"]]])))
  (is (r-eq #"a|b" (pre [:match ["a" "b"]])))
  (is (r-eq #"a" (pre [:match "a"])))
  (is (r-eq #"\d" (pre [:match [:digits]])))

  (testing "match can handle ranges")
  (is (r-eq #"[1-7]" (pre [:match [[1 :to 7]]])))

  (testing "match with anchors")
  (is (r-eq #"^a" (pre [:match ["a" :when :at-start]])))
  (is (r-eq #"a$" (pre [:match ["a" :when :at-end]])))
  (is (r-eq #"^a|b$" (pre [:match ["a" :when :at-start "b" :when :at-end]])))
  (is (r-eq #"^a|b$|^\d" (pre [:match ["a" :when :at-start "b" :when :at-end :digits :when :at-start]])))

  (testing "match with quantifiers")
  (is (r-eq #"a{5,}" (pre [:match ["a" :atleast 5 :times]])))
  (is (r-eq #"a{0,5}" (pre [:match ["a" :atmost 5 :times]])))
  (is (r-eq #"a{5,}|b" (pre [:match ["a" :atleast 5 :times "b"]])))
  (is (r-eq #"a{0,5}|b" (pre [:match ["a" :atmost 5 :times "b"]])))
  (is (r-eq #"a{2,5}" (pre [:match ["a" :between 2 :and 5 :times]])))

  (testing "match with quantifiers and anchors")
  (is (r-eq #"^a{0,5}" (pre [:match ["a" :atmost 5 :times :when :at-start]])))
  (is (r-eq #"a{0,5}|^b|\d" (pre [:match ["a" :atmost 5 :times "b" :when :at-start :digits]])))

  (testing "match with :lazily in char class vector")
  (is (r-eq #"x*?" (pre [:match ["x" :lazily]])))
  (is (r-eq #"abcx*?" (pre [:match [["a" "b" "c" "x"]] :lazily])))

  (is (r-eq #"x?" (pre [:match ["x" :greedily]])))
  ;(is (r-eq #"abcx?" (pre [:match [["a" "b" "c" "x"]] :greedily])))

  ;(is (r-eq #"x*+" (pre [:match ["x" :possessively]])))
  ;(is (r-eq #"abcx*+" (pre [:match [["a" "b" "c" "x"]] :possessively])))

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
  (is (r-eq #"[xy]$" (pre [:match ["x" "y"] :when :at-end])))

  (testing "match with quantifiers in mods")
  (is (r-eq #"[xy]{3,}" (pre [:match ["x" "y"] :atleast 3])))
  (is (r-eq #"[xy]{0,5}" (pre [:match ["x" "y"] :atmost 5])))
  (is (r-eq #"[xy]{2,5}" (pre [:match ["x" "y"] :between 2 :and 5]))))


(deftest capture
  (is (r-eq #"(x)" (pre [:capture "x"])))
  (is (r-eq #"(x)" (pre [:capture ["x"]])))
  (is (r-eq #"(xy)" (pre [:capture [["x" "y"]]])))
  (is (r-eq #"(x|y)" (pre [:capture ["x" "y"]])))
  (is (r-eq #"(^x)" (pre [:capture ["x" :when :at-start]])))
  (is (r-eq #"^(x)" (pre [:capture ["x"] :when :at-start])))

  (is (r-eq #"(?<=x)(y)(?=z)" (pre [:capture "y" :between "x" :and "z"])))
  (is (r-eq #"(?<=x)(y|b)(?=z)" (pre [:capture ["y" "b"] :between "x" :and "z"])))
  (is (r-eq #"(?<=x)(yb)(?=z)" (pre [:capture [["y" "b"]] :between "x" :and "z"])))

  (is (r-eq #"(^a|b$|^\d)" (pre [:capture ["a" :when :at-start "b" :when :at-end :digits :when :at-start]])))
  (is (r-eq #"(^ab$^\d)" (pre [:capture [["a" :when :at-start "b" :when :at-end :digits :when :at-start]]]))))


(deftest match-enclosed
  (is (r-eq #"(?:x|y)" (pre [:match-enc ["x" "y"]])))
  (is (r-eq #"(?:xy)" (pre [:match-enc [["x" "y"]]])))

  (testing "match becomes match-enclosed if quantifiers are used inside and outside the character class vector")
  (is (r-eq #"(?:x{5,}|y){2,}" (pre [:match ["x" :atleast 5 "y"] :atleast 2])))
  (is (r-eq #"(?:x{5,}y){2,}" (pre [:match [["x" :atleast 5 "y"]] :atleast 2]))))


(deftest set-constructs
  (testing "negation in sets")
  (is (r-eq #"[^abc]" (pre [:match #{:not "abc"}])))
  (is (r-eq #"[^abc]" (pre [:match #{:not "a" "b" "c"}])))
  (is (r-eq #"[^\da]" (pre [:match #{:not "a" :digits}])))  ; \d before a because sets change the order

  (testing "intersection in sets")
  (is (r-eq #"[123&&abc]" (pre [:match #{:and "123" "abc"}])))

  (testing "intersection and negation in sets")
  (is (r-eq #"[[^ab\d]&&abc]" (pre [:match #{:and "abc" #{:not "ab" :digits}}]))))
