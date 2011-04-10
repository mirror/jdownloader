package jd.controlling.reconnect.plugins.liveheader.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface LiveheaderTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Import Router" })
String gui_config_liveheader_dialog_importrouter();
@Default(lngs = { "en" }, values = { "Save" })
String jd_controlling_reconnect_plugins_liveheader_LiveHeaderReconnect_actionPerformed_save();
@Default(lngs = { "en" }, values = { "Search Router Model" })
String gui_config_reconnect_selectrouter();
@Default(lngs = { "en" }, values = { "Success!" })
String gui_config_jdrr_success();
@Default(lngs = { "en" }, values = { "RawMode?" })
String gui_config_jdrr_rawmode();
@Default(lngs = { "en" }, values = { "Reconnect successfully recorded!" })
String jd_router_reconnectrecorder_Gui_icon_good();
@Default(lngs = { "en" }, values = { "JDRRPopup" })
String gui_config_jdrr_popup_title();
@Default(lngs = { "en" }, values = { "Web browser window with the home page of the router opens" })
String jd_nrouter_recorder_Gui_info2();
@Default(lngs = { "en" }, values = { "After the Reconnection hit the Stop button and save" })
String jd_nrouter_recorder_Gui_info3();
@Default(lngs = { "en" }, values = { "Test Script %s1/%s2: %s3" })
String jd_controlling_reconnect_plugins_liveheader_LiveHeaderDetectionWizard_runTests(Object s1, Object s2, Object s3);
@Default(lngs = { "en" }, values = { "Help" })
String gui_btn_help();
@Default(lngs = { "en" }, values = { "IP-Check disabled!" })
String jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_title();
@Default(lngs = { "en" }, values = { "Check the IP address of the router and press the Start button" })
String jd_nrouter_recorder_Gui_info1();
@Default(lngs = { "en" }, values = { "Recording Status" })
String gui_config_jdrr_status_title();
@Default(lngs = { "en" }, values = { "Error while recording the Reconnect!" })
String jd_router_reconnectrecorder_Gui_icon_bad();
@Default(lngs = { "en" }, values = { "Example: 3Com ADSL" })
String gui_config_reconnect_selectrouter_example();
@Default(lngs = { "en" }, values = { "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!" })
String gui_config_liveHeader_warning_noCURLConvert();
@Default(lngs = { "en" }, values = { "You disabled the IP-Check. This will increase the reconnection times dramatically!\r\n\r\nSeveral further modules like Reconnect Recorder are disabled." })
String jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_message();
@Default(lngs = { "en" }, values = { "Can't find your routers hostname" })
String gui_config_routeripfinder_notfound();
@Default(lngs = { "en" }, values = { "Search for routers hostname..." })
String gui_config_routeripfinder_featchIP();
@Default(lngs = { "en" }, values = { "Hostname found: %s1" })
String gui_config_routeripfinder_ready(Object s1);
@Default(lngs = { "en" }, values = { "Start" })
String gui_btn_start();
@Default(lngs = { "en" }, values = { "Reconnect Recorder" })
String gui_config_jdrr_title();
@Default(lngs = { "en" }, values = { "Recording Reconnect ..." })
String jd_router_reconnectrecorder_Gui_icon_progress();
@Default(lngs = { "en" }, values = { "Cancel" })
String gui_btn_cancel();
@Default(lngs = { "en" }, values = { "No" })
String gui_btn_no();
@Default(lngs = { "en" }, values = { "Abort" })
String gui_btn_abort();
@Default(lngs = { "en" }, values = { "RouterIP" })
String gui_fengshuiconfig_routerip();
@Default(lngs = { "en" }, values = { "Reconnection was successful. Save now?" })
String gui_config_jdrr_savereconnect();
@Default(lngs = { "en" }, values = { "Reconnect failed" })
String gui_config_jdrr_reconnectfaild();
@Default(lngs = { "en" }, values = { "Yes" })
String gui_btn_yes();
}