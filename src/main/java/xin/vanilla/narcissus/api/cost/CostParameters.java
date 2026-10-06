package xin.vanilla.narcissus.api.cost;

import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.enums.EnumCostType;

import java.util.Objects;

@Value
@Accessors(fluent = true)
public class CostParameters {
    private static final CostParameters FREE = new CostParameters(EnumCostType.NONE, 0, .002, 0, 20, "", "", "");
    EnumCostType type;
    double fixedAmount;
    double perBlockAmount;
    int minAmount;
    int maxAmount;
    String item;
    String command;
    String customFile;

    public CostParameters(EnumCostType type, double fixedAmount, double perBlockAmount, int minAmount,
                          int maxAmount, String item, String command, String customFile) {
        if (!Double.isFinite(fixedAmount) || fixedAmount < 0 || !Double.isFinite(perBlockAmount)
                || perBlockAmount < 0 || minAmount < 0 || maxAmount < -1
                || maxAmount != -1 && maxAmount < minAmount) {
            throw new IllegalArgumentException("Invalid cost parameters");
        }
        this.type = Objects.requireNonNull(type, "type");
        this.fixedAmount = fixedAmount;
        this.perBlockAmount = perBlockAmount;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.item = text(item, 8192);
        this.command = text(command, 8192);
        this.customFile = text(customFile, 128);
        if (!customFile.isEmpty() && (!customFile.endsWith(".java") || customFile.startsWith("/")
                || customFile.contains("\\") || customFile.contains(":")
                || java.util.Arrays.asList(customFile.split("/", -1)).stream()
                .anyMatch(part -> part.isEmpty() || part.equals(".") || part.equals("..")))) {
            throw new IllegalArgumentException("Invalid relative Java source path");
        }
    }

    private static String text(String value, int limit) {
        if (Objects.requireNonNull(value, "text").length() > limit)
            throw new IllegalArgumentException("Cost text too long");
        return value;
    }

    public static CostParameters free() {
        return FREE;
    }
}
