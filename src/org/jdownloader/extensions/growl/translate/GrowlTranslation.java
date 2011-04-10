package org.jdownloader.extensions.growl.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface GrowlTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "All downloads stopped" })
String jd_plugins_optional_JDGrowlNotification_allfinished();
@Default(lngs = { "en" }, values = { "Growl Notification" })
String jd_plugins_optional_jdgrowlnotification_description();
@Default(lngs = { "en" }, values = { "Growl Notification" })
String jd_plugins_optional_jdgrowlnotification();
@Default(lngs = { "en" }, values = { "jDownloader started..." })
String jd_plugins_optional_JDGrowlNotification_started();
@Default(lngs = { "en" }, values = { "Download stopped" })
String jd_plugins_optional_JDGrowlNotification_finished();
}