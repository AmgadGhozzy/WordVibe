# Contract: Quality Mapping and Scheduling

Frozen rows (exact numeric values freeze with their tests in 04c):

| Signal             | Quality             | Scheduler direction |
|--------------------|---------------------|---------------------|
| Recall success     | Positive            | Advance             |
| Recall failure     | Strong negative     | Sharp shorten       |
| Production success | Productive positive | Advance             |
| Production failure | Negative            | Real shorten        |

Rules: the two failures differ in Quality values, never in scheduler rules. Production
failure has a real effect — no minimum-shortening exception. QualityPolicy interprets
events; the Scheduler primitive never knows recall from spelling and stays deterministic
for identical inputs.

## Frozen numeric values (04c)

- Recall success: +1.2 stability per success.
- Production success: +2.0 stability per success.
- Lapse penalty in stability recompute: −1.5 per lapse.
- Recall failure: −2.0 stability (strong negative), difficulty +1, lapses +1.
- Production failure: −1.0 stability (negative), difficulty +1, lapses +1.
- Any real failure demotes MASTERED → KNOWN (history preserved).
- Change any value only by reopening the freeze (ADR + approval); golden tests in
  `QualityMappingContractTest` guard every row above.
