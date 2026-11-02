package xin.vanilla.narcissus.client.ui;

@lombok.Value
@lombok.experimental.Accessors(fluent = true)
@lombok.AllArgsConstructor
public class AccessUiPresentation {
    String name;
    String uuid;
    String mode;
    String scope;
    String tpa;
    String tph;
    String empty;
    String footer;
    boolean selected;
    boolean confirming;
    boolean whitelist;

}
