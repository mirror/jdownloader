package org.jdownloader.extensions.infofilewriter.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface InfofilewriterTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Info File Writer" })
String jd_plugins_optional_jdinfofilewriter_description();
@Default(lngs = { "en" }, values = { "Info File Writer" })
String jd_plugins_optional_jdinfofilewriter();
@Default(lngs = { "en" }, values = { "downloadlinks" })
String jd_plugins_optional_JDInfoFileWriter_downloadlinks();
@Default(lngs = { "en" }, values = { "Filename:" })
String plugins_optional_infofilewriter_filename();
@Default(lngs = { "en" }, values = { "Insert selected Key into the Content" })
String plugins_optional_infofilewriter_insertKey();
@Default(lngs = { "en" }, values = { "Content:" })
String plugins_optional_infofilewriter_content();
@Default(lngs = { "en" }, values = { "Comment: %LAST_FINISHED_PACKAGE.COMMENT%\r\nPassword: %LAST_FINISHED_PACKAGE.PASSWORD%\r\nAuto-Password: %LAST_FINISHED_PACKAGE.AUTO_PASSWORD%\r\n%LAST_FINISHED_PACKAGE.FILELIST%\r\nFinalized %SYSTEM.DATE% to %SYSTEM.TIME% Clock" })
String plugins_optional_infofilewriter_contentdefault();
@Default(lngs = { "en" }, values = { "Available variables" })
String plugins_optional_infofilewriter_variables();
@Default(lngs = { "en" }, values = { "Insert" })
String plugins_optional_infofilewriter_insertKey_short();
@Default(lngs = { "en" }, values = { "Use only if password is enabled" })
String plugins_optional_infofilewriter_onlywithpassword();
@Default(lngs = { "en" }, values = { "Create Info File" })
String jd_plugins_optional_JDInfoFileWriter_createInfoFile();
@Default(lngs = { "en" }, values = { "packages" })
String jd_plugins_optional_JDInfoFileWriter_packages();
}