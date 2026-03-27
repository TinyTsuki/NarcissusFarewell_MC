package xin.vanilla.narcissus.config.access;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link CommonConfig} 运行时 {@link CommonConfig.RootView} 实现。
 */
public final class CommonConfigAccess {

    private static final CommonConfig.BaseCategory DEFAULT_BASE = new CommonConfig.BaseCategory();
    private static final CommonConfig.FeatureSwitchCategory DEFAULT_FS = new CommonConfig.FeatureSwitchCategory();
    private static final CommonConfig.CommandNamesCategory DEFAULT_CN = new CommonConfig.CommandNamesCategory();
    private static final CommonConfig.CommandTpAskNames DEFAULT_CN_TP_ASK = new CommonConfig.CommandTpAskNames();
    private static final CommonConfig.CommandTpHereNames DEFAULT_CN_TP_HERE = new CommonConfig.CommandTpHereNames();
    private static final CommonConfig.CommandTpHomeNames DEFAULT_CN_TP_HOME = new CommonConfig.CommandTpHomeNames();
    private static final CommonConfig.CommandTpStageNames DEFAULT_CN_TP_STAGE = new CommonConfig.CommandTpStageNames();
    private static final CommonConfig.ConciseCategory DEFAULT_CC = new CommonConfig.ConciseCategory();
    private static final CommonConfig.ConciseTpAskNames DEFAULT_CC_TP_ASK = new CommonConfig.ConciseTpAskNames();
    private static final CommonConfig.ConciseTpHereNames DEFAULT_CC_TP_HERE = new CommonConfig.ConciseTpHereNames();
    private static final CommonConfig.ConciseTpHomeNames DEFAULT_CC_TP_HOME = new CommonConfig.ConciseTpHomeNames();
    private static final CommonConfig.ConciseTpStageNames DEFAULT_CC_TP_STAGE = new CommonConfig.ConciseTpStageNames();
    private static final CommonConfig.GeneralCategory DEFAULT_GENERAL = new CommonConfig.GeneralCategory();
    private static final CommonConfig.SafeTeleportCategory DEFAULT_SAFE = new CommonConfig.SafeTeleportCategory();
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
                return ConfigCategoryViewProxy.create(CommonConfig.BaseView.class, holder, "base", DEFAULT_BASE,
                        CommonConfigAccess::readBase);
            case "featureSwitch":
                return ConfigCategoryViewProxy.create(CommonConfig.FeatureSwitchView.class, holder, "featureSwitch", DEFAULT_FS,
                        CommonConfigAccess::readFeatureSwitch);
            case "commandNames":
                return commandNames(holder);
            case "conciseCommands":
            case "concise":
                return conciseCommands(holder);
            case "general":
                return general(holder);
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

