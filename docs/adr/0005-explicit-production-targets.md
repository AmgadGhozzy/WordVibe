# Explicit machine-checkable production targets

Every corpus word stores its production target as explicit expectedForm plus acceptedVariants in the schema; graders never infer the correct answer from word forms. Inferring looks simpler but makes spelling grading ambiguous the moment a word has multiple valid forms — by then the corpus is built and the ambiguity is load-bearing.
