package xin.vanilla.narcissus.config;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.config.migration.CostConfigMigration;
import xin.vanilla.narcissus.data.cost.CostConfiguration;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.server.NarcissusCostRuntime;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Atomic business snapshots over the generated configuration view. */
public final class CommonCostConfiguration implements NarcissusCostRuntime.LiveConfiguration {
    private final ConfigHolder holder;
    private final Map<EnumTeleportType, CachedSelection> selections = new EnumMap<>(EnumTeleportType.class);
    public CommonCostConfiguration(ConfigHolder holder) { this.holder = Objects.requireNonNull(holder); }

    @Override public CostParameters parameters(EnumTeleportType type) {
        synchronized (holder) {
            CommonConfigView.CostView view = CommonConfig.get().cost();
            switch (type) {
                case TP_COORDINATE: {
                    CommonConfigView.CostView.CoordinateView g = view.coordinate();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_STRUCTURE: {
                    CommonConfigView.CostView.StructureView g = view.structure();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_ASK: {
                    CommonConfigView.CostView.AskView g = view.ask();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_HERE: {
                    CommonConfigView.CostView.HereView g = view.here();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_RANDOM: {
                    CommonConfigView.CostView.RandomView g = view.random();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_SPAWN: {
                    CommonConfigView.CostView.SpawnView g = view.spawn();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_WORLD_SPAWN: {
                    CommonConfigView.CostView.WorldSpawnView g = view.worldSpawn();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_TOP: {
                    CommonConfigView.CostView.TopView g = view.top();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_BOTTOM: {
                    CommonConfigView.CostView.BottomView g = view.bottom();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_UP: {
                    CommonConfigView.CostView.UpView g = view.up();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_DOWN: {
                    CommonConfigView.CostView.DownView g = view.down();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_VIEW: {
                    CommonConfigView.CostView.ViewView g = view.view();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_HOME: {
                    CommonConfigView.CostView.HomeView g = view.home();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_STAGE: {
                    CommonConfigView.CostView.StageView g = view.stage();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_BACK: {
                    CommonConfigView.CostView.BackView g = view.back();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                case TP_GRAVE: {
                    CommonConfigView.CostView.GraveView g = view.grave();
                    return new CostParameters(g.type(), g.fixedAmount(), g.perBlockAmount(), g.minAmount(),
                            g.maxAmount(), g.item(), g.command(), g.custom().file());
                }
                default: return CostParameters.free();
            }
        }
    }

    @Override public CostCardSettings cards() {
        synchronized (holder) {
            CommonConfigView.CostView.CardsView c = CommonConfig.get().cost().cards();
            return new CostCardSettings(c.enabled(), c.dailyGrant(), c.mode());
        }
    }
    @Override public int maxDistance() {
        synchronized (holder) { return CommonConfig.get().cost().distance().maxDistance(); }
    }
    @Override public int crossDimensionDistance() {
        synchronized (holder) { return CommonConfig.get().cost().distance().crossDimensionDistance(); }
    }
    @Override public CostConfiguration snapshot() {
        synchronized (holder) {
            requireBoundHolder();
            return NarcissusCostRuntime.LiveConfiguration.super.snapshot();
        }
    }
    @Override public CostConfiguration selection(EnumTeleportType type) {
        Objects.requireNonNull(type, "type");
        synchronized (holder) {
            requireBoundHolder();
            CachedSelection cached = selections.get(type);
            if (cached != null && cached.matches.getAsBoolean()) return cached.configuration;
            CostConfiguration configuration = NarcissusCostRuntime.LiveConfiguration.super.selection(type);
            selections.put(type, new CachedSelection(configuration, holder.prepareStoredMatch(expected(type, configuration), true)));
            return configuration;
        }
    }

    private void requireBoundHolder() {
        if (BaniraConfigs.handle(CommonConfig.class) != holder) {
            throw new IllegalStateException("Teleport cost holder has changed; restart the cost runtime");
        }
    }

    /** Freeze only expectations; every reuse still compares the actual unsaved backend values. */
    private static Map<String, Object> expected(EnumTeleportType type, CostConfiguration configuration) {
        Map<String, Object> values = new LinkedHashMap<>();
        CostParameters p = configuration.parameters(type);
        if (EnumTeleportType.countdownConfigurableTypes().contains(type)) {
            String prefix = "cost." + CostConfigMigration.groupName(type) + ".";
            values.put(prefix + "type", p.type());
            values.put(prefix + "fixedAmount", p.fixedAmount());
            values.put(prefix + "perBlockAmount", p.perBlockAmount());
            values.put(prefix + "minAmount", p.minAmount());
            values.put(prefix + "maxAmount", p.maxAmount());
            values.put(prefix + "item", p.item());
            values.put(prefix + "command", p.command());
            values.put(prefix + "custom.file", p.customFile());
        }
        if (p.type() != EnumCostType.NONE) {
            values.put("cost.cards.enabled", configuration.cards().enabled());
            values.put("cost.cards.dailyGrant", configuration.cards().dailyGrant());
            values.put("cost.cards.mode", configuration.cards().mode());
            values.put("cost.distance.maxDistance", configuration.maxDistance());
            values.put("cost.distance.crossDimensionDistance", configuration.crossDimensionDistance());
        }
        return values;
    }

    private static final class CachedSelection {
        final CostConfiguration configuration;
        final BooleanSupplier matches;
        CachedSelection(CostConfiguration configuration, BooleanSupplier matches) {
            this.configuration = configuration;
            this.matches = matches;
        }
    }
}
