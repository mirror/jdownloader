package org.jdownloader.extensions.chat.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface ChatTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Loading Message of the day" })
String jd_plugins_optional_jdchat_JDChat_topic_default();
@Default(lngs = { "en" }, values = { "Close Tab" })
String jd_plugins_optional_jdchat_closeTab();
@Default(lngs = { "en" }, values = { "Upload of logfile failed!" })
String sys_warning_loguploadfailed();
@Default(lngs = { "en" }, values = { "Your wished nickname?" })
String plugins_optional_jdchat_enternick();
@Default(lngs = { "en" }, values = { "%s1 needs a log to solve your problem. Do you agree to send him the Log?" })
String plugin_optional_jdchat_getlog(Object s1);
@Default(lngs = { "en" }, values = { "Message of the day" })
String jd_plugins_optional_jdchat_JDChat_topic_tooltip();
@Default(lngs = { "en" }, values = { "Chat" })
String jd_plugins_optional_jdchat_jdchat();
@Default(lngs = { "en" }, values = { "JD Support Chat" })
String jd_plugins_optional_jdchat_JDChatView_tooltip();
@Default(lngs = { "en" }, values = { "New Message from %s1:<hr> %s2" })
String jd_plugins_optional_jdchat_newmessage(Object s1, Object s2);
@Default(lngs = { "en" }, values = { "JD Support Chat" })
String jd_plugins_optional_jdchat_JDChatView_title();
}