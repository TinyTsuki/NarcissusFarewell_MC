package xin.vanilla.narcissus.data;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
public class PlayerAccess {
    /**
     * 黑名单，不为空则仅屏蔽该名单内玩家
     */
    @NonNull
    private final Set<String> blackList = new HashSet<>();
    /**
     * 白名单，不为空则仅接收该名单内玩家
     */
    @NonNull
    private final Set<String> whiteList = new HashSet<>();
    /**
     * 自动同意tpa请求名单
     */
    @NonNull
    private final Set<String> autoTpaList = new HashSet<>();
    /**
     * 自动同意tph请求名单
     */
    @NonNull
    private final Set<String> autoTphList = new HashSet<>();

    public void removeAutoTpa(String... uuids) {
        this.autoTpaList.removeIf(s -> Arrays.asList(uuids).contains(s));
    }

    public void removeAutoTph(String... uuids) {
        this.autoTphList.removeIf(s -> Arrays.asList(uuids).contains(s));
    }

    public void removeWhiteList(String... uuids) {
        this.whiteList.removeIf(s -> Arrays.asList(uuids).contains(s));
        this.removeAutoTpa(uuids);
        this.removeAutoTph(uuids);
    }

    public void removeBlackList(String... uuids) {
        this.blackList.removeIf(s -> Arrays.asList(uuids).contains(s));
    }

    public boolean addWhiteList(String... uuids) {
        boolean result = this.blackList.stream().noneMatch(s -> Arrays.asList(uuids).contains(s));
        if (result) {
            this.whiteList.addAll(Arrays.asList(uuids));
        }
        return result;
    }

    public boolean addBlackList(String... uuids) {
        boolean result = this.whiteList.stream().noneMatch(s -> Arrays.asList(uuids).contains(s));
        if (result) {
            this.blackList.addAll(Arrays.asList(uuids));
        }
        return result;
    }

    public boolean addTpaList(String... uuids) {
        return this.autoTpaList.addAll(Arrays.asList(uuids));
    }

    public boolean addTphList(String... uuids) {
        return this.autoTphList.addAll(Arrays.asList(uuids));
    }

    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag blackListTag = new ListTag();
        this.blackList.stream().map(StringTag::valueOf).forEach(blackListTag::add);
        tag.put("blackList", blackListTag);

        ListTag whiteListTag = new ListTag();
        this.whiteList.stream().map(StringTag::valueOf).forEach(whiteListTag::add);
        tag.put("whiteList", whiteListTag);

        ListTag tpaListTag = new ListTag();
        this.autoTpaList.stream().map(StringTag::valueOf).forEach(tpaListTag::add);
        tag.put("autoTpaList", tpaListTag);

        ListTag tphListTag = new ListTag();
        this.autoTphList.stream().map(StringTag::valueOf).forEach(tphListTag::add);
        tag.put("autoTphList", tphListTag);

        return tag;
    }

    public static PlayerAccess readFromNBT(CompoundTag tag) {
        PlayerAccess result = new PlayerAccess();

        tag.getList("blackList", 8).stream()
                .map(Tag::getAsString).forEach(result.blackList::add);

        tag.getList("whiteList", 8).stream()
                .map(Tag::getAsString).forEach(result.whiteList::add);

        tag.getList("autoTpaList", 8).stream()
                .map(Tag::getAsString).forEach(result.autoTpaList::add);

        tag.getList("autoTphList", 8).stream()
                .map(Tag::getAsString).forEach(result.autoTphList::add);

        return result;
    }
}
