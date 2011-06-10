package org.jdownloader.extensions.folderwatch.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface FolderwatchTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Delete all container files within selected folder" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_action_emptyfolder_long();

    @Default(lngs = { "en" }, values = { "Add history entry for every found container" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_option_history();

    @Default(lngs = { "en" }, values = { "Download contents of found container to container location" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_option_download_to_container_location();

    @Default(lngs = { "en" }, values = { "Actions" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_label_actions();

    @Default(lngs = { "en" }, values = { "Are you sure you want to clear the history?" })
    String action_folderwatch_clear_message();

    @Default(lngs = { "en" }, values = { "Watch registered folders recursively" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_option_recursive();

    @Default(lngs = { "en" }, values = { "Delete container after import" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_option_importdelete();

    @Default(lngs = { "en" }, values = { "show history" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_action_showhistory();

    @Default(lngs = { "en" }, values = { "open folder" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_action_openfolder();

    @Default(lngs = { "en" }, values = { "Import container when found" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_option_import();

    @Default(lngs = { "en" }, values = { "File exists" })
    String plugins_optional_folderwatch_panel_filestatus_exists();

    @Default(lngs = { "en" }, values = { "Show all (imported) containers found by FolderWatch" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_action_showhistory_long();

    @Default(lngs = { "en" }, values = { "Import date" })
    String jd_plugins_optional_folderwatch_tablemodel_importdate();

    @Default(lngs = { "en" }, values = { "FolderWatch" })
    String plugins_optional_folderwatch_view_title();

    @Default(lngs = { "en" }, values = { "Container type" })
    String jd_plugins_optional_folderwatch_tablemodel_filetype();

    @Default(lngs = { "en" }, values = { "Folder list" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_label_folderlist();

    @Default(lngs = { "en" }, values = { "File does not exist" })
    String plugins_optional_folderwatch_panel_filestatus_notexists();

    @Default(lngs = { "en" }, values = { "empty folder" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_action_emptyfolder();

    @Default(lngs = { "en" }, values = { "Could not open folder. Folder does not exist!" })
    String plugins_optional_folderwatch_JDFolderWatch_action_openfolder_errormessage();

    @Default(lngs = { "en" }, values = { "File status" })
    String plugins_optional_folderwatch_panel_filestatus();

    @Default(lngs = { "en" }, values = { "add" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_folderlist_add();

    @Default(lngs = { "en" }, values = { "Open selected folder with file manager" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_action_openfolder_long();

    @Default(lngs = { "en" }, values = { "Path" })
    String jd_plugins_optional_folderwatch_tablemodel_filepath();

    @Default(lngs = { "en" }, values = { "remove" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_folderlist_remove();

    @Default(lngs = { "en" }, values = { "Options" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_label_options();

    @Default(lngs = { "en" }, values = { "Shows all container files that were found by FolderWatch" })
    String plugins_optional_folderwatch_view_tooltip();

    @Default(lngs = { "en" }, values = { "Filename" })
    String jd_plugins_optional_folderwatch_tablemodel_filename();

    @Default(lngs = { "en" }, values = { "Are you sure you want to delete all container files?" })
    String plugins_optional_folderwatch_JDFolderWatch_gui_action_emptyfolder_message();
}