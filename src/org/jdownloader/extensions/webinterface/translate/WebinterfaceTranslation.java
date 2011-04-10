package org.jdownloader.extensions.webinterface.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface WebinterfaceTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Refresh Interval" })
String plugins_optional_webinterface_refresh_interval();
@Default(lngs = { "en" }, values = { "Status" })
String gui_linkinfo_status();
@Default(lngs = { "en" }, values = { "Comment" })
String gui_linkinfo_comment();
@Default(lngs = { "en" }, values = { "is in process" })
String gui_linkinfo_download_underway();
@Default(lngs = { "en" }, values = { "Need User Authentication" })
String plugins_optional_webinterface_needlogin();
@Default(lngs = { "en" }, values = { "Wait time" })
String gui_linkinfo_waittime();
@Default(lngs = { "en" }, values = { "AutoRefresh" })
String plugins_optional_webinterface_refresh();
@Default(lngs = { "en" }, values = { "Chunks" })
String linkinformation_download_chunks_label();
@Default(lngs = { "en" }, values = { "Access only from this Computer" })
String plugins_optional_webinterface_localhostonly();
@Default(lngs = { "en" }, values = { "%s1 sec" })
String gui_linkinfo_secs(Object s1);
@Default(lngs = { "en" }, values = { "is not in process" })
String gui_linkinfo_download_notunderway();
@Default(lngs = { "en" }, values = { "Webinterface" })
String jd_plugins_optional_webinterface_jdwebinterface();
@Default(lngs = { "en" }, values = { "Login Name" })
String plugins_optional_webinterface_loginname();
@Default(lngs = { "en" }, values = { "Connection" })
String download_chunks_connection();
@Default(lngs = { "en" }, values = { "Password" })
String gui_linkinfo_password();
@Default(lngs = { "en" }, values = { "Package" })
String gui_linkinfo_package();
@Default(lngs = { "en" }, values = { "Aborted" })
String linkinformation_download_aborted();
@Default(lngs = { "en" }, values = { "" })
String jd_plugins_optional_webinterface_jdwebinterface_description();
@Default(lngs = { "en" }, values = { "File is OK" })
String gui_linkinfo_available_ok();
@Default(lngs = { "en" }, values = { "Save to" })
String gui_linkinfo_saveto();
@Default(lngs = { "en" }, values = { "Port" })
String plugins_optional_webinterface_port();
@Default(lngs = { "en" }, values = { "is activated" })
String gui_linkinfo_download_activated();
@Default(lngs = { "en" }, values = { "Error!" })
String linkinformation_available_error();
@Default(lngs = { "en" }, values = { "Not checked" })
String gui_linkinfo_available_notchecked();
@Default(lngs = { "en" }, values = { "Use HTTPS" })
String plugins_optional_webinterface_https();
@Default(lngs = { "en" }, values = { "Filesize" })
String gui_linkinfo_filesize();
@Default(lngs = { "en" }, values = { "is deactivated" })
String gui_linkinfo_download_deactivated();
@Default(lngs = { "en" }, values = { "Speed" })
String gui_linkinfo_speed();
@Default(lngs = { "en" }, values = { "Available" })
String gui_linkinfo_available();
@Default(lngs = { "en" }, values = { "Login Pass" })
String plugins_optional_webinterface_loginpass();
@Default(lngs = { "en" }, values = { "Download" })
String gui_linkinfo_download();
@Default(lngs = { "en" }, values = { "Active" })
String gui_treetable_packagestatus_links_active();
}