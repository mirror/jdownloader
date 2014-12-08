package org.jdownloader.extensions.eventscripter;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface EventScripterTranslation extends TranslateInterface {
    @Default(lngs = { "en" }, values = { "Listens to internal JDownloader event, and allows to execute relevant scripts (Javascript)" })
    String description();

    @Default(lngs = { "en" }, values = { "A Download started" })
    String ON_DOWNLOAD_CONTROLLER_START();

    @Default(lngs = { "en" }, values = { "A Download stopped" })
    String ON_DOWNLOAD_CONTROLLER_STOPPED();

    @Default(lngs = { "en" }, values = { "None" })
    String NONE();

    @Default(lngs = { "en" }, values = { "Script Editor: %s1" })
    String script_editor_title(String name);

    @Default(lngs = { "en" }, values = { "Do you really want to delete %s1 script(s)?" })
    String sure_delete_entries(int size);

    @Default(lngs = { "en" }, values = { "Security Warning!" })
    String securityLoading_title();

    @Default(lngs = { "en" }, values = { "Your are trying to load and execute JavaScript from %s1. Please keep in mind, that this script has full access to the current Enviroment Properties and Functions. Do only load trusted code! We recommend to only load local trusted JavaScript files." })
    String securityLoading(String fileOrUrl);

    @Default(lngs = { "en" }, values = { "Alert message from %s1@%s2" })
    String showMessageDialog_title(String name, String trigger);

    @Default(lngs = { "en" }, values = { "JDownloader started" })
    String ON_JDOWNLOADER_STARTED();

    @Default(lngs = { "en" }, values = { "JavaScript Syntax Error" })
    String syntax_error();

    @Default(lngs = { "en" }, values = { "Event Scripter" })
    String title();

    @Default(lngs = { "en" }, values = { "Example" })
    String example_script_name();

    @Default(lngs = { "en" }, values = { "Trigger" })
    String event_trigger();

    @Default(lngs = { "en" }, values = { "Edit Script" })
    String edit_script();

    @Default(lngs = { "en" }, values = { "Loading Editor..." })
    String loading_editor_title();

    @Default(lngs = { "en" }, values = { "// ========= Properties for the EventTrigger '%s1'  =========" })
    String properties_for_eventtrigger(String label);

    @Default(lngs = { "en" }, values = { "// DownloadLink" })
    String downloadLink();

    @Default(lngs = { "en" }, values = { "// Filepackage" })
    String filepackage();

    @Default(lngs = { "en" }, values = { "// This Event will never be triggered." })
    String none_trigger();

    @Default(lngs = { "en" }, values = { "//Add your script here. Feel free to use the available api properties and methods" })
    String emptyScript();

    @Default(lngs = { "en" }, values = { "Show/Hide Help" })
    String editor_showhelp();

    @Default(lngs = { "en" }, values = { "Auto Format" })
    String editor_autoformat();

    @Default(lngs = { "en" }, values = { "Test Compile" })
    String editor_testcompile();

    @Default(lngs = { "en" }, values = { "Test Run" })
    String editor_testrun();

    @Default(lngs = { "en" }, values = { "Event Scripter permissions required!" })
    String permission_title();

    @Default(lngs = { "en" }, values = { "The Event Script '%s1' requires permissions for the trigger '%s2'.\r\nThe script tries to \r\n%s3. \r\nDo you want to allow this? If you are not sure, please check your script!" })
    String permission_msg(String name, String label, String string);

    @Default(lngs = { "en" }, values = { "allow" })
    String allow();

    @Default(lngs = { "en" }, values = { "deny" })
    String deny();

    @Default(lngs = { "en" }, values = { "Remote API Event fired" })
    String ON_OUTGOING_REMOTE_API_EVENT();

    @Default(lngs = { "en" }, values = { "A new file has been created" })
    String ON_NEW_FILE();

    @Default(lngs = { "en" }, values = { "/* ===== Classes ===== */" })
    String classes();

    @Default(lngs = { "en" }, values = { "New Crawler Job" })
    String ON_NEW_CRAWLER_JOB();

    @Default(lngs = { "en" }, values = { "Packagizer Hook" })
    String ON_PACKAGIZER();

}