# M16G hotfix — deterministic scientific tick labels

Fixed the failing `NiceTickGeneratorTest.verySmallValuesUseDeterministicScientificNotation()` regression.

Cause: generated tick values are computed with binary `double` multiplication (`index * step`), so values such as `3 * 1e-8` can become `3.0000000000000004E-8`. The scientific formatter was formatting that binary artifact verbatim.

Fix: labels for regular generated ticks are now formatted from the decimal representation of the nice step multiplied by the integer tick index. Axis numeric values remain `double`; only their user-facing generated labels avoid binary floating-point noise. Endpoint fallback formatting remains unchanged, including subnormal values such as `4.9E-324`.

Expected full test baseline after this fix: 704 tests, 0 failures, 0 errors, 0 skipped.
