package xin.vanilla.narcissus.client.data;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.util.JsonUtils;
import xin.vanilla.narcissus.util.StringUtils;
import xin.vanilla.narcissus.util.TextureUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

@Data
@Accessors(chain = true, fluent = true)
public class Texture {
    /**
     * 材质资源
     */
    @Nonnull
    private final ResourceLocation location;
    /**
     * 材质尺寸
     */
    private final int uvWidth, uvHeight;

    /**
     * 纹理在材质中的坐标
     */
    private int u0, v0, uWidth, vHeight;

    private Texture(@Nonnull ResourceLocation location) {
        this.location = location;
        KeyValue<Integer, Integer> size;
        size = TextureUtils.getTextureSize(location);
        this.uvWidth = size.key();
        this.uvHeight = size.val();
        this.u0 = 0;
        this.v0 = 0;
        this.uWidth = uvWidth;
        this.vHeight = uvHeight;
    }

    private Texture(ResourceLocation location, int uvWidth, int uvHeight) {
        this.location = location;
        this.uvWidth = uvWidth;
        this.uvHeight = uvHeight;
        this.u0 = 0;
        this.v0 = 0;
        this.uWidth = uvWidth;
        this.vHeight = uvHeight;
    }

    public static Texture empty() {
        return new Texture(MissingTextureAtlasSprite.getLocation());
    }

    public static Texture of(@Nonnull ResourceLocation location) {
        return new Texture(location);
    }

    public static Texture of(@Nonnull ResourceLocation location, int uvWidth, int uvHeight) {
        return new Texture(location, uvWidth, uvHeight);
    }

    public static Texture of(@Nonnull ResourceLocation location, Texture texture) {
        return new Texture(location).from(texture);
    }

    public Texture from(Texture texture) {
        this.u0 = texture.u0;
        this.v0 = texture.v0;
        this.uWidth = texture.uWidth;
        this.vHeight = texture.vHeight;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Texture)) return false;
        Texture that = (Texture) o;
        return location.equals(that.location) && uvWidth == that.uvWidth && uvHeight == that.uvHeight && u0 == that.u0 && v0 == that.v0 && uWidth == that.uWidth && vHeight == that.vHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, uvWidth, uvHeight, u0, v0, uWidth, vHeight);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        if (this.u0 != 0) {
            json.addProperty("u0", this.u0);
        }
        if (this.v0 != 0) {
            json.addProperty("v0", this.v0);
        }
        if (this.uWidth != 0) {
            json.addProperty("uWidth", this.uWidth);
        }
        if (this.vHeight != 0) {
            json.addProperty("vHeight", this.vHeight);
        }

        if (this.uvWidth != 0) {
            json.addProperty("uvWidth", this.uvWidth);
        }
        if (this.uvHeight != 0) {
            json.addProperty("uvHeight", this.uvHeight);
        }

        return json;
    }

    public static Texture fromJson(JsonObject json) {
        ResourceLocation location = null;
        String locationStr = JsonUtils.getString(json, "location", null);
        if (StringUtils.isNotNullOrEmpty(locationStr)) {
            try {
                location = new ResourceLocation(locationStr);
            } catch (Exception ignored) {
            }
        }
        int uvWidth = JsonUtils.getInt(json, "uvWidth", 256);
        int uvHeight = JsonUtils.getInt(json, "uvHeight", 256);
        Texture texture = new Texture(location, uvWidth, uvHeight);
        texture.u0(JsonUtils.getInt(json, "u0", 0));
        texture.v0(JsonUtils.getInt(json, "v0", 0));
        texture.uWidth(JsonUtils.getInt(json, "uWidth", uvWidth));
        texture.vHeight(JsonUtils.getInt(json, "vHeight", uvHeight));
        return texture;
    }
}
