package xin.vanilla.narcissus.api.cost;

/** Trusted server-local calculation; must not change game state. */
@FunctionalInterface
public interface CostFormula {
    double calculate(CostContext context);
}
