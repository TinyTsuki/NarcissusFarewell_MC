package xin.vanilla.narcissus.data;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

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

    public CompoundNBT writeToNBT() {
        CompoundNBT tag = new CompoundNBT();

        ListNBT blackListTag = new ListNBT();
        this.blackList.stream().map(StringNBT::new).forEach(blackListTag::add);
        tag.put("blackList", blackListTag);

        ListNBT whiteListTag = new ListNBT();
        this.whiteList.stream().map(StringNBT::new).forEach(whiteListTag::add);
        tag.put("whiteList", whiteListTag);

        ListNBT tpaListTag = new ListNBT();
        this.autoTpaList.stream().map(StringNBT::new).forEach(tpaListTag::add);
        tag.put("autoTpaList", tpaListTag);

        ListNBT tphListTag = new ListNBT();
        this.autoTphList.stream().map(StringNBT::new).forEach(tphListTag::add);
        tag.put("autoTphList", tphListTag);

        return tag;
    }

    public static PlayerAccess readFromNBT(CompoundNBT tag) {
        PlayerAccess result = new PlayerAccess();

        tag.getList("blackList", 8).stream()
                .map(INBT::getAsString).forEach(result.blackList::add);

        tag.getList("whiteList", 8).stream()
                .map(INBT::getAsString).forEach(result.whiteList::add);

        tag.getList("autoTpaList", 8).stream()
                .map(INBT::getAsString).forEach(result.autoTpaList::add);

        tag.getList("autoTphList", 8).stream()
                .map(INBT::getAsString).forEach(result.autoTphList::add);

        return result;
    }
}
