package org.jdownloader.extensions.langfileeditor.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface LangfileeditorTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "Updating SVN: Error!" })
String plugins_optional_langfileeditor_svn_updating_error();
@Default(lngs = { "en" }, values = { "File" })
String plugins_optional_langfileeditor_file();
@Default(lngs = { "en" }, values = { "more..." })
String jd_plugins_optional_langfileeditor_LangFileEditor_btn_readmore();
@Default(lngs = { "en" }, values = { "Language File Value" })
String plugins_optional_langfileeditor_languageFileValue();
@Default(lngs = { "en" }, values = { "Updating SVN: Please wait" })
String plugins_optional_langfileeditor_svn_updating();
@Default(lngs = { "en" }, values = { "Test & Save Logins" })
String jd_plugins_optional_langfileeditor_LangFileEditor_testlogins();
@Default(lngs = { "en" }, values = { "Translation Editor" })
String jd_plugins_optional_langfileeditor_LFEView_title();
@Default(lngs = { "en" }, values = { "Revert/Reload" })
String plugins_optional_langfileeditor_reload();
@Default(lngs = { "en" }, values = { "Insert the hotkey for the action %s1 here. Allowed modifiers are CTRL, ALTGR, ALT, META, SHIFT" })
String jd_plugins_optional_langfileeditor_columns_LanguageColumn_tooltip_accelerator(Object s1);
@Default(lngs = { "en" }, values = { "Language Editor" })
String plugins_optional_langfileeditor_title();
@Default(lngs = { "en" }, values = { "LanguageFile saved successfully!" })
String plugins_optional_langfileeditor_save_success_message();
@Default(lngs = { "en" }, values = { "Type in the name of the key:" })
String plugins_optional_langfileeditor_addKey_message1();
@Default(lngs = { "en" }, values = { "Type in the translated message of the key:" })
String plugins_optional_langfileeditor_addKey_message2();
@Default(lngs = { "en" }, values = { "Save changes?" })
String plugins_optional_langfileeditor_saveChanges();
@Default(lngs = { "en" }, values = { "Logins incorrect.\r\nPlease enter correct logins." })
String jd_plugins_optional_langfileeditor_langfileeditor_badlogins();
@Default(lngs = { "en" }, values = { "(Developer Addon)" })
String jd_plugins_optional_langfileeditor_langfileeditor_description();
@Default(lngs = { "en" }, values = { "Edit a translation file" })
String jd_plugins_optional_langfileeditor_LFEView_tooltip();
@Default(lngs = { "en" }, values = { "Complete Reload (Deletes Cache)" })
String plugins_optional_langfileeditor_completeReload();
@Default(lngs = { "en" }, values = { "Error while updating languages:\r\n %s1" })
String plugins_optional_langfileeditor_error_updatelanguages_message(Object s1);
@Default(lngs = { "en" }, values = { "Old" })
String plugins_optional_langfileeditor_keychart_old();
@Default(lngs = { "en" }, values = { "Error occured" })
String plugins_optional_langfileeditor_error_title();
@Default(lngs = { "en" }, values = { "Successful!" })
String jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_checklogins_succeeded();
@Default(lngs = { "en" }, values = { "Test if the logins are correct" })
String jd_plugins_optional_langfileeditor_LangFileEditor_testloginsmessage();
@Default(lngs = { "en" }, values = { "Delete Key(s)" })
String plugins_optional_langfileeditor_deleteKeys();
@Default(lngs = { "en" }, values = { "Upload (SVN) Password" })
String jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_password();
@Default(lngs = { "en" }, values = { "Add new key" })
String plugins_optional_langfileeditor_addKey_title();
@Default(lngs = { "en" }, values = { "Adopt Default(s)" })
String plugins_optional_langfileeditor_adoptDefaults();
@Default(lngs = { "en" }, values = { "The key '%s1' is already in use!" })
String plugins_optional_langfileeditor_addKey_error_message(Object s1);
@Default(lngs = { "en" }, values = { "Error while updating source:\r\n %s1" })
String plugins_optional_langfileeditor_error_updatesource_message(Object s1);
@Default(lngs = { "en" }, values = { "Delete Old Key(s)" })
String plugins_optional_langfileeditor_deleteOldKeys();
@Default(lngs = { "en" }, values = { "Language File Editor" })
String jd_plugins_optional_langfileeditor_langfileeditor();
@Default(lngs = { "en" }, values = { "Username or password wrong!" })
String jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_checklogins_failed();
@Default(lngs = { "en" }, values = { "Key" })
String plugins_optional_langfileeditor_key();
@Default(lngs = { "en" }, values = { "Test JD with current translation" })
String plugins_optional_langfileeditor_startcurrent();
@Default(lngs = { "en" }, values = { "Missing" })
String plugins_optional_langfileeditor_keychart_missing();
@Default(lngs = { "en" }, values = { "Delete Old Key(s)?" })
String plugins_optional_langfileeditor_deleteOld_title();
@Default(lngs = { "en" }, values = { "Load Language" })
String plugins_optional_langfileeditor_load();
@Default(lngs = { "en" }, values = { "Test JD in Key mode" })
String plugins_optional_langfileeditor_startkey();
@Default(lngs = { "en" }, values = { "Done" })
String plugins_optional_langfileeditor_keychart_done();
@Default(lngs = { "en" }, values = { "There are still %s1 old keys in the LanguageFile. Delete them before saving?" })
String plugins_optional_langfileeditor_deleteOld_message2(Object s1);
@Default(lngs = { "en" }, values = { "Updating SVN: Complete" })
String plugins_optional_langfileeditor_svn_updating_ready();
@Default(lngs = { "en" }, values = { "Add Key" })
String plugins_optional_langfileeditor_addKey();
@Default(lngs = { "en" }, values = { "Save and upload your changes to %s1?" })
String plugins_optional_langfileeditor_saveChanges_message_upload(Object s1);
@Default(lngs = { "en" }, values = { "en.loc" })
String plugins_optional_langfileeditor_english();
@Default(lngs = { "en" }, values = { "An error occured while writing the LanguageFile:\n%s1" })
String plugins_optional_langfileeditor_save_error_message(Object s1);
@Default(lngs = { "en" }, values = { "Analyzing Source Folder" })
String plugins_optional_langfileeditor_analyzingSource1();
@Default(lngs = { "en" }, values = { "Default Value" })
String plugins_optional_langfileeditor_sourceValue();
@Default(lngs = { "en" }, values = { "Save Offline" })
String plugins_optional_langfileeditor_savelocale();
@Default(lngs = { "en" }, values = { "Upload (SVN) Username" })
String jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_username();
@Default(lngs = { "en" }, values = { "To use this addon, you need a JD-SVN Account" })
String jd_plugins_optional_langfileeditor_LangFileEditor_initConfigEntries_message();
@Default(lngs = { "en" }, values = { "The modifier %s1 isn't allowed!" })
String jd_plugins_optional_langfileeditor_columns_LanguageColumn_tooltip_accelerator_wrong(Object s1);
@Default(lngs = { "en" }, values = { "Test" })
String plugins_optional_langfileeditor_test();
@Default(lngs = { "en" }, values = { "Save & Upload" })
String plugins_optional_langfileeditor_saveandupload();
@Default(lngs = { "en" }, values = { "Save your changes to %s1?" })
String plugins_optional_langfileeditor_saveChanges_message(Object s1);
@Default(lngs = { "en" }, values = { "No" })
String gui_btn_no();
@Default(lngs = { "en" }, values = { "Delete all %s1 old Key(s)?" })
String plugins_optional_langfileeditor_deleteOld_message(Object s1);
@Default(lngs = { "en" }, values = { "SVN Account missing. Click here to read more." })
String plugins_optional_langfileeditor_account_warning();
@Default(lngs = { "en" }, values = { "Clear Value(s)" })
String plugins_optional_langfileeditor_clearValues();
@Default(lngs = { "en" }, values = { "Your translated String contains a wrong count of placeholders!" })
String jd_plugins_optional_langfileeditor_columns_LanguageColumn_tooltip_wrongParameterCount();
@Default(lngs = { "en" }, values = { "Analyzing Source Folder: Complete" })
String plugins_optional_langfileeditor_analyzingSource_ready();
@Default(lngs = { "en" }, values = { "Yes" })
String gui_btn_yes();
}