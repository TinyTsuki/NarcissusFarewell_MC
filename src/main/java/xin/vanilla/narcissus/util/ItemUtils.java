package xin.vanilla.narcissus.util;

import com.mojang.brigadier.StringReader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.data.Color;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ItemUtils {
    private ItemUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation UNKNOWN_ITEM = Identifier.create("unknown_item");

    /**
     * 所有物品缓存
     */
    private static volatile List<ItemStack> allItemsCache = Collections.emptyList();

    /**
     * 物品堆信息缓存
     */
    private static final Map<String, ItemStackInfo> itemStackInfoCache = new ConcurrentHashMap<>();

    /**
     * Tooltip缓存
     */
    @OnlyIn(Dist.CLIENT)
    private static final Map<String, List<Component>> tooltipCache = new ConcurrentHashMap<>();

    /**
     * Mod名称缓存
     */
    private static final Map<String, String> modNameCache = new ConcurrentHashMap<>();

    /**
     * 反序列化缓存：String -> ItemStack
     */
    private static final Map<String, ItemStack> deserializeCache = new ConcurrentHashMap<>();


    /**
     * 物品堆信息类
     */
    private static class ItemStackInfo {
        final String registry;
        final String hoverName;
        final String description;
        final Set<String> tags;

        ItemStackInfo(String registry, String hoverName, String description, Set<String> tags) {
            this.registry = registry;
            this.hoverName = hoverName;
            this.description = description;
            this.tags = tags;
        }
    }

    // region 获取物品信息

    /**
     * 获取物品的注册ID
     *
     * @param item 物品
     */
    @Nullable
    public static ResourceLocation getItemRegistry(Item item) {
        if (item == null) return null;
        return ForgeRegistries.ITEMS.getKey(item);
    }

    /**
     * 获取物品的注册ID字符串
     *
     * @param item 物品
     */
    public static String getItemRegistryString(Item item) {
        ResourceLocation registryName = getItemRegistry(item);
        return registryName != null ? registryName.toString() : UNKNOWN_ITEM.toString();
    }

    /**
     * 获取物品堆叠的注册ID
     *
     * @param itemStack 物品堆叠
     */
    @Nullable
    public static ResourceLocation getItemRegistry(ItemStack itemStack) {
        if (isItemNull(itemStack)) {
            return null;
        }
        return getItemRegistry(itemStack.getItem());
    }

    /**
     * 获取物品堆叠的注册ID字符串
     *
     * @param itemStack 物品堆叠
     */
    public static String getItemRegistryString(ItemStack itemStack) {
        if (isItemNull(itemStack)) {
            return UNKNOWN_ITEM.toString();
        }
        return getItemRegistryString(itemStack.getItem());
    }

    /**
     * 获取物品名称翻译键
     *
     * @param item 物品
     */
    public static String getItemNameKey(Item item) {
        if (item == null) return "";
        return item.getDescriptionId();
    }

    /**
     * 获取物品名称翻译键
     *
     * @param itemStack 物品
     */
    public static String getItemNameKey(ItemStack itemStack) {
        if (isItemNull(itemStack)) return "";
        return itemStack.getDescriptionId();
    }

    /**
     * 获取物品显示名称字符串
     *
     * @param itemStack 物品
     */
    public static String getItemHoverNameString(ItemStack itemStack) {
        if (isItemNull(itemStack)) return "";
        return itemStack.getHoverName().getString();
    }

    /**
     * 获取本地化的物品显示名称字符串
     * <p>
     * 客户端专用
     *
     * @param itemStack 物品
     */
    @OnlyIn(Dist.CLIENT)
    public static String getItemHoverNameStringLocalized(ItemStack itemStack) {
        if (isItemNull(itemStack)) return "";
        try {
            return itemStack.getHoverName().getString();
        } catch (Exception e) {
            LOGGER.warn("Failed to get localized display name for item stack", e);
            return getItemHoverNameString(itemStack);
        }
    }

    // endregion

    // region 物品比较

    /**
     * 比较两个物品是否相同
     * <p>
     * 不比较NBT
     *
     * @param stack1 物品1
     * @param stack2 物品2
     */
    public static boolean areItemsEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1 == null || stack2 == null) return false;
        if (stack1.isEmpty() && stack2.isEmpty()) return true;
        if (stack1.isEmpty() || stack2.isEmpty()) return false;
        return ItemStack.isSameItem(stack1, stack2);
    }

    /**
     * 比较两个物品堆叠是否相同
     * <p>
     * 比较NBT
     *
     * @param stack1 物品1
     * @param stack2 物品2
     */
    public static boolean areItemsEqualWithNBT(ItemStack stack1, ItemStack stack2) {
        if (!areItemsEqual(stack1, stack2)) return false;
        // if (stack1 == null || stack2 == null) return false;
        if (stack1.isEmpty() && stack2.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(stack1, stack2);
    }

    /**
     * 比较两个物品是否完全相同
     * <p>
     * 包括数量
     *
     * @param stack1 物品1
     * @param stack2 物品2
     */
    public static boolean areItemsIdentical(ItemStack stack1, ItemStack stack2) {
        if (!areItemsEqualWithNBT(stack1, stack2)) return false;
        if (stack1 == null || stack2 == null) return false;
        return stack1.getCount() == stack2.getCount();
    }

    public static boolean isAir(ItemStack itemStack) {
        return itemStack != null && itemStack.getItem() == Items.AIR;
    }

    public static boolean isAir(Item item) {
        return item != null && item == Items.AIR;
    }

    public static boolean isItemEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty();
    }

    public static boolean isItemNull(ItemStack itemStack) {
        return itemStack == null || (!isAir(itemStack) && itemStack.isEmpty());
    }

    // endregion

    // region 物品序列化与反序列化

    /**
     * 生成 ItemStack 的缓存键
     */
    private static String getItemStackCacheKey(ItemStack itemStack) {
        if (isItemNull(itemStack)) {
            return "";
        }
        try {
            ResourceLocation itemId = getItemRegistry(itemStack);
            if (itemId == null) return "";
            String nbtString = serializeItemStackTag(itemStack);
            return itemId + nbtString;
        } catch (Exception e) {
            LOGGER.debug("Failed to generate cache key for item stack", e);
            return "";
        }
    }

    /**
     * 将物品序列化为字符串
     */
    public static String serializeItemStack(ItemStack itemStack) {
        if (isItemNull(itemStack)) {
            return "";
        }
        try {
            ResourceLocation itemId = getItemRegistry(itemStack);
            if (itemId == null) return "";
            return itemId + serializeItemStackTag(itemStack);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize item stack", e);
            return "";
        }
    }

    /**
     * 从字符串反序列化物品
     */
    public static ItemStack deserializeItemStack(String itemString, String nbtString) {
        if (StringUtils.isNullOrEmptyEx(itemString)) {
            return ItemStack.EMPTY;
        }
        if (StringUtils.isNullOrEmptyEx(nbtString)) {
            nbtString = "";
        }
        return deserializeItemStack(itemString + nbtString);
    }

    /**
     * 从字符串反序列化物品
     */
    public static ItemStack deserializeItemStack(String itemString) {
        if (StringUtils.isNullOrEmptyEx(itemString)) {
            return ItemStack.EMPTY;
        }
        itemString = itemString.trim();
        ItemStack cached = deserializeCache.computeIfAbsent(itemString, k -> {
            try {
                HolderLookup.Provider provider = getRegistryProvider();
                if (provider != null) {
                    ItemParser parser = new ItemParser(provider);
                    ItemParser.ItemResult itemResult = parser.parse(new StringReader(k));
                    ItemInput input = new ItemInput(itemResult.item(), itemResult.components());
                    ItemStack result = input.createItemStack(1, false);
                    return result.copy();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to deserialize item stack from string with components: {}", k, e);
            }
            try {
                String idPart = k;
                int sep = Math.min(
                        indexOrLength(k, '{'),
                        indexOrLength(k, '[')
                );
                if (sep >= 0 && sep < k.length()) {
                    idPart = k.substring(0, sep);
                }
                idPart = idPart.trim();
                if (idPart.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                Item item = getItemFromRegistry(idPart);
                if (item == null) {
                    return ItemStack.EMPTY;
                }
                return new ItemStack(item);
            } catch (Exception e2) {
                LOGGER.error("Failed to deserialize legacy item stack from string: {}", k, e2);
                return ItemStack.EMPTY;
            }
        });
        return cached.isEmpty() ? ItemStack.EMPTY : cached.copy();
    }

    @Nullable
    public static Item getItemFromRegistry(String itemId) {
        ResourceLocation location = Identifier.parse(itemId);
        return getItemFromRegistry(location);
    }

    @Nullable
    public static Item getItemFromRegistry(ResourceLocation location) {
        if (location == null) return null;
        try {
            return ForgeRegistries.ITEMS.getValue(location);
        } catch (Exception e) {
            LOGGER.debug("Failed to find item by registry name: {}", location, e);
            return null;
        }
    }

    /**
     * 将物的的NBT序列化为字符串
     */
    public static String getItemStackTag(ItemStack itemStack) {
        return serializeItemStackTag(itemStack);
    }

    /**
     * 将物的的NBT序列化为字符串
     */
    public static String serializeItemStackTag(ItemStack itemStack) {
        if (isItemNull(itemStack)) {
            return "";
        }
        try {
            HolderLookup.Provider provider = getRegistryProvider();
            if (provider == null) {
                return "";
            }
            Item item = itemStack.getItem();
            Holder.Reference<Item> holder = item.builtInRegistryHolder();
            DataComponentPatch patch = itemStack.getComponentsPatch();
            if (patch == null || patch.isEmpty()) {
                return "";
            }
            ItemInput input = new ItemInput(holder, patch);
            String full = input.serialize(provider);
            String id = getItemRegistryString(item);
            if (full.startsWith(id)) {
                return full.substring(id.length());
            }
            // 非预期格式时，直接返回完整串，保证不丢数据
            return full;
        } catch (Exception e) {
            LOGGER.error("Failed to serialize item stack components", e);
            return "";
        }
    }

    @Nullable
    private static HolderLookup.Provider getRegistryProvider() {
        try {
            try {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level != null) {
                    return minecraft.level.registryAccess();
                }
            } catch (Throwable ignored) {
            }

            return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        } catch (Throwable e) {
            LOGGER.debug("Failed to obtain registry provider for item serialization", e);
            return null;
        }
    }

    /**
     * 返回分隔符位置，如果不存在则返回字符串长度
     */
    private static int indexOrLength(String s, char ch) {
        int idx = s.indexOf(ch);
        return idx >= 0 ? idx : s.length();
    }

    // endregion

    // region 所有物品

    /**
     * 清除物品缓存
     */
    public static void clearCache() {
        allItemsCache = Collections.emptyList();
        itemStackInfoCache.clear();
        tooltipCache.clear();
        modNameCache.clear();
        deserializeCache.clear();
    }

    /**
     * 获取所有物品的列表
     */
    public static List<ItemStack> getAllItems() {
        if (allItemsCache.isEmpty()) {
            synchronized (ItemUtils.class) {
                if (allItemsCache.isEmpty()) {
                    allItemsCache = buildAllItemsList();
                }
            }
        }
        return new ArrayList<>(allItemsCache);
    }

    /**
     * 构建所有物品列表
     */
    private static List<ItemStack> buildAllItemsList() {
        List<ItemStack> items = new ArrayList<>();
        Set<Item> addedItems = new HashSet<>();

        items.add(new ItemStack(Items.AIR));
        addedItems.add(Items.AIR);

        try {
            // 首先从创造模式搜索标签中获取所有物品变体
            try {
                CreativeModeTab searchTab = CreativeModeTabs.searchTab();
                NonNullList<ItemStack> creativeItems = NonNullList.create();
                creativeItems.addAll(searchTab.getDisplayItems());
                if (CollectionUtils.isNotNullOrEmpty(creativeItems)) {
                    for (ItemStack stack : creativeItems) {
                        if (stack != null && !stack.isEmpty()) {
                            items.add(stack.copy());
                            addedItems.add(stack.getItem());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get items from creative search tab", e);
            }

            // 然后添加创造模式物品栏中其他物品组的物品
            try {
                for (CreativeModeTab group : CreativeModeTabs.allTabs()) {
                    if (group == null || group == CreativeModeTabs.searchTab()) continue;
                    try {
                        NonNullList<ItemStack> groupItems = NonNullList.create();
                        groupItems.addAll(group.getDisplayItems());
                        if (CollectionUtils.isNotNullOrEmpty(groupItems)) {
                            for (ItemStack stack : groupItems) {
                                if (stack != null && !stack.isEmpty()) {
                                    // 检查是否已存在相同的物品，不比较NBT
                                    boolean exists = items.stream().anyMatch(existing ->
                                            areItemsEqual(existing, stack));
                                    if (!exists) {
                                        items.add(stack.copy());
                                        addedItems.add(stack.getItem());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Failed to get items from item group: {}",
                                group.getDisplayName().getString(), e);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get items from item groups", e);
            }

            // 最后确保所有注册的物品至少有一个默认堆叠
            for (Item item : ForgeRegistries.ITEMS) {
                if (item == null) continue;
                if (!addedItems.contains(item)) {
                    try {
                        ItemStack defaultStack = new ItemStack(item);
                        if (!defaultStack.isEmpty()) {
                            items.add(defaultStack);
                            addedItems.add(item);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Failed to create default stack for item: {}",
                                getItemRegistry(item), e);
                    }
                }
            }

            LOGGER.info("Built item list with {} items", items.size());
        } catch (Exception e) {
            LOGGER.error("Failed to build all items list", e);
        }
        return items;
    }

    /**
     * 获取物品堆信息
     *
     * @param stack 物品堆
     * @return 物品堆信息
     */
    private static ItemStackInfo getItemStackInfo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ItemStackInfo("", "", "", Collections.emptySet());
        }

        // 使用序列化字符串作为key
        String cacheKey = serializeItemStack(stack);
        return itemStackInfoCache.computeIfAbsent(cacheKey, k -> {
            Item item = stack.getItem();
            String registry = getItemRegistryString(item).toLowerCase();
            String hoverName = getItemHoverNameString(stack).toLowerCase();
            String description = "";
            Set<String> tags = new HashSet<>();

            // 获取描述, 仅客户端
            try {
                if (Minecraft.getInstance().player != null) {
                    List<net.minecraft.network.chat.Component> tooltip = stack.getTooltipLines(
                            Item.TooltipContext.EMPTY,
                            Minecraft.getInstance().player,
                            TooltipFlag.Default.NORMAL
                    );
                    if (CollectionUtils.isNotNullOrEmpty(tooltip)) {
                        description = tooltip.stream()
                                .skip(1)
                                .map(net.minecraft.network.chat.Component::getString)
                                .collect(Collectors.joining(" "))
                                .toLowerCase();
                    }
                }
            } catch (Throwable ignored) {
            }

            // 获取标签 (Forge 1.18.2 使用 getReverseTag 获取物品所属标签)
            try {
                ForgeRegistries.ITEMS.tags().getReverseTag(item).ifPresent(reverseTag ->
                        reverseTag.getTagKeys().forEach(tagKey -> {
                            ResourceLocation loc = tagKey.location();
                            tags.add(loc.toString().toLowerCase());
                            tags.add(loc.getPath().toLowerCase());
                        }));
            } catch (Exception e) {
                LOGGER.debug("Failed to get tags for item: {}", registry, e);
            }

            return new ItemStackInfo(registry, hoverName, description, tags);
        });
    }

    /**
     * 筛选物品列表
     *
     * @param predicate 筛选条件
     * @return 筛选后的物品列表
     */
    public static List<ItemStack> filterItems(Predicate<ItemStack> predicate) {
        if (predicate == null) return getAllItems();
        return getAllItems().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * 检查物品堆是否匹配关键字
     * <p>
     * - @：搜索注册ID
     * <p>
     * - #：搜索标签
     * <p>
     * - $：搜索描述
     *
     * @param info    物品堆叠信息
     * @param keyword 关键字
     * @return 是否匹配
     */
    private static boolean matchesKeyword(ItemStackInfo info, String keyword) {
        if (StringUtils.isNullOrEmpty(keyword)) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase().trim();
        if (lowerKeyword.isEmpty()) {
            return true;
        }

        // 检查前缀
        if (lowerKeyword.startsWith("@")) {
            // @ 搜索注册ID
            String searchTerm = lowerKeyword.substring(1).trim();
            return !searchTerm.isEmpty() && info.registry.contains(searchTerm);
        } else if (lowerKeyword.startsWith("#")) {
            // # 搜索标签
            String searchTerm = lowerKeyword.substring(1).trim();
            return !searchTerm.isEmpty() && info.tags.stream().anyMatch(tag -> tag.contains(searchTerm));
        } else if (lowerKeyword.startsWith("$")) {
            // $ 搜索描述
            String searchTerm = lowerKeyword.substring(1).trim();
            return !searchTerm.isEmpty() && StringUtils.isNotNullOrEmpty(info.description) && info.description.contains(searchTerm);
        } else {
            // 搜索所有字段
            // 搜索注册ID
            if (info.registry.contains(lowerKeyword)) {
                return true;
            }
            // 搜索显示名称
            if (info.hoverName.contains(lowerKeyword)) {
                return true;
            }
            // 搜索描述
            if (StringUtils.isNotNullOrEmpty(info.description) && info.description.contains(lowerKeyword)) {
                return true;
            }
            // 搜索标签
            if (info.tags.stream().anyMatch(tag -> tag.contains(lowerKeyword))) {
                return true;
            }
            return false;
        }
    }

    /**
     * 模糊搜索物品
     * <p>
     * - @：搜索注册ID
     * <p>
     * - #：搜索标签
     * <p>
     * - $：搜索描述
     *
     * @param keyword 搜索关键字
     * @return 匹配的物品列表
     */
    public static List<ItemStack> searchItems(String keyword) {
        if (StringUtils.isNullOrEmpty(keyword)) {
            return getAllItems();
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return getAllItems();
        }

        return getAllItems().stream()
                .filter(stack -> {
                    if (stack == null || stack.isEmpty()) return false;
                    ItemStackInfo info = getItemStackInfo(stack);
                    return matchesKeyword(info, trimmedKeyword);
                })
                .collect(Collectors.toList());
    }

    /**
     * 模糊搜索物品
     * 支持多个关键字
     * <p>
     * - @：搜索注册ID
     * <p>
     * - #：搜索标签
     * <p>
     * - $：搜索描述
     *
     * @param keywords 搜索关键字数组
     * @return 匹配的物品列表
     */
    public static List<ItemStack> searchItems(String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return getAllItems();
        }

        List<String> validKeywords = Arrays.stream(keywords)
                .filter(StringUtils::isNotNullOrEmpty)
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .collect(Collectors.toList());

        if (validKeywords.isEmpty()) {
            return getAllItems();
        }

        return getAllItems().stream()
                .filter(stack -> {
                    if (stack == null || stack.isEmpty()) return false;
                    ItemStackInfo info = getItemStackInfo(stack);
                    // 所有关键字都必须匹配
                    for (String keyword : validKeywords) {
                        if (!matchesKeyword(info, keyword)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据注册ID查找物品
     *
     * @param registry 注册ID字符串
     */
    public static ItemStack findItemByRegistry(String registry) {
        if (StringUtils.isNullOrEmpty(registry)) {
            return ItemStack.EMPTY;
        }

        Item item = getItemFromRegistry(registry);
        if (item != null) {
            return new ItemStack(item);
        }

        return ItemStack.EMPTY;
    }

    /**
     * 根据注册ID精确查找物品
     *
     * @param location 注册ID
     */
    public static ItemStack findItemByRegistry(ResourceLocation location) {
        if (location == null) {
            return ItemStack.EMPTY;
        }

        Item item = getItemFromRegistry(location);
        if (item != null) {
            return new ItemStack(item);
        }

        return ItemStack.EMPTY;
    }

    // endregion 所有物品

    // region 玩家物品

    /**
     * 获取玩家身上的所有物品
     *
     * @return 玩家身上的所有物品列表副本
     */
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    public static List<ItemStack> getAllPlayerItems() {
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                return getAllPlayerItems(player);
            }
        } catch (Throwable ignored) {
        }
        return new ArrayList<>();
    }

    /**
     * 获取玩家身上的所有物品
     *
     * @param player 玩家
     * @return 玩家身上的所有物品列表副本
     */
    @Nonnull
    public static List<ItemStack> getAllPlayerItems(@Nonnull Player player) {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(Items.AIR));

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return items;
        }
        // 获取所有槽位的物品
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        return items;
    }

    /**
     * 模糊搜索玩家物品
     * <p>
     * - @：搜索注册ID
     * <p>
     * - #：搜索标签
     * <p>
     * - $：搜索描述
     *
     * @param player  玩家
     * @param keyword 搜索关键字
     * @return 匹配的物品列表
     */
    public static List<ItemStack> searchPlayerItems(@Nonnull Player player, String keyword) {
        List<ItemStack> playerItems = getAllPlayerItems(player);
        if (StringUtils.isNullOrEmpty(keyword)) {
            return playerItems;
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return playerItems;
        }

        return playerItems.stream()
                .filter(stack -> {
                    if (stack == null || stack.isEmpty()) return false;
                    ItemStackInfo info = getItemStackInfo(stack);
                    return matchesKeyword(info, trimmedKeyword);
                })
                .collect(Collectors.toList());
    }

    /**
     * 模糊搜索玩家物品
     * 支持多个关键字
     * <p>
     * - @：搜索注册ID
     * <p>
     * - #：搜索标签
     * <p>
     * - $：搜索描述
     *
     * @param player   玩家
     * @param keywords 搜索关键字数组
     * @return 匹配的物品列表
     */
    public static List<ItemStack> searchPlayerItems(@Nonnull Player player, String... keywords) {
        List<ItemStack> playerItems = getAllPlayerItems(player);
        if (keywords == null || keywords.length == 0) {
            return playerItems;
        }

        List<String> validKeywords = Arrays.stream(keywords)
                .filter(StringUtils::isNotNullOrEmpty)
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .toList();

        if (validKeywords.isEmpty()) {
            return playerItems;
        }

        return playerItems.stream()
                .filter(stack -> {
                    if (stack == null || stack.isEmpty()) return false;
                    ItemStackInfo info = getItemStackInfo(stack);
                    for (String keyword : validKeywords) {
                        if (!matchesKeyword(info, keyword)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // endregion 玩家物品管理

    // region Tooltip

    /**
     * 获取Mod名称
     */
    @Nonnull
    public static String getModName(@Nonnull String modId) {
        if (StringUtils.isNullOrEmpty(modId) || "minecraft".equals(modId)) {
            return "Minecraft";
        }
        return modNameCache.computeIfAbsent(modId, id -> {
            try {
                return ModList.get().getModContainerById(id)
                        .map(container -> container.getModInfo().getDisplayName())
                        .orElse(id);
            } catch (Exception e) {
                LOGGER.debug("Failed to get mod name for: {}", id, e);
                return id;
            }
        });
    }

    /**
     * 获取物品的完整Tooltip列表
     *
     * @param itemStack 物品堆
     * @param player    玩家
     * @param advanced  是否显示高级信息
     * @return Tooltip列表
     */
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    public static List<Component> getItemTooltip(@Nonnull ItemStack itemStack, @Nullable Player player, boolean advanced) {
        if (isItemNull(itemStack)) {
            List<Component> tooltip = new ArrayList<>();
            net.minecraft.network.chat.Component hoverName = itemStack.getHoverName();
            if (hoverName instanceof MutableComponent) {
                tooltip.add(Component.object(hoverName));
            } else {
                tooltip.add(Component.literal(hoverName.getString()));
            }
            if (advanced) {
                ResourceLocation registryName = getItemRegistry(itemStack);
                if (registryName != null) {
                    Component registryComponent = Component.literal(registryName.toString())
                            .color(Color.argb(0xFF404040));
                    tooltip.add(registryComponent);
                }
            }
            return tooltip;
        }

        String cacheKey = serializeItemStack(itemStack) + "|advanced:" + advanced;
        if (player != null) {
            cacheKey += "|player:" + player.getUUID();
        }

        return tooltipCache.computeIfAbsent(cacheKey, k -> {
            List<Component> result = new ArrayList<>();

            try {
                // 获取基础tooltip
                List<net.minecraft.network.chat.Component> baseTooltip = new ArrayList<>();
                if (player != null) {
                    baseTooltip.addAll(itemStack.getTooltipLines(
                            Item.TooltipContext.EMPTY,
                            player,
                            advanced ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
                    ));
                } else {
                    baseTooltip.add(itemStack.getHoverName());
                }

                if (baseTooltip.isEmpty()) {
                    return result;
                }

                Item item = itemStack.getItem();
                ResourceLocation registryName = getItemRegistry(itemStack);
                String registryString = registryName != null ? registryName.toString() : null;
                boolean baseTooltipContainsRegistry = false;

                // 1. 物品名称
                net.minecraft.network.chat.Component nameComponent = baseTooltip.get(0);
                Component name;
                if (nameComponent instanceof MutableComponent) {
                    name = Component.object(nameComponent);
                } else {
                    name = Component.literal(nameComponent.getString());
                }
                result.add(name);

                // 检查基础tooltip是否包含注册ID
                for (net.minecraft.network.chat.Component textComponent : baseTooltip) {
                    String text = textComponent.getString();
                    if (registryString != null && text.contains(registryString)) {
                        baseTooltipContainsRegistry = true;
                        break;
                    }
                }

                // 普通模式：物品名称 -> 描述 -> 物品ID
                if (!advanced) {
                    // 2. 描述
                    for (int i = 1; i < baseTooltip.size(); i++) {
                        net.minecraft.network.chat.Component textComponent = baseTooltip.get(i);
                        if (textComponent instanceof MutableComponent c) {
                            if (StringUtils.isNotNullOrEmpty(c.getString())) {
                                result.add(Component.object(textComponent));
                            }
                        } else {
                            String text = textComponent.getString();
                            if (StringUtils.isNotNullOrEmpty(text)) {
                                result.add(Component.literal(text));
                            }
                        }
                    }
                    // 3. 物品ID
                    if (registryName != null && !baseTooltipContainsRegistry) {
                        Component registryComponent = Component.literal(registryName.toString())
                                .color(Color.argb(0xFF404040));
                        result.add(registryComponent);
                    }
                    return result;
                }

                // 2. 物品组信息
                CreativeModeTab itemGroup = null;
                for (CreativeModeTab group : CreativeModeTabs.allTabs()) {
                    if (group == null || group == CreativeModeTabs.searchTab()) continue;
                    try {
                        if (group.contains(itemStack)) {
                            itemGroup = group;
                            break;
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Failed to check if tab contains item: {}", group.getDisplayName().getString(), e);
                    }
                }
                if (itemGroup != null) {
                    MutableComponent groupName = itemGroup.getDisplayName().copy();
                    groupName.withStyle(ChatFormatting.BLUE);
                    Component groupComponent = Component.object(groupName);
                    result.add(groupComponent);
                }

                // 3. 描述
                for (int i = 1; i < baseTooltip.size(); i++) {
                    net.minecraft.network.chat.Component textComponent = baseTooltip.get(i);
                    if (textComponent instanceof MutableComponent c) {
                        if (StringUtils.isNotNullOrEmpty(c.getString())) {
                            result.add(Component.object(textComponent));
                        }
                    } else {
                        String text = textComponent.getString();
                        if (StringUtils.isNotNullOrEmpty(text)) {
                            result.add(Component.literal(text));
                        }
                    }
                }

                // 5. 标签列表
                try {
                    List<ResourceLocation> tagIds = ForgeRegistries.ITEMS.tags().getReverseTag(item)
                            .map(rt -> rt.getTagKeys().map(TagKey::location)
                                    .sorted(Comparator.comparing(ResourceLocation::toString))
                                    .collect(Collectors.toList()))
                            .orElse(Collections.emptyList());
                    for (ResourceLocation tagId : tagIds) {
                        Component tagComponent = Component.literal("#" + tagId)
                                .color(Color.argb(0xFF8A2BE2));
                        result.add(tagComponent);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to get tags for item: {}", getItemRegistryString(itemStack), e);
                }

                // 6. 物品ID
                if (registryName != null && !baseTooltipContainsRegistry) {
                    Component registryComponent = Component.literal(registryName.toString())
                            .color(Color.argb(0xFF404040));
                    result.add(registryComponent);
                }

                // 7. 模组名称
                if (registryName != null && !"minecraft".equals(registryName.getNamespace())) {
                    String modName = getModName(registryName.getNamespace());
                    Component modComponent = Component.literal(modName)
                            .color(Color.argb(0xFF808080));
                    result.add(modComponent);
                }

                return result;
            } catch (Exception e) {
                LOGGER.error("Failed to get tooltip for item: {}", getItemRegistryString(itemStack), e);
                if (result.isEmpty()) {
                    net.minecraft.network.chat.Component hoverName = itemStack.getHoverName();
                    if (hoverName instanceof MutableComponent) {
                        result.add(Component.object(hoverName));
                    } else {
                        result.add(Component.literal(hoverName.getString()));
                    }
                }
                return result;
            }
        });
    }

    /**
     * 获取物品的完整Tooltip列表
     *
     * @param itemStack 物品堆
     * @param advanced  是否显示高级信息
     * @return Tooltip列表
     */
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    public static List<Component> getItemTooltip(@Nonnull ItemStack itemStack, boolean advanced) {
        try {
            Player player = Minecraft.getInstance().player;
            return getItemTooltip(itemStack, player, advanced);
        } catch (Exception e) {
            LOGGER.debug("Failed to get client player for tooltip", e);
            return getItemTooltip(itemStack, null, advanced);
        }
    }

    // endregion Tooltip

}
