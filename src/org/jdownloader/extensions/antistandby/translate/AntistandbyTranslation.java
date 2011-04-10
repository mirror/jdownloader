package org.jdownloader.extensions.antistandby.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface AntistandbyTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Simply prevents your computer from going into Stand By or Sleep Mode to keep downloads active." })
String jd_plugins_optional_antistandby_jdantistandby_description();
@Default(lngs = { "en" }, values = { "AntiStandBy" })
String jd_plugins_optional_antistandby_jdantistandby();
@Default(lngs = { "en" }, values = { "Mode:" })
String gui_config_antistandby_mode();
@Default(lngs = { "en" }, values = { "Prevent standby while JD is running" })
String gui_config_antistandby_whilejd();
@Default(lngs = { "en" }, values = { "Prevent standby while Downloading" })
String gui_config_antistandby_whiledl();
}