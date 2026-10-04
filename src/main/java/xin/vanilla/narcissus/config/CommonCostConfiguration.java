package xin.vanilla.narcissus.config;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.cost.CostConfiguration;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.server.NarcissusCostRuntime;

import java.util.Objects;

/** Atomic business snapshots over the generated configuration view. */
public final class CommonCostConfiguration implements NarcissusCostRuntime.LiveConfiguration {
    private final ConfigHolder holder;
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
        synchronized (holder) { return NarcissusCostRuntime.LiveConfiguration.super.snapshot(); }
    }
    @Override public CostConfiguration selection(EnumTeleportType type) {
        synchronized (holder) { return NarcissusCostRuntime.LiveConfiguration.super.selection(type); }
    }
}
