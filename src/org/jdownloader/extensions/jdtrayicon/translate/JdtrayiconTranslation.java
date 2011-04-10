package org.jdownloader.extensions.jdtrayicon.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface JdtrayiconTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Show info in TaskBar when minimized" })
String plugins_optional_JDLightTray_titleinfo();
@Default(lngs = { "en" }, values = { "Downloads:" })
String plugins_optional_trayIcon_downloads();
@Default(lngs = { "en" }, values = { "Finished:" })
String plugins_optional_trayIcon_dl_finished();
@Default(lngs = { "en" }, values = { "Progress:" })
String plugins_optional_trayIcon_progress();
@Default(lngs = { "en" }, values = { "Enter the Password to open JD:" })
String plugins_optional_JDLightTray_enterPassword();
@Default(lngs = { "en" }, values = { "Speed:" })
String plugins_optional_trayIcon_speed();
@Default(lngs = { "en" }, values = { "Running:" })
String plugins_optional_trayIcon_dl_running();
@Default(lngs = { "en" }, values = { "Start minimized" })
String plugins_optional_JDLightTray_startMinimized();
@Default(lngs = { "en" }, values = { "Speed Limit (KiB/s) [0 = Infinite]" })
String gui_tooltip_statusbar_speedlimiter();
@Default(lngs = { "en" }, values = { "Toggle window status with single click" })
String plugins_optional_JDLightTray_singleClick();
@Default(lngs = { "en" }, values = { "Simultaneous downloads" })
String plugins_trayicon_popup_bottom_simdls();
@Default(lngs = { "en" }, values = { "Enter Password to open from Tray" })
String plugins_optional_JDLightTray_passwordRequired();
@Default(lngs = { "en" }, values = { "Light Tray" })
String jd_plugins_optional_jdtrayicon_jdlighttray();
@Default(lngs = { "en" }, values = { "The entered Password was wrong!" })
String plugins_optional_JDLightTray_enterPassword_wrong();
@Default(lngs = { "en" }, values = { "Password:" })
String plugins_optional_JDLightTray_password();
@Default(lngs = { "en" }, values = { "Speed limit(KiB/s)" })
String plugins_trayicon_popup_bottom_speed();
@Default(lngs = { "en" }, values = { "ETA:" })
String plugins_optional_trayIcon_eta();
@Default(lngs = { "en" }, values = { "Show Tooltip" })
String plugins_optional_JDLightTray_tooltip();
@Default(lngs = { "en" }, values = { "Total:" })
String plugins_optional_trayIcon_dl_total();
@Default(lngs = { "en" }, values = { "Allows minimizing or closing JDownloader to the notification area, and other related features like password protection and more." })
String jd_plugins_optional_jdtrayicon_jdlighttray_description();
@Default(lngs = { "en" }, values = { "Close to tray" })
String plugins_optional_JDLightTray_closetotray();
@Default(lngs = { "en" }, values = { "Concurrent Connections" })
String plugins_trayicon_popup_bottom_simchunks();
@Default(lngs = { "en" }, values = { "Maximum simultaneous Downloads [1..20]" })
String gui_tooltip_statusbar_simultan_downloads();
@Default(lngs = { "en" }, values = { "Max. Connections/File" })
String gui_tooltip_statusbar_max_chunks();
@Default(lngs = { "en" }, values = { "Show on Linkgrabbing (always)" })
String plugins_optional_JDLightTray_linkgrabber_always();
@Default(lngs = { "en" }, values = { "Show on Linkgrabbing (when minimized as trayicon)" })
String plugins_optional_JDLightTray_linkgrabber_intray();
}