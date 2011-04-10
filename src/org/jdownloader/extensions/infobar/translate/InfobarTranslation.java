package org.jdownloader.extensions.infobar.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface InfobarTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Opacity of the Dialog [in %]" })
String jd_plugins_optional_infobar_opacity();
@Default(lngs = { "en" }, values = { "Drop URLs, Hyperlinks or DLC files here!" })
String jd_plugins_optional_infobar_InfoDialog_help_tooltip2();
@Default(lngs = { "en" }, values = { "Small floating panel with information on Downloads in realtime, and Drag'N'Drop Zone where you can add links, URLs or DLC files." })
String jd_plugins_optional_infobar_jdinfobar_description();
@Default(lngs = { "en" }, values = { "Info Bar" })
String jd_plugins_optional_infobar_jdinfobar();
@Default(lngs = { "en" }, values = { "Hide InfoBar" })
String jd_plugins_optional_infobar_InfoDialog_hideWindow();
@Default(lngs = { "en" }, values = { "Enable Docking to the Sides" })
String jd_plugins_optional_infobar_docking();
@Default(lngs = { "en" }, values = { "Drag'N'Drop Zone" })
String jd_plugins_optional_infobar_InfoDialog_help();
@Default(lngs = { "en" }, values = { "Enable Drop Location" })
String jd_plugins_optional_infobar_dropLocation2();
}