package xin.vanilla.narcissus.config.access;

import xin.vanilla.banira.common.config.ConfigCategoryViewProxy;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCardType;
import xin.vanilla.narcissus.enums.EnumCoolDownType;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

/**
 * {@link CommonConfig} 运行时 {@link CommonConfig.RootView} 实现。
 */
public final class CommonConfigAccess {

    private static final CommonConfig.OtherCategory DEFAULT_OTHER = new CommonConfig.OtherCategory();
    private static final CommonConfig.TeleportCardCategory DEFAULT_TELEPORT_CARD = new CommonConfig.TeleportCardCategory();
    private static final CommonConfig.TeleportLimitCategory DEFAULT_TELEPORT_LIMIT = new CommonConfig.TeleportLimitCategory();
    private static final CommonConfig.TeleportTogetherCategory DEFAULT_TELEPORT_TOGETHER = new CommonConfig.TeleportTogetherCategory();
    private static final CommonConfig.TeleportRequestCategory DEFAULT_TELEPORT_REQUEST = new CommonConfig.TeleportRequestCategory();
    private static final CommonConfig.SafeTeleportCategory DEFAULT_SAFE = new CommonConfig.SafeTeleportCategory();
    private static final CommonConfig.RandomTeleportCategory DEFAULT_RANDOM_TELEPORT = new CommonConfig.RandomTeleportCategory();
    private static final CommonConfig.CreativeFlightCategory DEFAULT_CREATIVE_FLIGHT = new CommonConfig.CreativeFlightCategory();
    private static final CommonConfig.FeatureSwitchCategory DEFAULT_FS = new CommonConfig.FeatureSwitchCategory();
    private static final CommonConfig.CommandCategory DEFAULT_CN = new CommonConfig.CommandCategory();
    private static final CommonConfig.CommandTpAskNames DEFAULT_CN_TP_ASK = new CommonConfig.CommandTpAskNames();
    private static final CommonConfig.CommandTpHereNames DEFAULT_CN_TP_HERE = new CommonConfig.CommandTpHereNames();
    private static final CommonConfig.CommandTpHomeNames DEFAULT_CN_TP_HOME = new CommonConfig.CommandTpHomeNames();
    private static final CommonConfig.CommandTpStageNames DEFAULT_CN_TP_STAGE = new CommonConfig.CommandTpStageNames();
    private static final CommonConfig.ConciseCategory DEFAULT_CC = new CommonConfig.ConciseCategory();
    private static final CommonConfig.ConciseTpAskNames DEFAULT_CC_TP_ASK = new CommonConfig.ConciseTpAskNames();
    private static final CommonConfig.ConciseTpHereNames DEFAULT_CC_TP_HERE = new CommonConfig.ConciseTpHereNames();
    private static final CommonConfig.ConciseTpHomeNames DEFAULT_CC_TP_HOME = new CommonConfig.ConciseTpHomeNames();
    private static final CommonConfig.ConciseTpStageNames DEFAULT_CC_TP_STAGE = new CommonConfig.ConciseTpStageNames();
    private static final CommonConfig.PermissionCommandCategory DEFAULT_PERM_CMD = new CommonConfig.PermissionCommandCategory();
    private static final CommonConfig.PermissionAcrossCategory DEFAULT_PERM_ACROSS = new CommonConfig.PermissionAcrossCategory();
    private static final CommonConfig.CooldownCategory DEFAULT_CD = new CommonConfig.CooldownCategory();
    private static final CommonConfig.TeleportCountdownCategory DEFAULT_TELEPORT_COUNTDOWN = new CommonConfig.TeleportCountdownCategory();
    private static final CommonConfig.ServerPerTypeTeleportCountdownGroup DEFAULT_SERVER_PER_TYPE_TPCD = new CommonConfig.ServerPerTypeTeleportCountdownGroup();
    private static final CommonConfig.TpCoordinateCostGroup DEFAULT_COST_TP_COORDINATE = new CommonConfig.TpCoordinateCostGroup();
    private static final CommonConfig.TpStructureCostGroup DEFAULT_COST_TP_STRUCTURE = new CommonConfig.TpStructureCostGroup();
    private static final CommonConfig.TpAskCostGroup DEFAULT_COST_TP_ASK = new CommonConfig.TpAskCostGroup();
    private static final CommonConfig.TpHereCostGroup DEFAULT_COST_TP_HERE = new CommonConfig.TpHereCostGroup();
    private static final CommonConfig.TpRandomCostGroup DEFAULT_COST_TP_RANDOM = new CommonConfig.TpRandomCostGroup();
    private static final CommonConfig.TpSpawnCostGroup DEFAULT_COST_TP_SPAWN = new CommonConfig.TpSpawnCostGroup();
    private static final CommonConfig.TpWorldSpawnCostGroup DEFAULT_COST_TP_WORLD_SPAWN = new CommonConfig.TpWorldSpawnCostGroup();
    private static final CommonConfig.TpTopCostGroup DEFAULT_COST_TP_TOP = new CommonConfig.TpTopCostGroup();
    private static final CommonConfig.TpBottomCostGroup DEFAULT_COST_TP_BOTTOM = new CommonConfig.TpBottomCostGroup();
    private static final CommonConfig.TpUpCostGroup DEFAULT_COST_TP_UP = new CommonConfig.TpUpCostGroup();
    private static final CommonConfig.TpDownCostGroup DEFAULT_COST_TP_DOWN = new CommonConfig.TpDownCostGroup();
    private static final CommonConfig.TpViewCostGroup DEFAULT_COST_TP_VIEW = new CommonConfig.TpViewCostGroup();
    private static final CommonConfig.TpHomeCostGroup DEFAULT_COST_TP_HOME = new CommonConfig.TpHomeCostGroup();
    private static final CommonConfig.TpStageCostGroup DEFAULT_COST_TP_STAGE = new CommonConfig.TpStageCostGroup();
    private static final CommonConfig.TpBackCostGroup DEFAULT_COST_TP_BACK = new CommonConfig.TpBackCostGroup();
    private static final CommonConfig.TpGraveCostGroup DEFAULT_COST_TP_GRAVE = new CommonConfig.TpGraveCostGroup();

