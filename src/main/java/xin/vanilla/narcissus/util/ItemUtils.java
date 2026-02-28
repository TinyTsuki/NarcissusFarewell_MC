package xin.vanilla.narcissus.util;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.command.arguments.ItemInput;
import net.minecraft.command.arguments.ItemParser;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
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
        return item.getRegistryName();
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
        return ItemStack.isSame(stack1, stack2);
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
        return ItemStack.tagMatches(stack1, stack2);
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
                ItemParser parse = new ItemParser(new StringReader(k), false).parse();
                ItemInput itemInput = new ItemInput(parse.getItem(), parse.getNbt());
                ItemStack result = itemInput.createItemStack(1, false);
                return result.copy();
            } catch (Exception e) {
                LOGGER.error("Failed to deserialize item stack from string: {}", k, e);
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
        String result = "";
        if (itemStack.hasTag() && itemStack.getTag() != null) {
            result = itemStack.getTag().toString();
        }
        return result;
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
                ItemGroup searchTab = ItemGroup.TAB_SEARCH;
                if (searchTab != null) {
                    NonNullList<ItemStack> creativeItems = NonNullList.create();
                    searchTab.fillItemList(creativeItems);

                    if (CollectionUtils.isNotNullOrEmpty(creativeItems)) {
                        for (ItemStack stack : creativeItems) {
                            if (stack != null && !stack.isEmpty()) {
                                items.add(stack.copy());
                                addedItems.add(stack.getItem());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get items from creative search tab", e);
            }

            // 然后添加创造模式物品栏中其他物品组的物品
            try {
                for (ItemGroup group : ItemGroup.TABS) {
                    if (group == null || group == ItemGroup.TAB_SEARCH) continue;
                    try {
                        NonNullList<ItemStack> groupItems = NonNullList.create();
                        group.fillItemList(groupItems);
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
                                item.getRegistryName(), e);
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
                    List<ITextComponent> tooltip = stack.getTooltipLines(
                            Minecraft.getInstance().player,
                            ITooltipFlag.TooltipFlags.NORMAL
                    );
                    if (CollectionUtils.isNotNullOrEmpty(tooltip)) {
                        description = tooltip.stream()
                                .skip(1)
                                .map(ITextComponent::getString)
                                .collect(Collectors.joining(" "))
                                .toLowerCase();
                    }
                }
            } catch (Throwable ignored) {
            }

            // 获取标签
            try {
                ResourceLocation itemId = item.getRegistryName();
                if (itemId != null) {
                    // 获取物品的所有标签
                    item.getTags().forEach(tag -> {
                        tags.add(tag.toString().toLowerCase());
                        tags.add(tag.getPath().toLowerCase());
                    });
                }
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
            PlayerEntity player = Minecraft.getInstance().player;
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
    public static List<ItemStack> getAllPlayerItems(@Nonnull PlayerEntity player) {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(Items.AIR));

        PlayerInventory inventory = player.inventory;
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
    public static List<ItemStack> searchPlayerItems(@Nonnull PlayerEntity player, String keyword) {
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
    public static List<ItemStack> searchPlayerItems(@Nonnull PlayerEntity player, String... keywords) {
        List<ItemStack> playerItems = getAllPlayerItems(player);
        if (keywords == null || keywords.length == 0) {
            return playerItems;
        }

        List<String> validKeywords = Arrays.stream(keywords)
                .filter(StringUtils::isNotNullOrEmpty)
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .collect(Collectors.toList());

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
    public static List<Component> getItemTooltip(@Nonnull ItemStack itemStack, @Nullable PlayerEntity player, boolean advanced) {
        if (isItemNull(itemStack)) {
            List<Component> tooltip = new ArrayList<>();
            ITextComponent hoverName = itemStack.getHoverName();
            if (hoverName instanceof IFormattableTextComponent) {
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
                List<ITextComponent> baseTooltip = new ArrayList<>();
                if (player != null) {
                    baseTooltip.addAll(itemStack.getTooltipLines(
                            player,
                            advanced ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL
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
                ITextComponent nameComponent = baseTooltip.get(0);
                Component name;
                if (nameComponent instanceof IFormattableTextComponent) {
                    name = Component.object(nameComponent);
                } else {
                    name = Component.literal(nameComponent.getString());
                }
                result.add(name);

                // 检查基础tooltip是否包含注册ID
                for (ITextComponent textComponent : baseTooltip) {
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
                        ITextComponent textComponent = baseTooltip.get(i);
                        if (textComponent instanceof IFormattableTextComponent) {
                            IFormattableTextComponent c = (IFormattableTextComponent) textComponent;
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

                // 高级模式：物品名称 -> 物品组 -> 描述 -> 附魔特殊描述 -> 标签 -> 物品ID -> 模组名称
                // 2. 物品组信息
                ItemGroup itemGroup = item.getItemCategory();
                if (itemGroup == null && item == Items.ENCHANTED_BOOK) {
                    // 附魔书的特殊处理
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(itemStack);
                    if (enchantments.size() == 1) {
                        Enchantment enchantment = enchantments.keySet().iterator().next();
                        for (ItemGroup group : ItemGroup.TABS) {
                            if (group.hasEnchantmentCategory(enchantment.category)) {
                                itemGroup = group;
                                break;
                            }
                        }
                    }
                }
                if (itemGroup != null) {
                    IFormattableTextComponent groupName = itemGroup.getDisplayName().copy();
                    groupName.withStyle(TextFormatting.BLUE);
                    Component groupComponent = Component.object(groupName);
                    result.add(groupComponent);
                }

                // 3. 描述
                for (int i = 1; i < baseTooltip.size(); i++) {
                    ITextComponent textComponent = baseTooltip.get(i);
                    if (textComponent instanceof IFormattableTextComponent) {
                        IFormattableTextComponent c = (IFormattableTextComponent) textComponent;
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
                    List<ResourceLocation> tagIds = new ArrayList<>(item.getTags());
                    tagIds.sort(Comparator.comparing(ResourceLocation::toString));
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
                    ITextComponent hoverName = itemStack.getHoverName();
                    if (hoverName instanceof IFormattableTextComponent) {
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
            PlayerEntity player = Minecraft.getInstance().player;
            return getItemTooltip(itemStack, player, advanced);
        } catch (Exception e) {
            LOGGER.debug("Failed to get client player for tooltip", e);
            return getItemTooltip(itemStack, null, advanced);
        }
    }

    // endregion Tooltip

}
