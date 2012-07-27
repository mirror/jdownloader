package jd.controlling.reconnect.pluginsinc.liveheader.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
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

    @Default(lngs = { "en" }, values = { "You disabled the IP-Check. This will increase the reconnection times dramatically!\r\n\r\nSeveral further modules like Reconnect Recorder are disabled." })
    String jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_message();

    @Default(lngs = { "en" }, values = { "Can't find your routers hostname" })
    String gui_config_routeripfinder_notfound();

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

    @Default(lngs = { "en" }, values = { "Scanning Connection Information" })
    String LiveHeaderDetectionWizard_runOnlineScan_collect();

    @Default(lngs = { "en" }, values = { "Get RouterIP" })
    String GetIPAction_GetIPAction_();

    @Default(lngs = { "en" }, values = { "Scanning Network..." })
    String GetIPAction_getString_progress();

    @Default(lngs = { "en" }, values = { "Search RouterIP" })
    String GetIPAction_actionPerformed_d_title();

    @Default(lngs = { "en" }, values = { "Searching Router IP" })
    String GetIPAction_actionPerformed_d_msg();

    @Default(lngs = { "en" }, values = { "Compare Router Information" })
    String DataCompareDialog_DataCompareDialog_();

    @Default(lngs = { "en" }, values = { "Please check the following information, and try to fill empty fields as accurate as possible." })
    String DataCompareDialog_layoutDialogContent__desc();

    @Default(lngs = { "en" }, values = { "Your Router" })
    String DataCompareDialog_layoutDialogContent_router();

    @Default(lngs = { "en" }, values = { "Router Web Interface" })
    String DataCompareDialog_layoutDialogContent_webinterface();

    @Default(lngs = { "en" }, values = { "To perform a reconnect, JDownloader needs the router's webinterface logins.\r\nVery often you can find the logins at the back of your router.\r\nIf Unknown, please ask your Network admin" })
    String DataCompareDialog_layoutDialogContent_webinterface_desc();

    @Default(lngs = { "en" }, values = { "Model Name" })
    String DataCompareDialog_layoutDialogContent_name();

    @Default(lngs = { "en" }, values = { "Enter your Router's name..." })
    String DataCompareDialog_layoutDialogContent_name_help();

    @Default(lngs = { "en" }, values = { "Manufactor" })
    String DataCompareDialog_layoutDialogContent_manufactorName();

    @Default(lngs = { "en" }, values = { "Enter your Router's Manufactor..." })
    String DataCompareDialog_layoutDialogContent_manufactorName_help();

    @Default(lngs = { "en" }, values = { "Firmware/Version" })
    String DataCompareDialog_layoutDialogContent_firmware();

    @Default(lngs = { "en" }, values = { "Enter your Router's Firmware version... leave empty if unknown" })
    String DataCompareDialog_layoutDialogContent_firmware_help();

    @Default(lngs = { "en" }, values = { "Username" })
    String DataCompareDialog_layoutDialogContent_user();

    @Default(lngs = { "en" }, values = { "Enter Webinterface Username..." })
    String DataCompareDialog_layoutDialogContent_user_help();

    @Default(lngs = { "en" }, values = { "Password" })
    String DataCompareDialog_layoutDialogContent_password();

    @Default(lngs = { "en" }, values = { "Enter Webinterface Password..." })
    String DataCompareDialog_layoutDialogContent_password_help();

    @Default(lngs = { "en" }, values = { "These information is very important. \r\nIn most cases, you can find your Router's name, manufactor and version on the back of your Router. \r\nAnother place to check is your Router's Webinterface." })
    String DataCompareDialog_layoutDialogContent_router_desc();

    @Default(lngs = { "en" }, values = { "IP or Hostname" })
    String DataCompareDialog_layoutDialogContent_ip();

    @Default(lngs = { "en" }, values = { "Enter Webinterface Hostname..." })
    String DataCompareDialog_layoutDialogContent_ip_help();

    @Default(lngs = { "en" }, values = { "Open Webinterface" })
    String DataCompareDialog_open_webinterface();

    @Default(lngs = { "en" }, values = { "It seems that there is no Webinterface at http://%s1. \r\nThe Router IP might be invalid. \r\n\r\nContinue anyway?" })
    String LiveHeaderDetectionWizard_runOnlineScan_warning_badip(String hostName);

    @Default(lngs = { "en" }, values = { "The Router IP '%s1' is invalid!" })
    String LiveHeaderDetectionWizard_runOnlineScan_warning_badhost(String hostName);

    @Default(lngs = { "en" }, values = { "Create New Script" })
    String ReconnectRecorderAction_ReconnectRecorderAction_();

    @Default(lngs = { "en" }, values = { "Edit Script" })
    String EditScriptAction_EditScriptAction_();

    @Default(lngs = { "en" }, values = { "Router Model" })
    String literally_router_model();

    @Default(lngs = { "en" }, values = { "Router IP" })
    String literally_router_ip();

    @Default(lngs = { "en" }, values = { "Username" })
    String literally_username();

    @Default(lngs = { "en" }, values = { "Password" })
    String literally_password();

    @Default(lngs = { "en" }, values = { "Enter Webinterface Password..." })
    String LiveHeaderReconnect_getGUI_help_password();

    @Default(lngs = { "en" }, values = { "Enter Webinterface Username..." })
    String LiveHeaderReconnect_getGUI_help_user();

    @Default(lngs = { "en" }, values = { "Enter Router's IP, or click [Find RouterIP]..." })
    String LiveHeaderReconnect_getGUI_help_ip();

    @Default(lngs = { "en" }, values = { "Autodetection of Reconnect Settings needs to enable the IP Check Feature. Continue?" })
    String ipcheck();

    @Default(lngs = { "en" }, values = { "IP Check has been disabled now. We recommend to enabled global IP Checks. Otherwise your reconnects may fail." })
    String ipcheckreverted();

    @Default(lngs = { "en" }, values = { "LiveHeader: %s1" })
    String LiveHeaderInvoker_getName_(String name);

    @Default(lngs = { "en" }, values = { "JDownloader could not find a working LiveHeader Reconnect Script. \r\nPlease make sure that you entered correct information, and that you have a dynamic IP connection." })
    String AutoDetectAction_run_failed();

    @Default(lngs = { "en" }, values = { "Scans your Network and tries to find your Router's IP" })
    String GetIPAction_GetIPAction_tt();

    @Default(lngs = { "en" }, values = { "Current Router Ip Settings: %s1.Click here to scan your Network and validate this IP." })
    String GetIPAction_getTooltipText_tt_2(String ip);

    @Default(lngs = { "en" }, values = { "Enter an own Reconnect Script, or modify the existing one." })
    String EditScriptAction_EditScriptAction_tt();

    @Default(lngs = { "en" }, values = { "The Reconnect Recorder records all steps while \r\nyou perform a manual Reconnect by clicking the 'Reboot' or 'Disconnect-Connect' Button \r\nin your router's webinterface." })
    String ReconnectRecorderAction_ReconnectRecorderAction_tt();

    @Default(lngs = { "en" }, values = { "Logins will NOT be uploaded. Please enter them anyway. \r\nWe need them to remove any sensitive information form your reconnect settings before uploading.\r\nIf you don't know them, leave empty, or ask your Network Administrator." })
    String LiveHeaderDetectionWizard_userConfirm_loginstext();

    @Default(lngs = { "en" }, values = { "We noticed, that your script already exists in our database.\r\nThanks anyway." })
    String LiveHeaderDetectionWizard_uploadData_sent_failed();

    @Default(lngs = { "en" }, values = { "Thank you!\r\nWe added your script to our router reconnect database." })
    String LiveHeaderDetectionWizard_uploadData_sent_ok();

    @Default(lngs = { "en" }, values = { "Reconnect failed. You can only share working reconnect settings." })
    String LiveHeaderDetectionWizard_sendRouter_reconnectFailed();

    @Default(lngs = { "en" }, values = { "Share Reconnect Settings." })
    String LiveHeaderReconnect_onConfigValueModified_ask_title();

    @Default(lngs = { "en" }, values = { "You have set up your Reconnect successfully. Please share your settings to improve our Reconnect Wizard." })
    String LiveHeaderReconnect_onConfigValueModified_ask_msg();

    @Default(lngs = { "en" }, values = { "Service not Available" })
    String LiveHeaderDetectionWizard_runOnlineScan_notavailable_t();

    @Default(lngs = { "en" }, values = { "This service is currently not available.\r\nPlease try again later!" })
    String LiveHeaderDetectionWizard_runOnlineScan_notavailable_mm();

    @Default(lngs = { "en" }, values = { "Share Settings" })
    String RouterSendAction_RouterSendAction_();

    @Default(lngs = { "en" }, values = { "Share your settings with others.\r\nThis Wizard helps you to upload your Reconnect Settings to our Server. \r\nThis helps to improve autodetection for all others." })
    String RouterSendAction_RouterSendAction_tt();

    @Default(lngs = { "en" }, values = { "Share Reconnect Settings" })
    String RouterSendAction_actionPerformed_title();

    @Default(lngs = { "en" }, values = { "We need your help to improve our reconnect database.\r\nPlease contribute to the 'JD Project' and send in our reconnect script.\r\nThis wizard will guide you through all required steps." })
    String RouterSendAction_actionPerformed_msg2();

}