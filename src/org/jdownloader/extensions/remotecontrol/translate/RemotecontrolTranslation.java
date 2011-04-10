package org.jdownloader.extensions.remotecontrol.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface RemotecontrolTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Port:" })
String plugins_optional_RemoteControl_port();
@Default(lngs = { "en" }, values = { "%s1 started on port %s2\nhttp://127.0.0.1:%s3\n/help for Developer Information." })
String plugins_optional_remotecontrol_startedonport2(Object s1, Object s2, Object s3);
@Default(lngs = { "en" }, values = { "localhost only?" })
String plugins_optional_RemoteControl_localhost();
@Default(lngs = { "en" }, values = { "Remote Control" })
String jd_plugins_optional_remotecontrol_jdremotecontrol_description();
@Default(lngs = { "en" }, values = { "%s1 stopped." })
String plugins_optional_remotecontrol_stopped2(Object s1);
@Default(lngs = { "en" }, values = { "Remote Control" })
String jd_plugins_optional_remotecontrol_jdremotecontrol();
}