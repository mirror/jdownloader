package org.jdownloader.gui.jdtrayicon.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface TrayiconTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Downloads:" })
    String plugins_optional_trayIcon_downloads();

    @Default(lngs = { "en" }, values = { "Finished:" })
    String plugins_optional_trayIcon_dl_finished();

    @Default(lngs = { "en" }, values = { "Progress:" })
    String plugins_optional_trayIcon_progress();

    @Default(lngs = { "en" }, values = { "Speed:" })
    String plugins_optional_trayIcon_speed();

    @Default(lngs = { "en" }, values = { "Running:" })
    String plugins_optional_trayIcon_dl_running();

    @Default(lngs = { "en" }, values = { "Start minimized" })
    String plugins_optional_JDLightTray_startMinimized();

    @Default(lngs = { "en" }, values = { "Toggle window status with single click" })
    String plugins_optional_JDLightTray_singleClick();

    @Default(lngs = { "en" }, values = { "Enter Password to open from Tray" })
    String plugins_optional_JDLightTray_passwordRequired();

    @Default(lngs = { "en" }, values = { "Light Tray" })
    String jd_plugins_optional_jdtrayicon_jdlighttray();

    @Default(lngs = { "en" }, values = { "ETA:" })
    String plugins_optional_trayIcon_eta();

    @Default(lngs = { "en" }, values = { "Show Tooltip" })
    String plugins_optional_JDLightTray_tooltip();

    @Default(lngs = { "en" }, values = { "Total:" })
    String plugins_optional_trayIcon_dl_total();

    @Default(lngs = { "en" }, values = { "Allows minimizing or closing JDownloader to the notification area, and other related features like password protection and more." })
    String jd_plugins_optional_jdtrayicon_jdlighttray_description();

    @Default(lngs = { "en" }, values = { "Pause Downloads" })
    String popup_pause();

    @Default(lngs = { "en" }, values = { "Check for Updates" })
    String popup_update();

    @Default(lngs = { "en" }, values = { "Reconnect Now" })
    String popup_reconnect();

    @Default(lngs = { "en" }, values = { "Open Downloadfolder" })
    String popup_downloadfolder();

    @Default(lngs = { "en" }, values = { "Premium enabled" })
    String popup_premiumtoggle();

    @Default(lngs = { "en" }, values = { "Clipboard Observer enabled" })
    String popup_clipboardtoggle();

    @Default(lngs = { "en" }, values = { "Auto Reconnect enabled" })
    String popup_reconnecttoggle();

    @Default(lngs = { "en" }, values = { "Exit JDownloader" })
    String popup_exit();

    @Default(lngs = { "en" }, values = { "Close to Tray" })
    String OnCloseAction_totray();

    @Default(lngs = { "en" }, values = { "Minimize to Taskbar" })
    String OnCloseAction_totaskbar();

    @Default(lngs = { "en" }, values = { "Exit Application" })
    String OnCloseAction_exit();

    @Default(lngs = { "en" }, values = { "Close to Tray" })
    String OnMinimizeAction_totray();

    @Default(lngs = { "en" }, values = { "Minimize to Taskbar" })
    String OnMinimizeAction_totaskbar();

    @Default(lngs = { "en" }, values = { "Click on 'Minimize <_>' Action" })
    String plugins_optional_JDLightTray_minimizetotray();

    @Default(lngs = { "en" }, values = { "Click on 'Close <X>' Action" })
    String plugins_optional_JDLightTray_closetotray2();

    @Default(lngs = { "en" }, values = { "Ask me" })
    String OnMinimizeAction_ask();

    @Default(lngs = { "en" }, values = { "Close to Tray" })
    String JDGui_windowClosing_try_title_();

    @Default(lngs = { "en" }, values = { "Please choose between \r\n   - Exit JDownloader (Cancel all running processes)\r\n   - Minimize to Taskbar (Downloads will continue)\r\n   - Hide in System Tray (Downloads will continue in the background)" })
    String JDGui_windowClosing_try_msg_2();

    @Default(lngs = { "en" }, values = { "Hide to Tray" })
    String JDGui_windowClosing_try_answer_tray();

    @Default(lngs = { "en" }, values = { "Exit JDownloader" })
    String JDGui_windowClosing_try_asnwer_close();

    @Default(lngs = { "en" }, values = { "Minimize to Taskbar" })
    String JDGui_windowClosing_try_answer_totaskbar();

    @Default(lngs = { "en" }, values = { "Hide Tray if Window is visible" })
    String plugins_optional_JDLightTray_hideifframevisible();

    @Default(lngs = { "en" }, values = { "Tray Icon" })
    String getName();

    @Default(lngs = { "en" }, values = { "Tray Icon Manager" })
    String TrayMenuManager_getName();

    @Default(lngs = { "en" }, values = { "New Links added" })
    String balloon_new_links();

    @Default(lngs = { "en" }, values = { "%s2 Link(s) in %s1 Package(s)" })
    String balloon_new_links_msg(int packagcount, int childrenCount);

    @Default(lngs = { "en" }, values = { "New Package added" })
    String balloon_new_package();

    @Default(lngs = { "en" }, values = { "A new Package has been added to the Linkgrabber: %s1" })
    String balloon_new_package_msg(String name);

    @Default(lngs = { "en" }, values = { "Ballon Notifications" })
    String plugins_optional_JDLightTray_ballon();

    @Default(lngs = { "en" }, values = { "New Linkgrabber Packages" })
    String plugins_optional_JDLightTray_ballon_newPackages();

    @Default(lngs = { "en" }, values = { "Ballon Notifications are tiny messages that notify you if there is a special event you should know about. These messages only appear if JDownloader is not the focused Window!" })
    String plugins_optional_JDLightTray_ballon_desc();

    @Default(lngs = { "en" }, values = { "New Linkgrabber Links" })
    String plugins_optional_JDLightTray_ballon_newlinks();

}