    private CommonConfigAccess() {
    }

    public static CommonConfig.RootView root(ConfigHolder holder) {
        return (CommonConfig.RootView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.RootView.class},
                (proxy, method, args) -> rootHandle(proxy, method, args, holder));
    }

    private static Object rootHandle(Object proxy, Method method, Object[] args, ConfigHolder holder) {
        if (method.getDeclaringClass() == Object.class) {
            return objectMethod(proxy, method, args, "CommonConfig.RootView");
        }
        switch (method.getName()) {
            case "base":
                return base(holder);
            case "featureSwitch":
                return ConfigCategoryViewProxy.create(CommonConfig.FeatureSwitchView.class, holder, "featureSwitch", DEFAULT_FS,
                        CommonConfigAccess::readFeatureSwitch);
            case "command":
                return command(holder);
            case "concise":
                return concise(holder);
            case "permission":
                return permission(holder);
            case "cooldown":
                return ConfigCategoryViewProxy.create(CommonConfig.CooldownView.class, holder, "cooldown", DEFAULT_CD,
                        CommonConfigAccess::readCooldownInt);
            case "teleportCountdown":
                return teleportCountdown(holder);
            case "cost":
                return cost(holder);
            case "holder":
                return holder;
            case "save":
                if (holder != null) {
                    holder.save();
                }
                return null;
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }

    private static CommonConfig.BaseView base(ConfigHolder holder) {
        return (CommonConfig.BaseView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.BaseView.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args, "BaseView");
                    }
                    if (method.getParameterCount() != 0) {
                        throw new UnsupportedOperationException(method.toString());
                    }
                    switch (method.getName()) {
                        case "other":
                            return ConfigCategoryViewProxy.create(CommonConfig.OtherView.class, holder,
                                    "base.other", DEFAULT_OTHER, CommonConfigAccess::readGeneralLeaf);
                        case "teleportCard":
                            return ConfigCategoryViewProxy.create(CommonConfig.TeleportCardView.class, holder,
                                    "base.teleportCard.teleportCard", DEFAULT_TELEPORT_CARD, CommonConfigAccess::readBase);
                        case "teleportLimit":
                            return ConfigCategoryViewProxy.create(CommonConfig.TeleportLimitView.class, holder,
                                    "base.teleportLimit", DEFAULT_TELEPORT_LIMIT, CommonConfigAccess::readGeneralLeaf);
                        case "teleportTogether":
                            return ConfigCategoryViewProxy.create(CommonConfig.TeleportTogetherView.class, holder,
                                    "base.teleportTogether", DEFAULT_TELEPORT_TOGETHER, CommonConfigAccess::readGeneralLeaf);
                        case "teleportRequest":
                            return ConfigCategoryViewProxy.create(CommonConfig.TeleportRequestView.class, holder,
                                    "base.teleportRequest", DEFAULT_TELEPORT_REQUEST, CommonConfigAccess::readGeneralLeaf);
                        case "safeTeleport":
                            return ConfigCategoryViewProxy.create(CommonConfig.SafeTeleportView.class, holder,
                                    "base.safeTeleport", DEFAULT_SAFE, CommonConfigAccess::readSafeTeleport);
                        case "randomTeleport":
                            return ConfigCategoryViewProxy.create(CommonConfig.RandomTeleportView.class, holder,
                                    "base.randomTeleport", DEFAULT_RANDOM_TELEPORT, CommonConfigAccess::readGeneralLeaf);
                        case "creativeFlight":
                            return ConfigCategoryViewProxy.create(CommonConfig.CreativeFlightView.class, holder,
                                    "base.creativeFlight", DEFAULT_CREATIVE_FLIGHT, CommonConfigAccess::readGeneralLeaf);
                        default:
                            throw new UnsupportedOperationException(method.toString());
                    }
                });
    }

    private static CommonConfig.CommandNamesView command(ConfigHolder holder) {
        return (CommonConfig.CommandNamesView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.CommandNamesView.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args, "CommandNamesView");
                    }
                    switch (method.getName()) {
                        case "tpAsk":
                            return ConfigCategoryViewProxy.create(CommonConfig.CommandTpAskView.class, holder, "command.tpAsk",
                                    DEFAULT_CN_TP_ASK, CommonConfigAccess::readCommandStringLeaf);
                        case "tpHere":
                            return ConfigCategoryViewProxy.create(CommonConfig.CommandTpHereView.class, holder, "command.tpHere",
                                    DEFAULT_CN_TP_HERE, CommonConfigAccess::readCommandStringLeaf);
                        case "tpHome":
                            return ConfigCategoryViewProxy.create(CommonConfig.CommandTpHomeView.class, holder, "command.tpHome",
                                    DEFAULT_CN_TP_HOME, CommonConfigAccess::readCommandStringLeaf);
                        case "tpStage":
                            return ConfigCategoryViewProxy.create(CommonConfig.CommandTpStageView.class, holder, "command.tpStage",
                                    DEFAULT_CN_TP_STAGE, CommonConfigAccess::readCommandStringLeaf);
                        default:
                            return commandLeaf(holder, proxy, method, args);
                    }
                });
    }

    private static Object commandLeaf(ConfigHolder holder, Object proxy, Method method, Object[] args) {
        String leaf = method.getName();
        int pc = method.getParameterCount();
        String pathPrefix = commandPathPrefix(leaf);
        String fullPath = pathPrefix + "." + leaf;
        Object defBean = defaultBeanForCommandLeaf(leaf);
        if (pc == 0) {
            Object raw = holder != null ? holder.get(fullPath) : null;
            try {
                if ("command".equals(pathPrefix) && "commandPrefix".equals(leaf)) {
                    return readCommandNamesTop(leaf, raw, DEFAULT_CN);
                }
                return readCommandStringLeaf(leaf, raw, defBean);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (pc == 1) {
            if (holder != null) {
                holder.set(fullPath, args[0]);
            }
            return proxy;
        }
        throw new UnsupportedOperationException(method.toString());
    }

    private static String commandPathPrefix(String leaf) {
        switch (leaf) {
            case "commandTpAsk":
            case "commandTpAskYes":
            case "commandTpAskNo":
            case "commandTpAskCancel":
                return "command.tpAsk";
            case "commandTpHere":
            case "commandTpHereYes":
            case "commandTpHereNo":
            case "commandTpHereCancel":
                return "command.tpHere";
            case "commandTpHome":
            case "commandSetHome":
            case "commandDelHome":
            case "commandGetHome":
                return "command.tpHome";
            case "commandTpStage":
            case "commandSetStage":
            case "commandDelStage":
            case "commandGetStage":
                return "command.tpStage";
            default:
                return "command";
        }
    }

    private static Object defaultBeanForCommandLeaf(String leaf) {
        switch (leaf) {
            case "commandTpAsk":
            case "commandTpAskYes":
            case "commandTpAskNo":
            case "commandTpAskCancel":
                return DEFAULT_CN_TP_ASK;
            case "commandTpHere":
            case "commandTpHereYes":
            case "commandTpHereNo":
            case "commandTpHereCancel":
                return DEFAULT_CN_TP_HERE;
            case "commandTpHome":
            case "commandSetHome":
            case "commandDelHome":
            case "commandGetHome":
                return DEFAULT_CN_TP_HOME;
            case "commandTpStage":
            case "commandSetStage":
            case "commandDelStage":
            case "commandGetStage":
                return DEFAULT_CN_TP_STAGE;
            default:
                return DEFAULT_CN;
        }
    }

    private static CommonConfig.ConciseCommandsView concise(ConfigHolder holder) {
        return (CommonConfig.ConciseCommandsView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.ConciseCommandsView.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args, "ConciseCommandsView");
                    }
                    switch (method.getName()) {
                        case "tpAsk":
                            return ConfigCategoryViewProxy.create(CommonConfig.ConciseTpAskView.class, holder, "concise.tpAsk",
                                    DEFAULT_CC_TP_ASK, CommonConfigAccess::readFeatureSwitch);
                        case "tpHere":
                            return ConfigCategoryViewProxy.create(CommonConfig.ConciseTpHereView.class, holder, "concise.tpHere",
                                    DEFAULT_CC_TP_HERE, CommonConfigAccess::readFeatureSwitch);
                        case "tpHome":
                            return ConfigCategoryViewProxy.create(CommonConfig.ConciseTpHomeView.class, holder, "concise.tpHome",
                                    DEFAULT_CC_TP_HOME, CommonConfigAccess::readFeatureSwitch);
                        case "tpStage":
                            return ConfigCategoryViewProxy.create(CommonConfig.ConciseTpStageView.class, holder, "concise.tpStage",
                                    DEFAULT_CC_TP_STAGE, CommonConfigAccess::readFeatureSwitch);
                        default:
                            return conciseLeaf(holder, proxy, method, args);
                    }
                });
    }

    private static Object conciseLeaf(ConfigHolder holder, Object proxy, Method method, Object[] args) {
        String leaf = method.getName();
        int pc = method.getParameterCount();
        String pathPrefix = concisePathPrefix(leaf);
        String fullPath = pathPrefix + "." + leaf;
        Object defBean = defaultBeanForConciseLeaf(leaf);
        if (pc == 0) {
            Object raw = holder != null ? holder.get(fullPath) : null;
            try {
                return readFeatureSwitch(leaf, raw, defBean);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (pc == 1) {
            if (holder != null) {
                holder.set(fullPath, args[0]);
            }
            return proxy;
        }
        throw new UnsupportedOperationException(method.toString());
    }

    private static String concisePathPrefix(String leaf) {
        switch (leaf) {
            case "conciseTpAsk":
            case "conciseTpAskYes":
            case "conciseTpAskNo":
            case "conciseTpAskCancel":
                return "concise.tpAsk";
            case "conciseTpHere":
            case "conciseTpHereYes":
            case "conciseTpHereNo":
            case "conciseTpHereCancel":
                return "concise.tpHere";
            case "conciseTpHome":
            case "conciseSetHome":
            case "conciseDelHome":
            case "conciseGetHome":
                return "concise.tpHome";
            case "conciseTpStage":
            case "conciseSetStage":
            case "conciseDelStage":
            case "conciseGetStage":
                return "concise.tpStage";
            default:
                return "concise";
        }
    }

    private static Object defaultBeanForConciseLeaf(String leaf) {
        switch (leaf) {
            case "conciseTpAsk":
            case "conciseTpAskYes":
            case "conciseTpAskNo":
            case "conciseTpAskCancel":
                return DEFAULT_CC_TP_ASK;
            case "conciseTpHere":
            case "conciseTpHereYes":
            case "conciseTpHereNo":
            case "conciseTpHereCancel":
                return DEFAULT_CC_TP_HERE;
            case "conciseTpHome":
            case "conciseSetHome":
            case "conciseDelHome":
            case "conciseGetHome":
                return DEFAULT_CC_TP_HOME;
            case "conciseTpStage":
            case "conciseSetStage":
            case "conciseDelStage":
            case "conciseGetStage":
                return DEFAULT_CC_TP_STAGE;
            default:
                return DEFAULT_CC;
        }
    }

    private static CommonConfig.PermissionView permission(ConfigHolder holder) {
        return (CommonConfig.PermissionView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.PermissionView.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args, "PermissionView");
                    }
                    String name = method.getName();
                    String path = name.endsWith("AcrossDimension")
                            ? "permission.across." + name
                            : "permission.command." + name;
                    int pc = method.getParameterCount();
                    if (pc == 0) {
                        Object raw = holder != null ? holder.get(path) : null;
                        try {
                            return readPermissionInt(name, raw);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (pc == 1) {
                        if (holder != null) {
                            holder.set(path, args[0]);
                        }
                        return proxy;
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }

    private static CommonConfig.CostView cost(ConfigHolder holder) {
        return (CommonConfig.CostView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.CostView.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args, "CostView");
                    }
                    String n = method.getName();
                    if (method.getParameterCount() != 0) {
                        throw new UnsupportedOperationException(method.toString());
                    }
                    switch (n) {
                        case "tpCoordinate":
                            return costGroup(holder, "tpCoordinate", "costTpCoordinate", DEFAULT_COST_TP_COORDINATE);
                        case "tpStructure":
                            return costGroup(holder, "tpStructure", "costTpStructure", DEFAULT_COST_TP_STRUCTURE);
                        case "tpAsk":
                            return costGroup(holder, "tpAsk", "costTpAsk", DEFAULT_COST_TP_ASK);
                        case "tpHere":
                            return costGroup(holder, "tpHere", "costTpHere", DEFAULT_COST_TP_HERE);
                        case "tpRandom":
                            return costGroup(holder, "tpRandom", "costTpRandom", DEFAULT_COST_TP_RANDOM);
                        case "tpSpawn":
                            return costGroup(holder, "tpSpawn", "costTpSpawn", DEFAULT_COST_TP_SPAWN);
                        case "tpWorldSpawn":
                            return costGroup(holder, "tpWorldSpawn", "costTpWorldSpawn", DEFAULT_COST_TP_WORLD_SPAWN);
                        case "tpTop":
                            return costGroup(holder, "tpTop", "costTpTop", DEFAULT_COST_TP_TOP);
                        case "tpBottom":
                            return costGroup(holder, "tpBottom", "costTpBottom", DEFAULT_COST_TP_BOTTOM);
                        case "tpUp":
                            return costGroup(holder, "tpUp", "costTpUp", DEFAULT_COST_TP_UP);
                        case "tpDown":
                            return costGroup(holder, "tpDown", "costTpDown", DEFAULT_COST_TP_DOWN);
                        case "tpView":
                            return costGroup(holder, "tpView", "costTpView", DEFAULT_COST_TP_VIEW);
                        case "tpHome":
                            return costGroup(holder, "tpHome", "costTpHome", DEFAULT_COST_TP_HOME);
                        case "tpStage":
                            return costGroup(holder, "tpStage", "costTpStage", DEFAULT_COST_TP_STAGE);
                        case "tpBack":
                            return costGroup(holder, "tpBack", "costTpBack", DEFAULT_COST_TP_BACK);
                        case "tpGrave":
                            return costGroup(holder, "tpGrave", "costTpGrave", DEFAULT_COST_TP_GRAVE);
                        default:
                            throw new UnsupportedOperationException(method.toString());
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private static CommonConfig.TeleportCostGroupView costGroup(ConfigHolder holder, String subPath, String fieldStem, Object defaultsBean) {
        return (CommonConfig.TeleportCostGroupView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.TeleportCostGroupView.class},
                (proxy, method, args) -> costGroupHandle(holder, subPath, fieldStem, defaultsBean, proxy, method, args));
    }

    private static Object costGroupHandle(ConfigHolder holder, String subPath, String fieldStem, Object defaultsBean,
                                          Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            return objectMethod(proxy, method, args, "TeleportCostGroupView");
        }
        String mn = method.getName();
        String suffix;
        switch (mn) {
            case "type":
                suffix = "Type";
                break;
            case "num":
                suffix = "Num";
                break;
            case "conf":
                suffix = "Conf";
                break;
            case "rate":
                suffix = "Rate";
                break;
            case "numUpper":
                suffix = "NumUpper";
                break;
            case "numLower":
                suffix = "NumLower";
                break;
            case "exp":
                suffix = "Exp";
                break;
            default:
                throw new UnsupportedOperationException(method.toString());
        }
        String leaf = fieldStem + suffix;
        String fullPath = "cost." + subPath + "." + leaf;
        int pc = method.getParameterCount();
        if (pc == 0) {
            Object raw = holder != null ? holder.get(fullPath) : null;
            try {
                return readCostLeaf(suffix, raw, defaultsBean, leaf);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (pc == 1) {
            if (holder != null) {
                holder.set(fullPath, args[0]);
            }
            return proxy;
        }
        throw new UnsupportedOperationException(method.toString());
    }

    private static Object readCostLeaf(String suffix, Object raw, Object defaultsBean, String leaf) throws Exception {
        if ("Type".equals(suffix)) {
            EnumCostType def = (EnumCostType) field(defaultsBean, leaf);
            return parseCostType(raw, def);
        }
        if (raw == null) {
            return field(defaultsBean, leaf);
        }
        return raw;
    }

    private static EnumCostType parseCostType(Object v, EnumCostType def) {
        if (v instanceof EnumCostType) {
            return (EnumCostType) v;
        }
        if (v instanceof String) {
            try {
                return EnumCostType.valueOf((String) v);
            } catch (IllegalArgumentException ignored) {
                return def;
            }
        }
        return def;
    }

    private static Object readBase(String leaf, Object raw, Object bean) throws Exception {
        if ("teleportCardType".equals(leaf)) {
            if (raw == null) {
                return field(bean, leaf);
            }
            if (raw instanceof EnumCardType) {
                return raw;
            }
            if (raw instanceof String) {
                try {
                    return EnumCardType.valueOf((String) raw);
                } catch (IllegalArgumentException e) {
                    return field(bean, leaf);
                }
            }
            return field(bean, leaf);
        }
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static Object readFeatureSwitch(String leaf, Object raw, Object bean) throws Exception {
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static Object readCommandStringLeaf(String leaf, Object raw, Object bean) throws Exception {
        if (raw == null) {
            return field(bean, leaf);
        }
        String s = (String) raw;
        return s.isEmpty() ? field(bean, leaf) : s;
    }

    private static Object readCommandNamesTop(String leaf, Object raw, Object bean) throws Exception {
        if ("commandPrefix".equals(leaf)) {
            if (raw == null) {
                return field(bean, leaf);
            }
            String s = (String) raw;
            return s.isEmpty() ? NarcissusFarewell.DEFAULT_COMMAND_PREFIX : s;
        }
        return readCommandStringLeaf(leaf, raw, bean);
    }

    private static Object readCooldownInt(String leaf, Object raw, Object bean) throws Exception {
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static CommonConfig.TeleportCountdownView teleportCountdown(ConfigHolder holder) {
        return (CommonConfig.TeleportCountdownView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.TeleportCountdownView.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args, "TeleportCountdownView");
                    }
                    if ("server".equals(method.getName()) && method.getParameterCount() == 0) {
                        return ConfigCategoryViewProxy.create(CommonConfig.ServerPerTypeTeleportCountdownView.class, holder,
                                "teleportCountdown.server", DEFAULT_SERVER_PER_TYPE_TPCD, CommonConfigAccess::readCooldownInt);
                    }
                    return teleportCountdownLeaf(holder, proxy, method, args);
                });
    }

    private static Object teleportCountdownLeaf(ConfigHolder holder, Object proxy, Method method, Object[] args) {
        String leaf = method.getName();
        int pc = method.getParameterCount();
        String fullPath = "teleportCountdown." + leaf;
        if (pc == 0) {
            Object raw = holder != null ? holder.get(fullPath) : null;
            try {
                return readTeleportCountdownTopLevel(leaf, raw, DEFAULT_TELEPORT_COUNTDOWN);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (pc == 1) {
            if (holder != null) {
                holder.set(fullPath, args[0]);
            }
            return proxy;
        }
        throw new UnsupportedOperationException(method.toString());
    }

    private static Object readTeleportCountdownTopLevel(String leaf, Object raw, Object bean) throws Exception {
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static Object readGeneralLeaf(String leaf, Object raw, Object bean) throws Exception {
        if ("teleportRequestCooldownType".equals(leaf)) {
            if (raw == null) {
                return field(bean, leaf);
            }
            return parseCoolDownType(raw, (EnumCoolDownType) field(bean, leaf));
        }
        if ("tpSpawnNoBedWorldDimension".equals(leaf)) {
            if (raw == null) {
                return field(bean, leaf);
            }
            String s = (String) raw;
            return s.isEmpty() ? field(bean, leaf) : s;
        }
        if ("teleportBackSkipType".equals(leaf)) {
            if (raw == null) {
                return field(bean, leaf);
            }
            return raw;
        }
        if ("tpSound".equals(leaf)) {
            if (raw == null) {
                return field(bean, leaf);
            }
            String s = (String) raw;
            return s.isEmpty() ? field(bean, leaf) : s;
        }
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static EnumCoolDownType parseCoolDownType(Object v, EnumCoolDownType def) {
        if (v instanceof EnumCoolDownType) {
            return (EnumCoolDownType) v;
        }
        if (v instanceof String) {
            try {
                return EnumCoolDownType.valueOf((String) v);
            } catch (IllegalArgumentException ignored) {
                return def;
            }
        }
        return def;
    }

    private static Object readSafeTeleport(String leaf, Object raw, Object bean) throws Exception {
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static Object readPermissionInt(String methodName, Object raw) throws Exception {
        Object bean = methodName.endsWith("AcrossDimension") ? DEFAULT_PERM_ACROSS : DEFAULT_PERM_CMD;
        if (raw == null) {
            return field(bean, methodName);
        }
        return raw;
    }

    private static Object field(Object bean, String name) throws Exception {
        Field f = bean.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(bean);
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args, String tag) {
        String n = method.getName();
        switch (n) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return tag + "@" + System.identityHashCode(proxy);
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }

    private static void applyBaseThroughConcise(ConfigHolder h) {
        h.set("base.teleportCard.teleportCard", false);
        h.set("base.teleportCard.teleportCardDaily", 0);
        h.set("base.teleportCard.teleportCardType", EnumCardType.REFUND_ALL_COST);
        h.set("base.other.removeOriginalTp", false);
        h.set("base.creativeFlight.flySpeedMin", -5d);
        h.set("base.creativeFlight.flySpeedMax", 5d);
        h.set("featureSwitch.switchShare", true);
        h.set("featureSwitch.switchFeed", true);
        h.set("featureSwitch.switchTpCoordinate", true);
        h.set("featureSwitch.switchTpStructure", true);
        h.set("featureSwitch.switchTpAsk", true);
        h.set("featureSwitch.switchTpHere", true);
        h.set("featureSwitch.switchTpRandom", true);
        h.set("featureSwitch.switchTpSpawn", true);
        h.set("featureSwitch.switchTpWorldSpawn", true);
        h.set("featureSwitch.switchTpTop", true);
        h.set("featureSwitch.switchTpBottom", true);
        h.set("featureSwitch.switchTpUp", true);
        h.set("featureSwitch.switchTpDown", true);
        h.set("featureSwitch.switchTpView", true);
        h.set("featureSwitch.switchTpHome", true);
        h.set("featureSwitch.switchTpStage", true);
        h.set("featureSwitch.switchTpBack", true);
        h.set("featureSwitch.switchTpGrave", true);
        h.set("featureSwitch.switchFly", true);
        h.set("command.commandPrefix", NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        h.set("command.commandUuid", "uuid");
        h.set("command.commandDimension", "dim");
        h.set("command.commandCard", "card");
        h.set("command.commandShare", "share");
        h.set("command.commandFeed", "feed");
        h.set("command.commandTpCoordinate", "tpx");
        h.set("command.commandTpStructure", "tpst");
        h.set("command.tpAsk.commandTpAsk", "tpa");
        h.set("command.tpAsk.commandTpAskYes", "tpay");
        h.set("command.tpAsk.commandTpAskNo", "tpan");
        h.set("command.tpAsk.commandTpAskCancel", "tpac");
        h.set("command.tpHere.commandTpHere", "tph");
        h.set("command.tpHere.commandTpHereYes", "tphy");
        h.set("command.tpHere.commandTpHereNo", "tphn");
        h.set("command.tpHere.commandTpHereCancel", "tphc");
        h.set("command.commandTpRandom", "tpr");
        h.set("command.commandTpSpawn", "tpsp");
        h.set("command.commandTpWorldSpawn", "tpws");
        h.set("command.commandTpTop", "tpt");
        h.set("command.commandTpBottom", "tpb");
        h.set("command.commandTpUp", "tpu");
        h.set("command.commandTpDown", "tpd");
        h.set("command.commandTpView", "tpv");
        h.set("command.tpHome.commandTpHome", "home");
        h.set("command.tpHome.commandSetHome", "sethome");
        h.set("command.tpHome.commandDelHome", "delhome");
        h.set("command.tpHome.commandGetHome", "gethome");
        h.set("command.tpStage.commandTpStage", "stage");
        h.set("command.tpStage.commandSetStage", "setstage");
        h.set("command.tpStage.commandDelStage", "delstage");
        h.set("command.tpStage.commandGetStage", "getstage");
        h.set("command.commandTpBack", "back");
        h.set("command.commandTpGrave", "grave");
        h.set("command.commandFly", "fly");
        h.set("concise.conciseLanguage", false);
        h.set("concise.conciseUuid", false);
        h.set("concise.conciseDimension", false);
        h.set("concise.conciseCard", false);
        h.set("concise.conciseShare", false);
        h.set("concise.conciseFeed", false);
        h.set("concise.conciseTpCoordinate", true);
        h.set("concise.conciseTpStructure", true);
        h.set("concise.tpAsk.conciseTpAsk", true);
        h.set("concise.tpAsk.conciseTpAskYes", true);
        h.set("concise.tpAsk.conciseTpAskNo", true);
        h.set("concise.tpAsk.conciseTpAskCancel", true);
        h.set("concise.tpHere.conciseTpHere", true);
        h.set("concise.tpHere.conciseTpHereYes", true);
        h.set("concise.tpHere.conciseTpHereNo", true);
        h.set("concise.tpHere.conciseTpHereCancel", true);
        h.set("concise.conciseTpRandom", false);
        h.set("concise.conciseTpSpawn", true);
        h.set("concise.conciseTpWorldSpawn", false);
        h.set("concise.conciseTpTop", false);
        h.set("concise.conciseTpBottom", false);
        h.set("concise.conciseTpUp", false);
        h.set("concise.conciseTpDown", false);
        h.set("concise.conciseTpView", false);
        h.set("concise.tpHome.conciseTpHome", true);
        h.set("concise.tpHome.conciseSetHome", true);
        h.set("concise.tpHome.conciseDelHome", true);
        h.set("concise.tpHome.conciseGetHome", true);
        h.set("concise.tpStage.conciseTpStage", true);
        h.set("concise.tpStage.conciseSetStage", true);
        h.set("concise.tpStage.conciseDelStage", true);
        h.set("concise.tpStage.conciseGetStage", true);
        h.set("concise.conciseTpBack", true);
        h.set("concise.conciseTpGrave", true);
        h.set("concise.conciseFly", true);
        h.set("concise.conciseVirtualOp", false);
    }

    private static void applyBasePermissionCooldownCost(ConfigHolder h) {
        h.set("base.teleportLimit.teleportRecordLimit", 100);
        h.set("base.teleportLimit.teleportBackSkipType", new ArrayList<String>() {{
            add(EnumTeleportType.TP_BACK.name());
        }});
        h.set("base.teleportLimit.teleportAcrossDimension", true);
        h.set("base.teleportLimit.teleportCostDistanceLimit", 10000);
        h.set("base.teleportLimit.teleportCostDistanceAcrossDimension", 10000);
        h.set("base.teleportLimit.teleportViewDistanceLimit", 16 * 64);
        h.set("base.teleportRequest.teleportRequestExpireTime", 60);
        h.set("base.teleportRequest.teleportRequestCooldownType", EnumCoolDownType.INDIVIDUAL);
        h.set("base.teleportRequest.teleportRequestCooldown", 10);
        h.set("base.randomTeleport.teleportRandomDistanceLimit", 10000);
        h.set("base.randomTeleport.tpRandomSafeNotFoundRetries", 0);
        h.set("base.teleportLimit.graveSearchRangeLimit", 32);
        h.set("base.teleportLimit.teleportHomeLimit", 5);
        h.set("base.other.tpSound", DEFAULT_OTHER.tpSound());
        h.set("base.teleportTogether.tpWithVehicle", true);
        h.set("base.teleportTogether.tpWithFollower", true);
        h.set("base.teleportTogether.tpWithFollowerRange", 10);
        h.set("base.teleportTogether.tpWithEnemy", false);
        h.set("base.teleportLimit.tpSpawnNoBedWorldDimension", "minecraft:overworld");
        h.set("base.safeTeleport.unsafeBlocks", new ArrayList<>(DEFAULT_SAFE.unsafeBlocks()));
        h.set("base.safeTeleport.suffocatingBlocks", new ArrayList<>(DEFAULT_SAFE.suffocatingBlocks()));
        h.set("base.safeTeleport.setBlockWhenSafeNotFound", false);
        h.set("base.safeTeleport.getBlockFromInventory", true);
        h.set("base.safeTeleport.safeBlocks", new ArrayList<>(DEFAULT_SAFE.safeBlocks()));
        h.set("base.safeTeleport.safeChunkRange", 1);
        h.set("permission.command.permissionFeedOther", 2);
        h.set("permission.command.permissionTpCoordinate", 2);
        h.set("permission.command.permissionTpStructure", 2);
        h.set("permission.command.permissionTpAsk", 0);
        h.set("permission.command.permissionTpHere", 0);
        h.set("permission.command.permissionTpRandom", 1);
        h.set("permission.command.permissionTpSpawn", 0);
        h.set("permission.command.permissionTpSpawnOther", 2);
        h.set("permission.command.permissionTpWorldSpawn", 0);
        h.set("permission.command.permissionTpTop", 1);
        h.set("permission.command.permissionTpBottom", 1);
        h.set("permission.command.permissionTpUp", 1);
        h.set("permission.command.permissionTpDown", 1);
        h.set("permission.command.permissionTpView", 1);
        h.set("permission.command.permissionTpHome", 0);
        h.set("permission.command.permissionTpStage", 0);
        h.set("permission.command.permissionTpStageSet", 2);
        h.set("permission.command.permissionTpStageDel", 2);
        h.set("permission.command.permissionTpStageGet", 0);
        h.set("permission.command.permissionTpBack", 0);
        h.set("permission.command.permissionTpGrave", 0);
        h.set("permission.command.permissionFly", 2);
        h.set("permission.command.permissionVirtualOp", 4);
        h.set("permission.command.permissionSetCard", 2);
        h.set("permission.across.permissionTpCoordinateAcrossDimension", 2);
        h.set("permission.across.permissionTpStructureAcrossDimension", 2);
        h.set("permission.across.permissionTpAskAcrossDimension", 0);
        h.set("permission.across.permissionTpHereAcrossDimension", 0);
        h.set("permission.across.permissionTpRandomAcrossDimension", 0);
        h.set("permission.across.permissionTpSpawnAcrossDimension", 0);
        h.set("permission.across.permissionTpWorldSpawnAcrossDimension", 0);
        h.set("permission.across.permissionTpHomeAcrossDimension", 0);
        h.set("permission.across.permissionTpStageAcrossDimension", 0);
        h.set("permission.across.permissionTpBackAcrossDimension", 0);
        h.set("permission.across.permissionTpGraveAcrossDimension", 0);
        h.set("cooldown.cooldownTpCoordinate", 10);
        h.set("cooldown.cooldownTpStructure", 10);
        h.set("cooldown.cooldownTpAsk", 10);
        h.set("cooldown.cooldownTpHere", 10);
        h.set("cooldown.cooldownTpRandom", 10);
        h.set("cooldown.cooldownTpSpawn", 10);
        h.set("cooldown.cooldownTpWorldSpawn", 10);
        h.set("cooldown.cooldownTpTop", 10);
        h.set("cooldown.cooldownTpBottom", 10);
        h.set("cooldown.cooldownTpUp", 10);
        h.set("cooldown.cooldownTpDown", 10);
        h.set("cooldown.cooldownTpView", 10);
        h.set("cooldown.cooldownTpHome", 10);
        h.set("cooldown.cooldownTpStage", 10);
        h.set("cooldown.cooldownTpBack", 10);
        h.set("cooldown.cooldownTpGrave", 10);
        applyTeleportCountdownDefaults(h);
        applyCostGroupDefaults(h);
    }

    private static void applyTeleportCountdownDefaults(ConfigHolder h) {
        String p = "teleportCountdown.server.";
        h.set(p + "serverCountdownTpCoordinate", 0);
        h.set(p + "serverCountdownTpStructure", 0);
        h.set(p + "serverCountdownTpAsk", 0);
        h.set(p + "serverCountdownTpHere", 0);
        h.set(p + "serverCountdownTpRandom", 0);
        h.set(p + "serverCountdownTpSpawn", 0);
        h.set(p + "serverCountdownTpWorldSpawn", 0);
        h.set(p + "serverCountdownTpTop", 0);
        h.set(p + "serverCountdownTpBottom", 0);
        h.set(p + "serverCountdownTpUp", 0);
        h.set(p + "serverCountdownTpDown", 0);
        h.set(p + "serverCountdownTpView", 0);
        h.set(p + "serverCountdownTpHome", 0);
        h.set(p + "serverCountdownTpStage", 0);
        h.set(p + "serverCountdownTpBack", 0);
        h.set(p + "serverCountdownTpGrave", 0);
        h.set("teleportCountdown.forceServerCountdown", false);
        h.set("teleportCountdown.playerCountdownRangeMin", 0);
        h.set("teleportCountdown.playerCountdownRangeMax", 300);
        h.set("teleportCountdown.cancelCountdownOnPlayerMove", false);
        h.set("teleportCountdown.cancelCountdownOnPlayerDamage", false);
    }

    public static void resetConfig(ConfigHolder h) {
        if (h == null) {
            return;
        }
        applyBaseThroughConcise(h);
        applyBasePermissionCooldownCost(h);
        h.save();
    }

    public static void resetConfigWithMode1(ConfigHolder h) {
        if (h == null) {
            return;
        }
        applyBaseThroughConcise(h);
        applyBasePermissionCooldownCost(h);
        h.set("base.teleportLimit.teleportBackSkipType", new ArrayList<>());
        h.set("command.tpHome.commandTpHome", "home");
        h.set("command.tpHome.commandSetHome", "home_set");
        h.set("command.tpHome.commandDelHome", "home_del");
        h.set("command.tpHome.commandGetHome", "home_get");
        h.set("command.tpStage.commandTpStage", "warp");
        h.set("command.tpStage.commandSetStage", "warp_set");
        h.set("command.tpStage.commandDelStage", "warp_del");
        h.set("command.tpStage.commandGetStage", "warp_get");
        h.set("command.commandTpTop", "top");
        h.set("command.commandTpUp", "up");
        h.set("command.commandTpDown", "down");
        h.set("command.commandTpBottom", "bottom");
        h.save();
    }

    public static void resetConfigWithMode2(ConfigHolder h) {
        if (h == null) {
            return;
        }
        resetConfigWithMode1(h);
        h.set("featureSwitch.switchFeed", false);
        h.set("featureSwitch.switchTpStructure", false);
        h.set("featureSwitch.switchTpRandom", false);
        h.set("featureSwitch.switchTpSpawn", false);
        h.set("featureSwitch.switchTpWorldSpawn", false);
        h.set("featureSwitch.switchTpBottom", false);
        h.set("featureSwitch.switchTpDown", false);
        h.set("featureSwitch.switchTpUp", false);
        h.set("featureSwitch.switchTpView", false);
        h.save();
    }

    public static void resetConfigWithMode3(ConfigHolder h) {
        if (h == null) {
            return;
        }
        applyBaseThroughConcise(h);
        applyBasePermissionCooldownCost(h);
        h.set("concise.tpAsk.conciseTpAskCancel", false);
        h.set("concise.tpHere.conciseTpHereCancel", false);
        h.set("concise.conciseTpRandom", false);
        h.set("concise.conciseTpSpawn", false);
        h.set("concise.conciseTpWorldSpawn", false);
        h.set("concise.conciseTpTop", false);
        h.set("concise.conciseTpUp", false);
        h.set("concise.conciseTpBottom", false);
        h.set("concise.conciseTpDown", false);
        h.set("concise.conciseTpView", false);
        String[] subs = {"tpCoordinate", "tpStructure", "tpAsk", "tpHere", "tpRandom", "tpSpawn", "tpWorldSpawn",
                "tpTop", "tpBottom", "tpUp", "tpDown", "tpView", "tpHome", "tpStage", "tpBack", "tpGrave"};
        String[] stems = {"costTpCoordinate", "costTpStructure", "costTpAsk", "costTpHere", "costTpRandom", "costTpSpawn",
                "costTpWorldSpawn", "costTpTop", "costTpBottom", "costTpUp", "costTpDown", "costTpView", "costTpHome",
                "costTpStage", "costTpBack", "costTpGrave"};
        for (int i = 0; i < subs.length; i++) {
            h.set("cost." + subs[i] + "." + stems[i] + "Type", EnumCostType.EXP_POINT);
        }
        h.set("cost.tpGrave.costTpGraveType", EnumCostType.HUNGER);
        h.save();
    }

    private static void applyCostGroupDefaults(ConfigHolder h) {
        setCostGroupDefaults(h, "tpCoordinate", "costTpCoordinate");
        setCostGroupDefaults(h, "tpStructure", "costTpStructure");
        setCostGroupDefaults(h, "tpAsk", "costTpAsk");
        setCostGroupDefaults(h, "tpHere", "costTpHere");
        setCostGroupDefaults(h, "tpRandom", "costTpRandom");
        setCostGroupDefaults(h, "tpSpawn", "costTpSpawn");
        setCostGroupDefaults(h, "tpWorldSpawn", "costTpWorldSpawn");
        setCostGroupDefaults(h, "tpTop", "costTpTop");
        setCostGroupDefaults(h, "tpBottom", "costTpBottom");
        setCostGroupDefaults(h, "tpUp", "costTpUp");
        setCostGroupDefaults(h, "tpDown", "costTpDown");
        setCostGroupDefaults(h, "tpView", "costTpView");
        setCostGroupDefaults(h, "tpHome", "costTpHome");
        setCostGroupDefaults(h, "tpStage", "costTpStage");
        setCostGroupDefaults(h, "tpBack", "costTpBack");
        setCostGroupDefaults(h, "tpGrave", "costTpGrave");
    }

    private static void setCostGroupDefaults(ConfigHolder h, String sub, String stem) {
        h.set("cost." + sub + "." + stem + "Type", EnumCostType.NONE);
        h.set("cost." + sub + "." + stem + "Num", 1);
        h.set("cost." + sub + "." + stem + "Conf", "");
        h.set("cost." + sub + "." + stem + "Rate", 0.002);
        h.set("cost." + sub + "." + stem + "NumUpper", 20);
        h.set("cost." + sub + "." + stem + "NumLower", 0);
        h.set("cost." + sub + "." + stem + "Exp", "num * distance * rate");
    }
}
