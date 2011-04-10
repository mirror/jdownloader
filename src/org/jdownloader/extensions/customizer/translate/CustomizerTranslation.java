package org.jdownloader.extensions.customizer.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface CustomizerTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Use SubDirectory" })
    String jd_plugins_optional_customizer_CustomizerTableModel_subDirectory();

    @Default(lngs = { "en" }, values = { "Customized with the Regex %s" })
    String jd_plugins_optional_customizer_JDPackageCustomizer_customized();

    @Default(lngs = { "en" }, values = { "Regex" })
    String jd_plugins_optional_customizer_CustomizerTableModel_regex();

    @Default(lngs = { "en" }, values = { "Please insert the name for the new Setting:" })
    String action_customize_addsetting_ask();

    @Default(lngs = { "en" }, values = { "Remove selected Setting(s)? (%s1 Account(s))" })
    String action_customize_removesetting_ask(Object s1);

    @Default(lngs = { "en" }, values = { "Match count from Start" })
    String jd_plugins_optional_customizer_CustomizerTableModel_matchCount();

    @Default(lngs = { "en" }, values = { "Package Customizer" })
    String jd_plugins_optional_customizer_jdpackagecustomizer();

    @Default(lngs = { "en" }, values = { "FilePackage name" })
    String jd_plugins_optional_customizer_CustomizerTableModel_packageName();

    @Default(lngs = { "en" }, values = { "Post Processing" })
    String jd_plugins_optional_customizer_CustomizerTableModel_postProcessing();

    @Default(lngs = { "en" }, values = { "Insert examplelinks here to highlight the matched setting:" })
    String jd_plugins_optional_customizer_CustomizerGui_tester();

    @Default(lngs = { "en" }, values = { "On URL?" })
    String jd_plugins_optional_customizer_CustomizerTableModel_onurl();

    @Default(lngs = { "en" }, values = { "Password" })
    String jd_plugins_optional_customizer_CustomizerTableModel_password();

    @Default(lngs = { "en" }, values = { "Malformed Regex!" })
    String jd_plugins_optional_customizer_CustomizerTableModel_regex_malformed();

    @Default(lngs = { "en" }, values = { "Download directory" })
    String jd_plugins_optional_customizer_CustomizerTableModel_downloadDir();

    @Default(lngs = { "en" }, values = { "Customize your FilePackages" })
    String jd_plugins_optional_customizer_CustomizerView_tooltip();

    @Default(lngs = { "en" }, values = { "The name of the filepackage, if the link matches the regex. Leave it empty to use the default name!" })
    String jd_plugins_optional_customizer_columns_PackageNameColumn_toolTip();

    @Default(lngs = { "en" }, values = { "Download Priority" })
    String jd_plugins_optional_customizer_CustomizerTableModel_dlPriority();

    @Default(lngs = { "en" }, values = { "Name" })
    String jd_plugins_optional_customizer_CustomizerTableModel_name();

    @Default(lngs = { "en" }, values = { "Package Customizer" })
    String jd_plugins_optional_customizer_jdpackagecustomizer_description();

    @Default(lngs = { "en" }, values = { "Enabled" })
    String jd_plugins_optional_customizer_CustomizerTableModel_enabled();

    @Default(lngs = { "en" }, values = { "Package Customizer" })
    String jd_plugins_optional_customizer_CustomizerView_title();
}