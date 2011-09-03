package org.jdownloader.extensions.antireconnect.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface AntireconnectTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "If Other Clients are Online" })
    String gui_config_antireconnect_newgroup();

    @Default(lngs = { "en" }, values = { "Check Timeout (ms):" })
    String gui_config_antireconnect_timeout();

    @Default(lngs = { "en" }, values = { "Mode:" })
    String gui_config_antireconnect_mode();

    @Default(lngs = { "en" }, values = { "Simultanious Downloads:" })
    String gui_config_antireconnect_olddownloads();

    @Default(lngs = { "en" }, values = { "Check Each (ms):" })
    String gui_config_antireconnect_each();

    @Default(lngs = { "en" }, values = { "Downloadspeed in kb/s" })
    String gui_config_antireconnect_newspeed();

    @Default(lngs = { "en" }, values = { "Normally" })
    String gui_config_antireconnect_oldgroup();

    @Default(lngs = { "en" }, values = { "Check Ips (192.168.1.20-80)" })
    String gui_config_antireconnect_ips();

    @Default(lngs = { "en" }, values = { "Allow Reconnect:" })
    String gui_config_antireconnect_newreconnect();

    @Default(lngs = { "en" }, values = { "Allow Reconnect:" })
    String gui_config_antireconnect_oldreconnect();

    @Default(lngs = { "en" }, values = { "Anti Reconnect" })
    String jd_plugins_optional_antireconnect_jdantireconnect();

    @Default(lngs = { "en" }, values = { "Downloadspeed in kb/s" })
    String gui_config_antireconnect_oldspeed();

    @Default(lngs = { "en" }, values = { "Simultanious Downloads:" })
    String gui_config_antireconnect_newdownloads();

    @Default(lngs = { "en" }, values = { "Disabled" })
    String mode_disabled();

    @Default(lngs = { "en" }, values = { "Detect only by Ping (faster)" })
    String mode_ping();

    @Default(lngs = { "en" }, values = { "Ping & ARP (recommended)" })
    String mode_arp();

    @Default(lngs = { "en" }, values = { "Anti Reconnect" })
    String name();

    @Default(lngs = { "en" }, values = { "Could not start addon. Error: %s1" })
    String start_failed(String message);

    @Default(lngs = { "en" }, values = { "Automaticly disables reconnect features of one ore more network conditions match." })
    String description();

    @Default(lngs = { "en" }, values = { "Auto Setup" })
    String AutoDetectAction_AutoDetectAction_();

    @Default(lngs = { "en" }, values = { "Auto Setup Reconnect" })
    String AutoDetectAction_actionPerformed_d_title();

    @Default(lngs = { "en" }, values = { "Please wait. JDownloader tries to find the correct settings for your internet connection. This may take several minutes." })
    String AutoDetectAction_actionPerformed_d_msg();

    @Default(lngs = { "en" }, values = { "Share your Reconnect Settings" })
    String RouterSendAction_actionPerformed_();

    @Default(lngs = { "en" }, values = { "JDownloader detected that you have successfully set up your reconnection settings.\r\nPlease follow this wizard and share your settings with others.\r\nThis will only take a few seconds." })
    String RouterSendAction_actionPerformed_msg();

}