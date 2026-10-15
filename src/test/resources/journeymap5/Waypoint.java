package journeymap.client.waypoint;
import java.util.*;
public class Waypoint {
    public enum Type { Normal }
    public static boolean viewingNether;
    public String name, origin, displayId, nativeId;
    public int x, y, z;
    public Collection<String> dimensions;
    public Waypoint(String name, int x, int y, int z, boolean enabled, int r, int g, int b,
                    Type type, String origin, String dimension, Collection<String> dimensions, boolean deviation) {
        this.name=name; this.x=x*(dimension.equals("minecraft:the_nether")?8:1); this.y=y;
        this.z=z*(dimension.equals("minecraft:the_nether")?8:1); this.origin=origin; this.dimensions=dimensions;
        nativeId=name+"_"+this.x+","+y+","+this.z;
    }
    public Waypoint(journeymap.client.api.display.Waypoint point) {
        this(point.name,point.pos.getX(),point.pos.getY(),point.pos.getZ(),true,0,255,0,Type.Normal,
             point.owner,point.dimension,Collections.singletonList(point.dimension),true);
        displayId=point.id;
    }
    public String getId(){return displayId==null ? nativeId : origin+":"+displayId;}
    public String getFileName(){return nativeId+".json";}
    public String getDisplayId(){return displayId;}
    public String getOrigin(){return origin;}
    public String getName(){return name;}
    public Collection<String> getDimensions(){return dimensions;}
    public int getX(){return viewingNether ? x>>3 : x;}
    public int getY(){return y;}
    public int getZ(){return viewingNether ? z>>3 : z;}
    public double getRawCenterX(){return x+0.5;}
    public double getRawCenterZ(){return z+0.5;}
    public Waypoint setName(String name){this.name=name;return this;}
    public Waypoint setColor(Integer color){return this;}
    public Waypoint setShowDeviation(Boolean value){return this;}
}
