package jd.plugins.optional.antireconnect;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface JDAntiReconnectTranslation extends TranslateInterface {

    @Default(lngs = { "en", }, values = { "Disabled" })
    String mode_disabled();

    @Default(lngs = { "en", }, values = { "Detect only by Ping (faster)" })
    String mode_ping();

    @Default(lngs = { "en", }, values = { "Ping & ARP (recommended)" })
    String mode_arp();

    @Default(lngs = { "en", }, values = { "Anti Reconnect" })
    String name();

    @Default(lngs = { "en", }, values = { "Could not start addon. Error: %s1" })
    String start_failed(String message);

    @Default(lngs = { "en", }, values = { "Automaticly disables reconnect features of one ore more network conditions match." })
    String description();

}