    private static CommonConfig.CommandNamesView commandNames(ConfigHolder holder) {
        return (CommonConfig.CommandNamesView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.CommandNamesView.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args, "CommandNamesView");
                    }
                    switch (method.getName()) {
                        case "tpAsk":
                            return ConfigCategoryViewProxy.create(CommonConfig.CommandTpAskView.class, holder, "commandNames.tpAsk",
                                    DEFAULT_CN_TP_ASK, CommonConfigAccess::readCommandStringLeaf);
                        case "tpHere":
                            return ConfigCategoryViewProxy.create(CommonConfig.CommandTpHereView.class, holder, "commandNames.tpHere",
                                    DEFAULT_CN_TP_HERE, CommonConfigAccess::readCommandStringLeaf);
                        case "tpHome":
                            return ConfigCategoryViewProxy.create(CommonConfig.CommandTpHomeView.class, holder, "commandNames.tpHome",
                                    DEFAULT_CN_TP_HOME, CommonConfigAccess::readCommandStringLeaf);
                        case "tpStage":
                            return ConfigCategoryViewProxy.create(CommonConfig.CommandTpStageView.class, holder, "commandNames.tpStage",
                                    DEFAULT_CN_TP_STAGE, CommonConfigAccess::readCommandStringLeaf);
                        default:
                            return commandNamesLeaf(holder, proxy, method, args);
                    }
                });
    }

    private static Object commandNamesLeaf(ConfigHolder holder, Object proxy, Method method, Object[] args) {
        String leaf = method.getName();
        int pc = method.getParameterCount();
        String pathPrefix = commandNamesPathPrefix(leaf);
        String fullPath = pathPrefix + "." + leaf;
        Object defBean = defaultBeanForCommandLeaf(leaf);
        if (pc == 0) {
            Object raw = holder != null ? holder.get(fullPath) : null;
            try {
                if ("commandNames".equals(pathPrefix) && "commandPrefix".equals(leaf)) {
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

    private static String commandNamesPathPrefix(String leaf) {
        switch (leaf) {
            case "commandTpAsk":
            case "commandTpAskYes":
            case "commandTpAskNo":
            case "commandTpAskCancel":
                return "commandNames.tpAsk";
            case "commandTpHere":
            case "commandTpHereYes":
            case "commandTpHereNo":
            case "commandTpHereCancel":
                return "commandNames.tpHere";
            case "commandTpHome":
            case "commandSetHome":
            case "commandDelHome":
            case "commandGetHome":
                return "commandNames.tpHome";
            case "commandTpStage":
            case "commandSetStage":
            case "commandDelStage":
            case "commandGetStage":
                return "commandNames.tpStage";
            default:
                return "commandNames";
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

    private static CommonConfig.ConciseCommandsView conciseCommands(ConfigHolder holder) {
        return (CommonConfig.ConciseCommandsView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.ConciseCommandsView.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args, "ConciseCommandsView");
                    }
                    switch (method.getName()) {
                        case "tpAsk":
                            return ConfigCategoryViewProxy.create(CommonConfig.ConciseTpAskView.class, holder, "conciseCommands.tpAsk",
                                    DEFAULT_CC_TP_ASK, CommonConfigAccess::readFeatureSwitch);
                        case "tpHere":
                            return ConfigCategoryViewProxy.create(CommonConfig.ConciseTpHereView.class, holder, "conciseCommands.tpHere",
                                    DEFAULT_CC_TP_HERE, CommonConfigAccess::readFeatureSwitch);
                        case "tpHome":
                            return ConfigCategoryViewProxy.create(CommonConfig.ConciseTpHomeView.class, holder, "conciseCommands.tpHome",
                                    DEFAULT_CC_TP_HOME, CommonConfigAccess::readFeatureSwitch);
                        case "tpStage":
                            return ConfigCategoryViewProxy.create(CommonConfig.ConciseTpStageView.class, holder, "conciseCommands.tpStage",
                                    DEFAULT_CC_TP_STAGE, CommonConfigAccess::readFeatureSwitch);
                        default:
                            return conciseCommandsLeaf(holder, proxy, method, args);
                    }
                });
    }

    private static Object conciseCommandsLeaf(ConfigHolder holder, Object proxy, Method method, Object[] args) {
        String leaf = method.getName();
        int pc = method.getParameterCount();
        String pathPrefix = conciseCommandsPathPrefix(leaf);
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

    private static String conciseCommandsPathPrefix(String leaf) {
        switch (leaf) {
            case "conciseTpAsk":
            case "conciseTpAskYes":
            case "conciseTpAskNo":
            case "conciseTpAskCancel":
                return "conciseCommands.tpAsk";
            case "conciseTpHere":
            case "conciseTpHereYes":
            case "conciseTpHereNo":
            case "conciseTpHereCancel":
                return "conciseCommands.tpHere";
            case "conciseTpHome":
            case "conciseSetHome":
            case "conciseDelHome":
            case "conciseGetHome":
                return "conciseCommands.tpHome";
            case "conciseTpStage":
            case "conciseSetStage":
            case "conciseDelStage":
            case "conciseGetStage":
                return "conciseCommands.tpStage";
            default:
                return "conciseCommands";
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

    private static CommonConfig.GeneralView general(ConfigHolder holder) {
        return (CommonConfig.GeneralView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.GeneralView.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args, "GeneralView");
                    }
                    if ("safeTeleport".equals(method.getName()) && method.getParameterCount() == 0) {
                        return ConfigCategoryViewProxy.create(CommonConfig.SafeTeleportView.class, holder, "general.safeTeleport",
                                DEFAULT_SAFE, CommonConfigAccess::readSafeTeleport);
                    }
                    return generalLeaf(holder, proxy, method, args);
                });
    }

    private static Object generalLeaf(ConfigHolder holder, Object proxy, Method method, Object[] args) {
        String leaf = method.getName();
        int pc = method.getParameterCount();
        if (pc == 0) {
            Object raw = holder != null ? holder.get("general." + leaf) : null;
            try {
                return readGeneralLeaf(leaf, raw, DEFAULT_GENERAL);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (pc == 1) {
            if (holder != null) {
                holder.set("general." + leaf, args[0]);
            }
            return proxy;
        }
        throw new UnsupportedOperationException(method.toString());
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
        if ("defaultLanguage".equals(leaf) || "helpHeader".equals(leaf) || "tpSound".equals(leaf)) {
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
        h.set("base.teleportCard", false);
        h.set("base.teleportCardDaily", 0);
        h.set("base.teleportCardType", EnumCardType.REFUND_ALL_COST);
        h.set("base.removeOriginalTp", false);
        h.set("base.flySpeedMin", -5d);
        h.set("base.flySpeedMax", 5d);
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
        h.set("commandNames.commandPrefix", NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        h.set("commandNames.commandUuid", "uuid");
        h.set("commandNames.commandDimension", "dim");
        h.set("commandNames.commandCard", "card");
        h.set("commandNames.commandShare", "share");
        h.set("commandNames.commandFeed", "feed");
        h.set("commandNames.commandTpCoordinate", "tpx");
        h.set("commandNames.commandTpStructure", "tpst");
        h.set("commandNames.tpAsk.commandTpAsk", "tpa");
        h.set("commandNames.tpAsk.commandTpAskYes", "tpay");
        h.set("commandNames.tpAsk.commandTpAskNo", "tpan");
        h.set("commandNames.tpAsk.commandTpAskCancel", "tpac");
        h.set("commandNames.tpHere.commandTpHere", "tph");
        h.set("commandNames.tpHere.commandTpHereYes", "tphy");
        h.set("commandNames.tpHere.commandTpHereNo", "tphn");
        h.set("commandNames.tpHere.commandTpHereCancel", "tphc");
        h.set("commandNames.commandTpRandom", "tpr");
        h.set("commandNames.commandTpSpawn", "tpsp");
        h.set("commandNames.commandTpWorldSpawn", "tpws");
        h.set("commandNames.commandTpTop", "tpt");
        h.set("commandNames.commandTpBottom", "tpb");
        h.set("commandNames.commandTpUp", "tpu");
        h.set("commandNames.commandTpDown", "tpd");
        h.set("commandNames.commandTpView", "tpv");
        h.set("commandNames.tpHome.commandTpHome", "home");
        h.set("commandNames.tpHome.commandSetHome", "sethome");
        h.set("commandNames.tpHome.commandDelHome", "delhome");
        h.set("commandNames.tpHome.commandGetHome", "gethome");
        h.set("commandNames.tpStage.commandTpStage", "stage");
        h.set("commandNames.tpStage.commandSetStage", "setstage");
        h.set("commandNames.tpStage.commandDelStage", "delstage");
        h.set("commandNames.tpStage.commandGetStage", "getstage");
        h.set("commandNames.commandTpBack", "back");
        h.set("commandNames.commandTpGrave", "grave");
        h.set("commandNames.commandFly", "fly");
        h.set("conciseCommands.conciseLanguage", false);
        h.set("conciseCommands.conciseUuid", false);
        h.set("conciseCommands.conciseDimension", false);
        h.set("conciseCommands.conciseCard", false);
        h.set("conciseCommands.conciseShare", false);
        h.set("conciseCommands.conciseFeed", false);
        h.set("conciseCommands.conciseTpCoordinate", true);
        h.set("conciseCommands.conciseTpStructure", true);
        h.set("conciseCommands.tpAsk.conciseTpAsk", true);
        h.set("conciseCommands.tpAsk.conciseTpAskYes", true);
        h.set("conciseCommands.tpAsk.conciseTpAskNo", true);
        h.set("conciseCommands.tpAsk.conciseTpAskCancel", true);
        h.set("conciseCommands.tpHere.conciseTpHere", true);
        h.set("conciseCommands.tpHere.conciseTpHereYes", true);
        h.set("conciseCommands.tpHere.conciseTpHereNo", true);
        h.set("conciseCommands.tpHere.conciseTpHereCancel", true);
        h.set("conciseCommands.conciseTpRandom", false);
        h.set("conciseCommands.conciseTpSpawn", true);
        h.set("conciseCommands.conciseTpWorldSpawn", false);
        h.set("conciseCommands.conciseTpTop", false);
        h.set("conciseCommands.conciseTpBottom", false);
        h.set("conciseCommands.conciseTpUp", false);
        h.set("conciseCommands.conciseTpDown", false);
        h.set("conciseCommands.conciseTpView", false);
        h.set("conciseCommands.tpHome.conciseTpHome", true);
        h.set("conciseCommands.tpHome.conciseSetHome", true);
        h.set("conciseCommands.tpHome.conciseDelHome", true);
        h.set("conciseCommands.tpHome.conciseGetHome", true);
        h.set("conciseCommands.tpStage.conciseTpStage", true);
        h.set("conciseCommands.tpStage.conciseSetStage", true);
        h.set("conciseCommands.tpStage.conciseDelStage", true);
        h.set("conciseCommands.tpStage.conciseGetStage", true);
        h.set("conciseCommands.conciseTpBack", true);
        h.set("conciseCommands.conciseTpGrave", true);
        h.set("conciseCommands.conciseFly", true);
        h.set("conciseCommands.conciseVirtualOp", false);
    }

    private static void applyGeneralPermissionCooldownCost(ConfigHolder h) {
        h.set("general.teleportRecordLimit", 100);
        h.set("general.teleportBackSkipType", new ArrayList<String>() {{
            add(EnumTeleportType.TP_BACK.name());
        }});
        h.set("general.teleportAcrossDimension", true);
        h.set("general.teleportCostDistanceLimit", 10000);
        h.set("general.teleportCostDistanceAcrossDimension", 10000);
        h.set("general.teleportViewDistanceLimit", 16 * 64);
        h.set("general.teleportRequestExpireTime", 60);
        h.set("general.teleportRequestCooldownType", EnumCoolDownType.INDIVIDUAL);
        h.set("general.teleportRequestCooldown", 10);
        h.set("general.teleportRandomDistanceLimit", 10000);
        h.set("general.graveSearchRangeLimit", 32);
        h.set("general.teleportHomeLimit", 5);
        h.set("general.helpHeader", "-----==== Narcissus Farewell Help (%d/%d) ====-----");
        h.set("general.tpSound", SoundEvents.ENDERMAN_TELEPORT.getRegistryName().toString());
        h.set("general.tpWithVehicle", true);
        h.set("general.tpWithFollower", true);
        h.set("general.tpWithFollowerRange", 10);
        h.set("general.helpInfoNumPerPage", 5);
        h.set("general.defaultLanguage", "en_us");
        h.set("general.tpWithEnemy", false);
        h.set("general.tpSpawnNoBedWorldDimension", "minecraft:overworld");
        h.set("general.safeTeleport.unsafeBlocks", Stream.of(Blocks.LAVA, Blocks.FIRE, Blocks.CAMPFIRE, Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE, Blocks.CACTUS, Blocks.MAGMA_BLOCK, Blocks.SWEET_BERRY_BUSH).map(b -> {
            ResourceLocation rl = b.getRegistryName();
            return rl == null ? "" : rl.toString();
        }).collect(Collectors.toList()));
        h.set("general.safeTeleport.suffocatingBlocks", Stream.of(Blocks.LAVA, Blocks.WATER).map(b -> {
            ResourceLocation rl = b.getRegistryName();
            return rl == null ? "" : rl.toString();
        }).collect(Collectors.toList()));
        h.set("general.safeTeleport.setBlockWhenSafeNotFound", false);
        h.set("general.safeTeleport.getBlockFromInventory", true);
        h.set("general.safeTeleport.safeBlocks", Stream.of(Blocks.GRASS_BLOCK, Blocks.DIRT_PATH, Blocks.DIRT, Blocks.COBBLESTONE).map(b -> {
            ResourceLocation rl = b.getRegistryName();
            return rl == null ? "" : rl.toString();
        }).collect(Collectors.toList()));
        h.set("general.safeTeleport.safeChunkRange", 1);
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
        applyGeneralPermissionCooldownCost(h);
        h.save();
    }

    public static void resetConfigWithMode1(ConfigHolder h) {
        if (h == null) {
            return;
        }
        applyBaseThroughConcise(h);
        applyGeneralPermissionCooldownCost(h);
        h.set("general.teleportBackSkipType", new ArrayList<>());
        h.set("commandNames.tpHome.commandTpHome", "home");
        h.set("commandNames.tpHome.commandSetHome", "home_set");
        h.set("commandNames.tpHome.commandDelHome", "home_del");
        h.set("commandNames.tpHome.commandGetHome", "home_get");
        h.set("commandNames.tpStage.commandTpStage", "warp");
        h.set("commandNames.tpStage.commandSetStage", "warp_set");
        h.set("commandNames.tpStage.commandDelStage", "warp_del");
        h.set("commandNames.tpStage.commandGetStage", "warp_get");
        h.set("commandNames.commandTpTop", "top");
        h.set("commandNames.commandTpUp", "up");
        h.set("commandNames.commandTpDown", "down");
        h.set("commandNames.commandTpBottom", "bottom");
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
        applyGeneralPermissionCooldownCost(h);
        h.set("conciseCommands.tpAsk.conciseTpAskCancel", false);
        h.set("conciseCommands.tpHere.conciseTpHereCancel", false);
        h.set("conciseCommands.conciseTpRandom", false);
        h.set("conciseCommands.conciseTpSpawn", false);
        h.set("conciseCommands.conciseTpWorldSpawn", false);
        h.set("conciseCommands.conciseTpTop", false);
        h.set("conciseCommands.conciseTpUp", false);
        h.set("conciseCommands.conciseTpBottom", false);
        h.set("conciseCommands.conciseTpDown", false);
        h.set("conciseCommands.conciseTpView", false);
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
