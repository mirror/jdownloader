package org.jdownloader.extensions.extraction.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface ExtractionTranslation extends TranslateInterface {
    @Default(lngs = { "en", "de" }, values = { "Archive Extractor", "Archiv Entpacker" })
    String name();

    @Default(lngs = { "en", "de" }, values = { "Extracts all usual types of archives (zip,rar,7zip,...)", "Entpackt all üblichen Archivtypen (zip,rar,7zip,..)" })
    String description();

    @Default(lngs = { "en" }, values = { "Leave x MiB additional space after unpacking" })
    String gui_config_extraction_additional_space();

    @Default(lngs = { "en" }, values = { "Multi unpacker settings" })
    String plugins_optional_extraction_multi_config();

    @Default(lngs = { "en" }, values = { "Extract OK" })
    String plugins_optional_extraction_status_extractok();

    @Default(lngs = { "en" }, values = { "Cracking password" })
    String plugins_optional_extraction_status_crackingpass();

    @Default(lngs = { "en" }, values = { "Ask for unknown passwords?" })
    String gui_config_extraction_ask_path();

    @Default(lngs = { "en" }, values = { "Extraction" })
    String plugins_optional_extraction_name();

    @Default(lngs = { "en" }, values = { "High" })
    String plugins_optional_extraction_multi_priority_high();

    @Default(lngs = { "en" }, values = { "Overwrite existing files?" })
    String gui_config_extraction_overwrite();

    @Default(lngs = { "en" }, values = { "Extract failed" })
    String plugins_optional_extraction_status_extractfailed();

    @Default(lngs = { "en" }, values = { "Password found" })
    String plugins_optional_extraction_status_passfound();

    @Default(lngs = { "en" }, values = { "Extract: failed (CRC in unknown file)" })
    String plugins_optional_extraction_error_extrfailedcrc();

    @Default(lngs = { "en" }, values = { "Don't unpack files with the following patterns" })
    String plugins_optional_extraction_config_matcher_title();

    @Default(lngs = { "en" }, values = { "All supported formats" })
    String plugins_optional_extraction_filefilter();

    @Default(lngs = { "en" }, values = { "Extract %s1" })
    String plugins_optional_extraction_progress_extractfile(Object s1);

    @Default(lngs = { "en" }, values = { "Extracting" })
    String plugins_optional_extraction_status_extracting();

    @Default(lngs = { "en" }, values = { "Extract failed (password)" })
    String plugins_optional_extraction_status_extractfailedpass();

    @Default(lngs = { "en" }, values = { "Extract to" })
    String gui_config_extraction_path();

    @Default(lngs = { "en" }, values = { "Priority" })
    String plugins_optional_extraction_multi_priority();

    @Default(lngs = { "en" }, values = { "Extract Directory" })
    String plugins_optional_extraction_filefilter_extractto();

    @Default(lngs = { "en" }, values = { "Low" })
    String plugins_optional_extraction_multi_priority_low();

    @Default(lngs = { "en" }, values = { "Opening archive" })
    String plugins_optional_extraction_status_openingarchive();

    @Default(lngs = { "en" }, values = { "Use customized extract path" })
    String gui_config_extraction_use_extractto();

    @Default(lngs = { "en" }, values = { "Extract: failed (CRC in %s1)" })
    String plugins_optional_extraction_crcerrorin(Object s1);

    @Default(lngs = { "en" }, values = { "Subpath" })
    String gui_config_extraction_subpath();

    @Default(lngs = { "en" }, values = { "Only use subpath if archive contains more than x files" })
    String gui_config_extraction_subpath_minnum();

    @Default(lngs = { "en" }, values = { "Use subpath" })
    String gui_config_extraction_use_subpath();

    @Default(lngs = { "en" }, values = { "Delete Infofile after extraction" })
    String gui_config_extraction_remove_infofile();

    @Default(lngs = { "en" }, values = { "Middle" })
    String plugins_optional_extraction_multi_priority_middle();

    @Default(lngs = { "en" }, values = { "The path %s1 does not exist." })
    String plugins_optional_extraction_messages(Object s1);

    @Default(lngs = { "en" }, values = { "Extract failed (CRC error)" })
    String plugins_optional_extraction_status_extractfailedcrc();

    @Default(lngs = { "en" }, values = { "Only use subpath if the files of the archive are in not in one folder" })
    String gui_config_extraction_subpath_no_folder();

    @Default(lngs = { "en" }, values = { "Deep-Extraction" })
    String gui_config_extraction_deep_extract();

    @Default(lngs = { "en" }, values = { "Extract: failed (File not found)" })
    String plugins_optional_extraction_filenotfound();

    @Default(lngs = { "en" }, values = { "Not enough space to extract" })
    String plugins_optional_extraction_status_notenoughspace();

    @Default(lngs = { "en" }, values = { "Advanced settings" })
    String plugins_optional_extraction_config_advanced();

    @Default(lngs = { "en" }, values = { "Queued for extracting" })
    String plugins_optional_extraction_status_queued();

    @Default(lngs = { "en" }, values = { "Delete archives after suc. extraction?" })
    String gui_config_extraction_remove_after_extract();

    @Default(lngs = { "en" }, values = { "Subfolder settings" })
    String plugins_optional_extraction_config_subfolder();

    @Default(lngs = { "en" }, values = { "Password for %s1?" })
    String plugins_optional_extraction_askForPassword(Object s1);
}