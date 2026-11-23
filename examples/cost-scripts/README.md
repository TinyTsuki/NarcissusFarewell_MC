# Teleport cost scripts

Place a trusted UTF-8 Java file in
`config/vanilla.xin/narcissus_farewell/scripts/` on the server. Use the package
`xin.vanilla.banira.generated.cost` for a file at the root, and give its public
no-argument class the same name as the file. Implement `CostFormula` and return
the amount from `calculate(CostContext context)` without modifying game state.

Set the relevant teleport group's fee type to a value other than `NONE` and
set `custom.file` to the relative path, e.g. `MyCost.java`. Restart or use
`/narcissus cost reload`. A formula supplies the amount; the configured fee type
still determines how payment is collected.

Existing paths remain valid, including formula helpers under `helpers/`.
The loader also looks in the old `cost/sources/` directory. A same-name file in
`scripts/` takes priority. The mod leaves old files in place.
