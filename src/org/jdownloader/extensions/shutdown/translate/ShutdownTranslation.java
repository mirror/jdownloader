package org.jdownloader.extensions.shutdown.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface ShutdownTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Mode:" })
    String gui_config_jdshutdown_mode();

    @Default(lngs = { "en" }, values = { "The System will NOT be shut down by JDownloader." })
    String addons_jdshutdown_statusmessage_disabled();

    @Default(lngs = { "en" }, values = { "<h2><font color=\"red\">System will be put into standby mode!</font></h2>" })
    String interaction_shutdown_dialog_msg_standby();

    @Default(lngs = { "en" }, values = { "Hibernate?" })
    String interaction_shutdown_dialog_title_hibernate();

    @Default(lngs = { "en" }, values = { "Enabled on Start" })
    String gui_config_jdshutdown_enabledOnStart();

    @Default(lngs = { "en" }, values = { "Close JD" })
    String gui_config_jdshutdown_close();

    @Default(lngs = { "en" }, values = { "Hibernate (Not for all OS)" })
    String gui_config_jdshutdown_hibernate();

    @Default(lngs = { "en" }, values = { "Standby?" })
    String interaction_shutdown_dialog_title_standby();

    @Default(lngs = { "en" }, values = { "<h2><font color=\"red\">System will be put into hibernate mode!</font></h2>" })
    String interaction_shutdown_dialog_msg_hibernate();

    @Default(lngs = { "en" }, values = { "Standby (Not for all OS)" })
    String gui_config_jdshutdown_standby();

    @Default(lngs = { "en" }, values = { "JD Shutdown" })
    String jd_plugins_optional_jdshutdown();

    @Default(lngs = { "en" }, values = { "Shutdown" })
    String gui_config_jdshutdown_shutdown();

    @Default(lngs = { "en" }, values = { "Shutdown?" })
    String interaction_shutdown_dialog_title_shutdown();

    @Default(lngs = { "en" }, values = { "<h2><font color=\"red\">JDownloader will be closed!</font></h2>" })
    String interaction_shutdown_dialog_msg_closejd();

    @Default(lngs = { "en" }, values = { "Automatically shut down, suspend, hibernate your PC or close JDownloader when downloads are finished." })
    String jd_plugins_optional_jdshutdown_description();

    @Default(lngs = { "en" }, values = { "JDownloader will shut down your System after downloads finished." })
    String addons_jdshutdown_statusmessage_enabled();

    @Default(lngs = { "en" }, values = { "Force Shutdown (Not for all OS)" })
    String gui_config_jdshutdown_forceshutdown();

    @Default(lngs = { "en" }, values = { "Install" })
    String gui_config_jdshutdown_osx_force_short();

    @Default(lngs = { "en" }, values = { "<h2><font color=\"red\">System will be shut down!</font></h2>" })
    String interaction_shutdown_dialog_msg_shutdown();

    @Default(lngs = { "en" }, values = { "Close JD?" })
    String interaction_shutdown_dialog_title_closejd();

    @Default(lngs = { "en" }, values = { "Install Force Shutdown (only Mac OSX)" })
    String gui_config_jdshutdown_osx_force_long();

    @Default(lngs = { "en" }, values = { "Shutdown" })
    String lit_shutdownn();

    @Default(lngs = { "en" }, values = { "Enable to Shutdown the System after Downloads have finished" })
    String action_tooltip();

    @Default(lngs = { "en" }, values = { "Toolbar Button visible" })
    String config_toolbarbutton();

    @Default(lngs = { "en" }, values = { "Install Force Shutdown" })
    String install_force();

    @Default(lngs = { "en" }, values = { "Install Force Shutdown" })
    String install_title();

    @Default(lngs = { "en" }, values = { "Do you want to prepare your System for the force shutdown feature?" })
    String install_msg();

    @Default(lngs = { "en" }, values = { "Administrator Rights are required to setup JDownloader for Hibernateor Standby Mode" })
    String show_admin();
}