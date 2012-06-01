package org.jdownloader.extensions.jdpremclient.translate;
import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;
@Defaults(lngs = { "en"})
public interface JdpremclientTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "JDPremium" })
String jd_plugins_optional_jdpremium_name();
}