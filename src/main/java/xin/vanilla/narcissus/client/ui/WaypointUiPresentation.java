package xin.vanilla.narcissus.client.ui;

@lombok.Value
@lombok.experimental.Accessors(fluent = true)
@lombok.AllArgsConstructor
public class WaypointUiPresentation {
    String name;
    String dimension;
    String dimensionId;
    String x;
    String y;
    String z;
    String distance;
    String created;
    String cost;
    String note;
    String empty;
    String footer;
    boolean confirming;
    boolean history;

}
