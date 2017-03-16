package org.jdownloader.extensions.antistandby.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface AntistandbyTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Simply prevents your computer from going into Stand By or Sleep Mode to keep downloads active." })
    String jd_plugins_optional_antistandby_jdantistandby_description();

    @Default(lngs = { "en" }, values = { "AntiStandBy" })
    String jd_plugins_optional_antistandby_jdantistandby();

    @Default(lngs = { "en" }, values = { "JDownloader is running" })
    String gui_config_antistandby_whilejd2();

    @Default(lngs = { "en" }, values = { "Download is in progress" })
    String gui_config_antistandby_whiledl2();

    @Default(lngs = { "en" }, values = { "Crawler is in progress" })
    String gui_config_antistandby_whilecrawl();

    @Default(lngs = { "en" }, values = { "Download or Crawler is in progress" })
    String gui_config_antistandby_whiledl2orcrawl();

    @Default(lngs = { "en" }, values = { "Prevent standby/sleep when" })
    String mode();

    @Default(lngs = { "en" }, values = { "Prevent screensaver" })
    String prevent_screensaver();
}