package org.jdownloader.api.cnl2.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;
import org.appwork.txtresource.TranslationFactory;

@Defaults(lngs = { "en" })
public interface ExternInterfaceTranslation extends TranslateInterface {

    public static final ExternInterfaceTranslation _ = TranslationFactory.create(ExternInterfaceTranslation.class);

    @Default(lngs = { "en" }, values = { "Deny access!" })
    String jd_plugins_optional_interfaces_jdflashgot_security_btn_deny();

    @Default(lngs = { "en" }, values = { "An external application tries to add links" })
    String jd_plugins_optional_interfaces_jdflashgot_security_message();

    @Default(lngs = { "en" }, values = { "External request from %s1!" })
    String jd_plugins_optional_interfaces_jdflashgot_security_title(String s1);

    @Default(lngs = { "en" }, values = { "Allow it!" })
    String jd_plugins_optional_interfaces_jdflashgot_security_btn_allow();

}
