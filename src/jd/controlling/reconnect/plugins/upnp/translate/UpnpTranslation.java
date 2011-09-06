package jd.controlling.reconnect.plugins.upnp.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface UpnpTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Successful" })
    String jd_controlling_reconnect_plugins_upnp_UPNPRouterPlugin_autoFind_successdialog_title();

    @Default(lngs = { "en" }, values = { "JD set up the reconnection settings successfully!\r\n\r\nYour Router is \r\n'%s1'" })
    String jd_controlling_reconnect_plugins_upnp_UPNPRouterPlugin_autoFind_successdialog_message(Object s1);

    @Default(lngs = { "en" }, values = { "Could not find any working UPNP Routers" })
    String jd_controlling_reconnect_plugins_upnp_UPNPRouterPlugin_autoFind_faileddialog_message();

    @Default(lngs = { "en" }, values = { "Internet Protocol" })
    String interaction_UpnpReconnect_wanservice_ip();

    @Default(lngs = { "en" }, values = { "Point-to-Point Protocol" })
    String interaction_UpnpReconnect_wanservice_ppp();

    @Default(lngs = { "en" }, values = { "Failed" })
    String jd_controlling_reconnect_plugins_upnp_UPNPRouterPlugin_autoFind_faileddialog_title();

    @Default(lngs = { "en" }, values = { "Enter Service Type (e.g. urn:schemas-upnp-org:service:ConnectionManager:1)" })
    String servicetype_help();

    @Default(lngs = { "en" }, values = { "Enter Control URL ... " })
    String controlURLTxt_help();

    @Default(lngs = { "en" }, values = { "Service Type" })
    String literally_service_type();

    @Default(lngs = { "en" }, values = { "Control URL" })
    String literally_control_url();

    @Default(lngs = { "en" }, values = { "UPNP Router" })
    String literally_router();

    @Default(lngs = { "en" }, values = { "Choose Device" })
    String literally_choose_router();

    @Default(lngs = { "en" }, values = { "Auto Setup" })
    String auto();

    @Default(lngs = { "en" }, values = { "UPNP Router Reconnect" })
    String UPNPReconnectInvoker_getName_();

    @Default(lngs = { "en" }, values = { "Could not find any UPNP Devices. Try Live Header Reconnect instead!" })
    String UPNPRouterPlugin_run_error();

    @Default(lngs = { "en" }, values = { "UPNP Router Wizard" })
    String UPNPRouterPlugin_run_wizard_title();

    @Default(lngs = { "en" }, values = { "Scanning all network interfaces" })
    String UPNPRouterPlugin_run_mesg();

    @Default(lngs = { "en" }, values = { "Optimize: %s1" })
    String AutoDetectAction_run_optimize(String name);

    @Default(lngs = { "en" }, values = { "Found UPNP Devices, but could not perform a reconnect. \r\nTry Live Header Reconnect instead!" })
    String AutoDetectAction_run_failed();

}