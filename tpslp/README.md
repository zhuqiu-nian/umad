# TPSLP

TPSLP is a first prototype of Transformed Pivot-Space Linear Partitioning.

The subsystem keeps the first implementation deliberately small:

- in-memory exact range search;
- pivot-distance coordinate maps;
- pivot-space, log-distance, and power-distance transforms;
- linear slab regions;
- query-box pruning based on the triangle inequality.

Future modules such as density-aware envelopes, MBR/mBR refinements, and
expected-exclusion-power learners can plug into the current coordinate,
partition, and pruning interfaces.
