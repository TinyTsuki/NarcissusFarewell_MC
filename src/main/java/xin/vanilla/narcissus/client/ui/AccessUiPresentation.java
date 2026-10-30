package xin.vanilla.narcissus.client.ui;

public record AccessUiPresentation(String name, String uuid, String mode, String scope,
                                   String tpa, String tph, String empty, String footer,
                                   boolean selected, boolean confirming, boolean whitelist) {
}
