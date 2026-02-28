package xin.vanilla.narcissus.client.data;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.client.enums.EnumCoordinateType;
import xin.vanilla.narcissus.client.enums.EnumSizeType;
import xin.vanilla.narcissus.util.JsonUtils;
import xin.vanilla.narcissus.util.StringUtils;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Accessors(chain = true, fluent = true)
public class ScreenCoordinate implements Serializable {
    private static final long serialVersionUID = 1L;

    private ScreenCoordinate parent;

    private double x;
    private double y;
    private EnumCoordinateType xType = EnumCoordinateType.ABSOLUTE;
    private EnumCoordinateType yType = EnumCoordinateType.ABSOLUTE;

    private double width;
    private double height;
    private EnumSizeType wType = EnumSizeType.ABSOLUTE;
    private EnumSizeType hType = EnumSizeType.ABSOLUTE;

    private String textureId = "";

    private Texture texture = Texture.empty();

    public ScreenCoordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public ScreenCoordinate(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean hasParent() {
        return this.parent != null;
    }


    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.parent != null) {
            JsonObject parentJson = this.parent.toJson();
            if (parentJson != null && !parentJson.entrySet().isEmpty()) {
                json.add("parent", parentJson);
            }
        }
        if (this.x != 0.0) {
            json.addProperty("x", this.x);
        }
        if (this.y != 0.0) {
            json.addProperty("y", this.y);
        }
        if (this.xType != null && this.xType != EnumCoordinateType.ABSOLUTE) {
            json.addProperty("xType", this.xType.name());
        }
        if (this.yType != null && this.yType != EnumCoordinateType.ABSOLUTE) {
            json.addProperty("yType", this.yType.name());
        }

        if (this.width != 0.0) {
            json.addProperty("width", this.width);
        }
        if (this.height != 0.0) {
            json.addProperty("height", this.height);
        }
        if (this.wType != null && this.wType != EnumSizeType.ABSOLUTE) {
            json.addProperty("wType", this.wType.name());
        }
        if (this.hType != null && this.hType != EnumSizeType.ABSOLUTE) {
            json.addProperty("hType", this.hType.name());
        }

        if (StringUtils.isNotNullOrEmpty(this.textureId)) {
            json.addProperty("textureId", this.textureId);
        }
        if (this.texture != null) {
            JsonObject textureJson = this.texture.toJson();
            if (textureJson != null && !textureJson.entrySet().isEmpty()) {
                json.add("texture", textureJson);
            }
        }

        return json;
    }

    public static ScreenCoordinate fromJson(JsonObject json) {
        ScreenCoordinate coordinate = new ScreenCoordinate();
        if (json.has("parent") && json.get("parent").isJsonObject()) {
            coordinate.parent(fromJson(json.getAsJsonObject("parent")));
        }
        coordinate.x(JsonUtils.getDouble(json, "x", 0.0));
        coordinate.y(JsonUtils.getDouble(json, "y", 0.0));
        if (json.has("xType")) {
            try {
                coordinate.xType(EnumCoordinateType.valueOf(json.get("xType").getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (json.has("yType")) {
            try {
                coordinate.yType(EnumCoordinateType.valueOf(json.get("yType").getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        coordinate.width(JsonUtils.getDouble(json, "width", 0.0));
        coordinate.height(JsonUtils.getDouble(json, "height", 0.0));
        if (json.has("wType")) {
            try {
                coordinate.wType(EnumSizeType.valueOf(json.get("wType").getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (json.has("hType")) {
            try {
                coordinate.hType(EnumSizeType.valueOf(json.get("hType").getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        coordinate.textureId(JsonUtils.getString(json, "textureId", ""));
        if (json.has("texture") && json.get("texture").isJsonObject()) {
            coordinate.texture(Texture.fromJson(json.getAsJsonObject("texture")));
        }
        return coordinate;
    }

    public ScreenCoordinate copy() {
        return fromJson(this.toJson());
    }
}
