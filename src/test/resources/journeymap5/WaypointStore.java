package journeymap.client.waypoint;
import java.util.*;
public enum WaypointStore {
    INSTANCE;
    public static Error failure;
    public final Map<String,Waypoint> values=new LinkedHashMap<>();
    public final Map<String,Waypoint> files=new LinkedHashMap<>();
    public boolean hasLoaded(){return true;}
    public Collection<Waypoint> getAll(){return values.values();}
    public void save(Waypoint value,boolean persist){if(failure!=null)throw failure;values.put(value.getId(),value);files.put(value.getFileName(),value);}
    public void remove(Waypoint value,boolean persist){values.remove(value.getId());files.remove(value.getFileName());}
    public void reload(){values.clear();for(Waypoint value:files.values())values.put(value.getId(),value);}
}
