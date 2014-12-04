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

}