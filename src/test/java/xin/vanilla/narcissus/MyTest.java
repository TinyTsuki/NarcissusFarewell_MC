package xin.vanilla.narcissus;

import org.junit.Test;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.enums.EMCColor;
import xin.vanilla.narcissus.util.StringUtils;

public class MyTest {

    @Test
    public void testColor() {
        for (EMCColor value : EMCColor.values()) {
            System.out.print(value.getCode());
            System.out.println(StringUtils.rgbToMinecraftColor(value.getColor()));
        }
    }

    @Test
    public void testRandom() {
        for (int i = 0; i < 100; i++) {
            System.out.print(i + ": ");
            for (int j = 0; j < 20; j++) {
                System.out.print(Coordinate.getRandomWithWeight(0, 250, i, 0.75) + " ");
            }
            System.out.println();
        }
    }
}
