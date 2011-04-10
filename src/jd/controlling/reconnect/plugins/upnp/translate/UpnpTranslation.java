package jd.controlling.reconnect.plugins.upnp.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
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
}