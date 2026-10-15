package journeymap.client.api.display;
import net.minecraft.core.BlockPos;
public class Waypoint {
    public final String owner,id,name,dimension;
    public final BlockPos pos;
    public Waypoint(String owner,String id,String name,String dimension,BlockPos pos){
        this.owner=owner; this.id=id; this.name=name; this.dimension=dimension; this.pos=pos;
    }
}
