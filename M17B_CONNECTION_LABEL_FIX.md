# M17B Connection Label Placement Fix

## Bug
For a direct two-point connection, M17B used `path.get(path.size() / 2)` as the label anchor. With a two-point path this selects the target endpoint, so the connection label could be painted underneath the target node (for example, `signal` appeared only as `sig`).

## Fix
Connection labels are now anchored to the midpoint of a routed segment rather than to a path vertex. The layout chooses the longest segment deterministically, preferring a horizontal segment on ties, then centers the label on that segment.

This remains a pure-Java layout decision; the Minecraft renderer continues to consume positioned geometry only.

## Regression coverage
Added `horizontalConnectionLabelIsCenteredOnSegmentInsteadOfTargetEndpoint()` to verify the sample `Sensor -> Processor` geometry keeps the complete label centered in the free span between nodes.
