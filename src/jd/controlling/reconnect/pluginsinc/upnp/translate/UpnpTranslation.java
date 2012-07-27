package jd.controlling.reconnect.pluginsinc.upnp.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface UpnpTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Internet Protocol" })
    String interaction_UpnpReconnect_wanservice_ip();

    @Default(lngs = { "en" }, values = { "Point-to-Point Protocol" })
    String interaction_UpnpReconnect_wanservice_ppp();

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

    @Default(lngs = { "en" }, values = { "UPNP Auto Setup" })
    String auto();

    @Default(lngs = { "en" }, values = { "UPNP Router Reconnect" })
    String UPNPReconnectInvoker_getName_();

    @Default(lngs = { "en" }, values = { "Could not find any UPNP Devices. Try Live Header Reconnect instead!" })
    String UPNPRouterPlugin_run_error();

    @Default(lngs = { "en" }, values = { "UPNP Router Wizard" })
    String UPNPRouterPlugin_run_wizard_title();

    @Default(lngs = { "en" }, values = { "Scanning all network interfaces" })
    String UPNPRouterPlugin_run_mesg();

    @Default(lngs = { "en" }, values = { "Found UPNP Devices, but could not perform a reconnect. \r\nTry Live Header Reconnect instead!" })
    String AutoDetectAction_run_failed();

    @Default(lngs = { "en" }, values = { "Tries to find all UPNP Reconnect Devices in your Network.\r\nMost FritzBox Routers can be reonnected this way." })
    String AutoDetectUpnpAction_AutoDetectUpnpAction_();

    @Default(lngs = { "en" }, values = { "Tries to find all UPNP Reconnect Devices in your Network, \r\nand lets you choose which one to use for Reconnet" })
    String UPNPScannerAction_UPNPScannerAction_tt();

    @Default(lngs = { "en" }, values = { "Try Upnp Reconnect: %s1" })
    String try_reconnect(String friendlyname);

    @Default(lngs = { "en" }, values = { "Control URL is invalid. Try UPNP Autosetup." })
    String malformedurl();

    @Default(lngs = { "en" }, values = { "Service Type is invalid. Try UPNP Autosetup." })
    String malformedservicetype();

}