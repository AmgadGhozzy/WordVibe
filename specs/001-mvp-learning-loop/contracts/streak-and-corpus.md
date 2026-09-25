# Contract: Streak Table and Corpus Acceptance

## Streak (frozen table)

| State                                              | Streak shown | Freeze consumed | History / XP written |
|----------------------------------------------------|--------------|-----------------|----------------------|
| Active today, chain unbroken                       | Chain length | None            | Normal               |
| Viewing today, no activity, chain ends yesterday   | Length held  | None            | Nothing new          |
| Active today, yesterday missed, freeze available   | Preserved    | Exactly one     | Gap day absent       |
| Active today, yesterday missed, no freeze          | Zero         | —               | Gap day absent       |
| Gap of two or more days                            | Zero         | Never           | Gap days absent      |

A freeze fabricates nothing: no event, no XP, no studied day, no history, no dashboard
effect. Milestone bonuses fire on exact milestones only.

## Corpus acceptance (per word, human-reviewed and signed)

source/reference, CEFR (one of six), IELTS topic (closed 10-topic list), relevance
rationale (non-empty sentence), Arabic meaning (non-empty), English definition
(standalone-comprehensible per reviewer judgement, never a contains-word regex), example
(uses a valid inflection/derived form where appropriate), collocations ≥ 1, synonyms ≥ 1,
word family, recall target (gapped sentence), production target (explicit expectedForm +
acceptedVariants, never inferred), reviewer + date sign-off. Linguistic correctness alone
fails review.
