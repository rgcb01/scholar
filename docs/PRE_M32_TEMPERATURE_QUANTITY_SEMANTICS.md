# Pre-M32 Temperature Quantity Semantics

## Scope

This M31 hardening step distinguishes thermodynamic temperatures from temperature differences before M32 computation. It does not add variables, expressions, evaluation, or computed document content.

## Semantic Model

Every `Quantity` has one immutable interpretation:

- `LINEAR` for ordinary multiplicative physical quantities;
- `ABSOLUTE_TEMPERATURE` for thermodynamic states such as `25 °C` or `298.15 K`;
- `TEMPERATURE_DIFFERENCE` for intervals such as `Δ5 °C` or `Δ5 K`.

Temperature interpretations are valid only for the exact thermodynamic-temperature dimension. That dimension may not use `LINEAR`; compound dimensions involving temperature, such as heat capacity, remain ordinary linear quantities.

Kelvin's zero offset does not make it semantically linear. Both absolute kelvin and delta kelvin are represented explicitly.

## Defaults And Compatibility

Existing constructors and Scholar JSON V2 values without a semantics field use this deterministic rule:

- exact thermodynamic-temperature dimension defaults to `ABSOLUTE_TEMPERATURE`;
- every other dimension defaults to `LINEAR`.

New JSON V2 writes persist semantics explicitly for quantities, dataset columns, and plot display units. This is an additive V2 extension; no Persistence V3 is introduced.

## Conversion And Measurement

Absolute temperatures use affine unit offsets. Temperature differences use scale only. Therefore `0 °C` is `273.15 K`, while `Δ1 °C` is `Δ1 K`.

A measured absolute temperature such as `25 ± 1 °C` contains an absolute nominal value and an implicitly differential uncertainty. Conversion produces `298.15 ± 1 K`; uncertainty propagation beyond unit conversion remains out of scope.

## Arithmetic Contract

- absolute minus absolute produces a difference;
- absolute plus/minus difference produces an absolute temperature;
- difference plus absolute produces an absolute temperature;
- difference plus/minus difference produces a difference;
- absolute plus absolute and difference minus absolute are invalid;
- multiplication, division, and integer powers involving an absolute temperature are invalid;
- differences behave linearly when combined with other dimensions.

`QuantityArithmeticPolicy` owns this contract so a future evaluator does not reproduce temperature-specific rules.

## Product Surface

The production `/scholar` ribbon exposes temperature-difference insertion and `Δ°C`/`ΔK` dataset-column and plot-axis choices. There is no development command or separate temperature tool.
