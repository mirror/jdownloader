package jd.controlling.reconnect.plugins.extern.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface ExternTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "External Tool Reconnect" })
String jd_controlling_reconnect_plugins_extern_ExternReconnectPlugin_getName();
@Default(lngs = { "en" }, values = { "Parameter (1 parameter every line)" })
String interaction_externreconnect_parameter();
@Default(lngs = { "en" }, values = { "Command (use absolute directory paths)" })
String interaction_externreconnect_command();
@Default(lngs = { "en" }, values = { "Use special executer for windows" })
String interaction_externreconnect_dummybat();
}