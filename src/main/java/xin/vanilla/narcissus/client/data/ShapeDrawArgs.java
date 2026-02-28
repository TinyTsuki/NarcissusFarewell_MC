package xin.vanilla.narcissus.client.data;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 形状绘制参数
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class ShapeDrawArgs {
    /**
     * 矩阵栈
     */
    private PoseStack stack;

    /**
     * 颜色
     */
    private int color;

    /**
     * 形状类型
     */
    private ShapeType type = ShapeType.RECT;

    /**
     * 矩形参数
     */
    private RectParams rect = new RectParams();

    /**
     * 圆形参数
     */
    private CircleParams circle = new CircleParams();

    /**
     * 椭圆参数
     */
    private EllipseParams ellipse = new EllipseParams();

    /**
     * 扇形参数
     */
    private SectorParams sector = new SectorParams();

    /**
     * 扇环参数
     */
    private SectorRingParams sectorRing = new SectorRingParams();

    /**
     * 多边形参数
     */
    private PolygonParams polygon = new PolygonParams();

    /**
     * 形状类型枚举
     */
    public enum ShapeType {
        /**
         * 矩形
         */
        RECT,
        /**
         * 圆形
         */
        CIRCLE,
        /**
         * 椭圆
         */
        ELLIPSE,
        /**
         * 扇形
         */
        SECTOR,
        /**
         * 扇环
         */
        SECTOR_RING,
        /**
         * 多边形
         */
        POLYGON,
    }

    /**
     * 圆角模式枚举
     */
    public enum RoundedCornerMode {
        /**
         * 粗糙
         */
        ROUGH,
        /**
         * 精细
         */
        FINE,
        /**
         * 自动
         */
        AUTO,
    }

    /**
     * 矩形参数
     */
    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class RectParams {
        /**
         * X坐标
         */
        private float x;

        /**
         * Y坐标
         */
        private float y;

        /**
         * 宽度
         */
        private float width;

        /**
         * 高度
         */
        private float height;

        /**
         * 左上角圆角半径
         */
        private float topLeft;

        /**
         * 右上角圆角半径
         */
        private float topRight;

        /**
         * 左下角圆角半径
         */
        private float bottomLeft;

        /**
         * 右下角圆角半径
         */
        private float bottomRight;

        /**
         * 边框厚度</br>
         * 0为无边框实心矩形
         */
        private float border = 0;

        /**
         * 矩形圆角模式
         */
        private RoundedCornerMode cornerMode = RoundedCornerMode.AUTO;

        /**
         * 设置圆角半径
         */
        public RectParams radius(float radius) {
            this.topLeft = radius;
            this.topRight = radius;
            this.bottomLeft = radius;
            this.bottomRight = radius;
            return this;
        }

        /**
         * 设置圆角半径
         */
        public RectParams radius(float topLeft, float topRight, float bottomLeft, float bottomRight) {
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomLeft = bottomLeft;
            this.bottomRight = bottomRight;
            return this;
        }

        /**
         * 是否有圆角
         */
        public boolean hasRadius() {
            return topLeft > 0 || topRight > 0 || bottomLeft > 0 || bottomRight > 0;
        }

        /**
         * 是否为统一圆角
         */
        public boolean isUniformRadius() {
            return topLeft == topRight && topLeft == bottomLeft && topLeft == bottomRight;
        }

    }

    /**
     * 圆形参数
     */
    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CircleParams {
        /**
         * 中心X坐标
         */
        private float centerX;

        /**
         * 中心Y坐标
         */
        private float centerY;

        /**
         * 半径
         */
        private float radius;

        /**
         * 分段数</br>
         * <=0自动
         */
        private int segments = 0;

        /**
         * 边框厚度</br>
         * 0为无边框实心圆形
         */
        private float border = 0;

    }

    /**
     * 椭圆参数
     */
    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class EllipseParams {
        /**
         * 中心X坐标
         */
        private float centerX;

        /**
         * 中心Y坐标
         */
        private float centerY;

        /**
         * X轴半径
         */
        private float radiusX;

        /**
         * Y轴半径
         */
        private float radiusY;

        /**
         * 旋转角度</br>
         * 0为正右, 顺时针
         */
        private double rotation = 0;

        /**
         * 分段数</br>
         * <=0自动
         */
        private int segments = 0;

        /**
         * 边框厚度</br>
         * 0为无边框实心椭圆
         */
        private float border = 0;

    }

    /**
     * 扇形参数
     */
    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class SectorParams {
        /**
         * 中心X坐标
         */
        private float centerX;

        /**
         * 中心Y坐标
         */
        private float centerY;

        /**
         * 半径
         */
        private float radius;

        /**
         * 起始角度</br>
         * 0为正右, 顺时针
         */
        private double startAngle = 0;

        /**
         * 结束角度</br>
         * start至end顺时针旋转
         */
        private double endAngle = 90;

        /**
         * 是否使用弧度
         */
        private boolean useRadians = false;

        /**
         * 分段数</br>
         * <=0自动
         */
        private int segments = 0;

        /**
         * 设置角度
         */
        public SectorParams angles(double startAngle, double endAngle) {
            this.startAngle = startAngle;
            this.endAngle = endAngle;
            this.useRadians = false;
            return this;
        }

        /**
         * 设置弧度
         */
        public SectorParams anglesRad(double startAngle, double endAngle) {
            this.startAngle = startAngle;
            this.endAngle = endAngle;
            this.useRadians = true;
            return this;
        }

    }

    /**
     * 扇环参数
     */
    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class SectorRingParams {
        /**
         * 中心X坐标
         */
        private float centerX;

        /**
         * 中心Y坐标
         */
        private float centerY;

        /**
         * 外半径
         */
        private float outerRadius;

        /**
         * 内半径</br>
         * =0 从中心开始, >0 从内半径开始
         */
        private float innerRadius = 0;

        /**
         * 扇环厚度（外半径 - 内半径，如果设置了此值，则 innerRadius = outerRadius - lineWidth）
         */
        private float lineWidth = 0;

        /**
         * 是否使用 lineWidth 来计算 innerRadius
         */
        private boolean useLineWidth = false;

        /**
         * 起始角度（度，0为正右，顺时针）
         */
        private double startAngle = 0;

        /**
         * 结束角度（度，start至end顺时针旋转）
         */
        private double endAngle = 90;

        /**
         * 是否使用弧度（false表示使用度，true表示使用弧度）
         */
        private boolean useRadians = false;

        /**
         * 分段数（<=0表示自动选择）
         */
        private int segments = 0;

        /**
         * 设置角度（度）
         */
        public SectorRingParams angles(double startAngle, double endAngle) {
            this.startAngle = startAngle;
            this.endAngle = endAngle;
            this.useRadians = false;
            return this;
        }

        /**
         * 设置角度（弧度）
         */
        public SectorRingParams anglesRad(double startAngle, double endAngle) {
            this.startAngle = startAngle;
            this.endAngle = endAngle;
            this.useRadians = true;
            return this;
        }

        /**
         * 设置扇环厚度（会自动计算内半径：innerRadius = outerRadius - lineWidth）
         */
        public SectorRingParams lineWidth(float lineWidth) {
            this.lineWidth = lineWidth;
            this.useLineWidth = true;
            return this;
        }

        /**
         * 设置内半径
         */
        public SectorRingParams innerRadius(float innerRadius) {
            this.innerRadius = innerRadius;
            this.useLineWidth = false;
            return this;
        }

        /**
         * 获取实际的内半径（如果使用 lineWidth，则计算得出）
         */
        public float getActualInnerRadius() {
            if (useLineWidth && lineWidth > 0) {
                return Math.max(0, outerRadius - lineWidth);
            }
            return innerRadius;
        }
    }

    /**
     * 多边形参数
     */
    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class PolygonParams {
        /**
         * 中心X坐标
         */
        private float centerX;

        /**
         * 中心Y坐标
         */
        private float centerY;

        /**
         * 外接圆半径
         */
        private float radius;

        /**
         * 边数
         * >= 3
         */
        private int sides = 3;

        /**
         * 旋转角度</br>
         * 0为正右, 顺时针
         */
        private double rotation = 0;

        /**
         * 边框厚度</br>
         * 0为无边框实心多边形
         */
        private float border = 0;
    }

    /**
     * 创建矩形绘制参数
     */
    public static ShapeDrawArgs rect(PoseStack stack, float x, float y, float width, float height, int color) {
        return new ShapeDrawArgs()
                .stack(stack)
                .color(color)
                .type(ShapeType.RECT)
                .rect(new RectParams().x(x).y(y).width(width).height(height));
    }

    /**
     * 创建圆形绘制参数
     */
    public static ShapeDrawArgs circle(PoseStack stack, float centerX, float centerY, float radius, int color) {
        return new ShapeDrawArgs()
                .stack(stack)
                .color(color)
                .type(ShapeType.CIRCLE)
                .circle(new CircleParams().centerX(centerX).centerY(centerY).radius(radius));
    }

    /**
     * 创建椭圆绘制参数
     */
    public static ShapeDrawArgs ellipse(PoseStack stack, float centerX, float centerY, float radiusX, float radiusY, int color) {
        return new ShapeDrawArgs()
                .stack(stack)
                .color(color)
                .type(ShapeType.ELLIPSE)
                .ellipse(new EllipseParams().centerX(centerX).centerY(centerY).radiusX(radiusX).radiusY(radiusY));
    }

    /**
     * 创建扇形绘制参数
     */
    public static ShapeDrawArgs sector(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, int color) {
        return new ShapeDrawArgs()
                .stack(stack)
                .color(color)
                .type(ShapeType.SECTOR)
                .sector(new SectorParams().centerX(centerX).centerY(centerY).radius(radius).angles(startAngle, endAngle));
    }

    /**
     * 使用弧度创建扇形绘制参数
     */
    public static ShapeDrawArgs sectorRad(PoseStack stack, float centerX, float centerY, float radius, double startAngle, double endAngle, int color) {
        return new ShapeDrawArgs()
                .stack(stack)
                .color(color)
                .type(ShapeType.SECTOR)
                .sector(new SectorParams().centerX(centerX).centerY(centerY).radius(radius).anglesRad(startAngle, endAngle));
    }

    /**
     * 创建扇环绘制参数
     */
    public static ShapeDrawArgs sectorRing(PoseStack stack, float centerX, float centerY, float outerRadius, float innerRadius, double startAngle, double endAngle, int color) {
        return new ShapeDrawArgs()
                .stack(stack)
                .color(color)
                .type(ShapeType.SECTOR_RING)
                .sectorRing(new SectorRingParams().centerX(centerX).centerY(centerY).outerRadius(outerRadius).innerRadius(innerRadius).angles(startAngle, endAngle));
    }

    /**
     * 创建扇环绘制参数（使用弧度）
     */
    public static ShapeDrawArgs sectorRingRad(PoseStack stack, float centerX, float centerY, float outerRadius, double startAngle, double endAngle, int color) {
        return new ShapeDrawArgs()
                .stack(stack)
                .color(color)
                .type(ShapeType.SECTOR_RING)
                .sectorRing(new SectorRingParams().centerX(centerX).centerY(centerY).outerRadius(outerRadius).anglesRad(startAngle, endAngle));
    }

    /**
     * 创建多边形绘制参数
     */
    public static ShapeDrawArgs polygon(PoseStack stack, float centerX, float centerY, float radius, int sides, int color) {
        return new ShapeDrawArgs()
                .stack(stack)
                .color(color)
                .type(ShapeType.POLYGON)
                .polygon(new PolygonParams().centerX(centerX).centerY(centerY).radius(radius).sides(sides));
    }
}
