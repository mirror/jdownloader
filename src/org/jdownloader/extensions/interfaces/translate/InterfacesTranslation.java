package org.jdownloader.extensions.interfaces.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface InterfacesTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Deny access!" })
String jd_plugins_optional_interfaces_jdflashgot_security_btn_deny();
@Default(lngs = { "en" }, values = { "An external application tries to add links. See Log for details." })
String jd_plugins_optional_interfaces_jdflashgot_security_message();
@Default(lngs = { "en" }, values = { "Do you want to update to JD-%s1" })
String updater_beta_rlyupdate_message(Object s1);
@Default(lngs = { "en" }, values = { "External request from %s1 to %s2 interface!" })
String jd_plugins_optional_interfaces_jdflashgot_security_title(Object s1, Object s2);
@Default(lngs = { "en" }, values = { "Allow it!" })
String jd_plugins_optional_interfaces_jdflashgot_security_btn_allow();
@Default(lngs = { "en" }, values = { "Update to beta now?" })
String updater_beta_rlyupdate_title();
@Default(lngs = { "en" }, values = { "Listen only on localhost?" })
String jd_plugins_optional_interfaces_JDExternInterface_localonly();
}