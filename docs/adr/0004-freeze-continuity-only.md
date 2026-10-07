# Streak freeze preserves continuity only

A consumed freeze keeps the streak counter across exactly one missed day but fabricates nothing: no LearningEvent, no XP, no studied day, no history, no dashboard effect. The alternative (counting the gap as studied) would corrupt the dashboard-reconciliation invariant, which projects actual learning events only.
