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

    // TODO Remove unused code found by UCDetector
    // @Default(lngs = { "en" }, values = {
    // "Leave x MiB additional space after unpacking" })
    // String gui_config_extraction_additional_space();

    @Default(lngs = { "en" }, values = { "Archive has %s1 part(s) and is complete" })
    String ValidateArchiveAction_actionPerformed_(int size);

    @Default(lngs = { "en" }, values = { "Archive has %s1 part(s) and is incomplete." })
    String ValidateArchiveAction_actionPerformed_bad(int size);

    @Default(lngs = { "en" }, values = { "Validate Archive(s)" })
    String ValidateArchiveAction_ValidateArchiveAction_object_();

    @Default(lngs = { "en" }, values = { "Extract OK" })
    String plugins_optional_extraction_status_extractok();

    @Default(lngs = { "en" }, values = { "Cracking password: %s1 %" })
    String plugins_optional_extraction_status_crackingpass_progress(double percent);

    @Default(lngs = { "en" }, values = { "Overwrite existing files?" })
    String settings_overwrite();

    @Default(lngs = { "en" }, values = { "Password found" })
    String plugins_optional_extraction_status_passfound();

    @Default(lngs = { "en" }, values = { "Extract: failed (CRC in unknown file)" })
    String plugins_optional_extraction_error_extrfailedcrc();

    @Default(lngs = { "en" }, values = { "All supported formats" })
    String plugins_optional_extraction_filefilter();

    @Default(lngs = { "en" }, values = { "Extracting %s1" })
    String plugins_optional_extraction_status_extracting_filename(String string);

    @Default(lngs = { "en" }, values = { "Extract failed (password)" })
    String plugins_optional_extraction_status_extractfailedpass();

    @Default(lngs = { "en" }, values = { "Extraction running" })
    String plugins_optional_extraction_status_openingarchive2();

    @Default(lngs = { "en" }, values = { "Extract: failed (CRC in %s1)" })
    String plugins_optional_extraction_crcerrorin(Object s1);

    @Default(lngs = { "en" }, values = { "Subfolder pattern" })
    String settings_subpath();

    @Default(lngs = { "en" }, values = { "   ... only if the archive root contains at least" })
    String settings_subpath_minnum3();

    @Default(lngs = { "en" }, values = { "Create Subfolder" })
    String settings_use_subpath();

    @Default(lngs = { "en" }, values = { "... only if there are no root folders" })
    String settings_subpath_no_folder2();

    @Default(lngs = { "en" }, values = { "Extract: failed (File not found)" })
    String plugins_optional_extraction_filenotfound();

    @Default(lngs = { "en" }, values = { "Not enough space to extract" })
    String plugins_optional_extraction_status_notenoughspace();

    @Default(lngs = { "en" }, values = { "Queued for extracting" })
    String plugins_optional_extraction_status_queued();

    @Default(lngs = { "en" }, values = { "Delete Archive Files after suc. extraction?" })
    String settings_remove_after_extract();

    @Default(lngs = { "en" }, values = { "Delete Archive Downloadlinks after suc. extraction?" })
    String settings_remove_after_extract_downloadlink();

    @Default(lngs = { "en" }, values = { "To extract the archive, a password is required!" })
    String plugins_optional_extraction_askForPassword2();

    @Default(lngs = { "en" }, values = { "Extract destination folder" })
    String settings_extractto();

    @Default(lngs = { "en" }, values = { "Extract to the following folder" })
    String settings_extract_to_archive_folder();

    @Default(lngs = { "en" }, values = { "Extract to" })
    String settings_extract_to_path();

    @Default(lngs = { "en" }, values = { "Do not use a subfolder if all files in the archive already are  packed in folders." })
    String settings_subpath_no_folder_tt();

    @Default(lngs = { "en" }, values = { "Miscellaneous" })
    String settings_various();

    @Default(lngs = { "en" }, values = { "file(s)" })
    String files2();

    @Default(lngs = { "en" }, values = { "Don't unpack files matching the following conditions (One per line)" })
    String settings_blacklist();

    @Default(lngs = { "en" }, values = { "Special settings (Only for rar, 7z and zip. Not for split files)" })
    String settings_multi();

    @Default(lngs = { "en" }, values = { "CPU Priority" })
    String settings_cpupriority();

    @Default(lngs = { "en" }, values = { "High" })
    String settings_cpupriority_high();

    @Default(lngs = { "en" }, values = { "Middle" })
    String settings_cpupriority_middle();

    @Default(lngs = { "en" }, values = { "Low" })
    String settings_cpupriority_low();

    @Default(lngs = { "en" }, values = { "Use original file date if possible" })
    String settings_multi_use_original_file_date();

    @Default(lngs = { "en" }, values = { "Password List" })
    String settings_passwords();

    @Default(lngs = { "en" }, values = { "Archive Passwords (one per line)" })
    String settings_passwordlist();

    @Default(lngs = { "en" }, values = { "No Extraction Job" })
    String tooltip_empty();

    @Default(lngs = { "en" }, values = { "Archive's Name" })
    String tooltip_NameColumn();

    @Default(lngs = { "en" }, values = { "Extracting" })
    String plugins_optional_extraction_status_extracting2();

    @Default(lngs = { "en" }, values = { "Extract Now" })
    String contextmenu_extract();

    @Default(lngs = { "en" }, values = { "Archive(s)" })
    String contextmenu_main();

    @Default(lngs = { "en" }, values = { "Auto Extract Enabled" })
    String contextmenu_autoextract();

    @Default(lngs = { "en" }, values = { "Set Extraction Path" })
    String contextmenu_extract_to();

    @Default(lngs = { "en" }, values = { "Validate %s1" })
    String ValidateArchiveAction_ValidateArchiveAction(String name);

    @Default(lngs = { "en" }, values = { "Cannot extract %s1. Archive is incomplete!" })
    String cannot_extract_incopmplete(String name);

    @Default(lngs = { "en" }, values = { "Archive %s1" })
    String dummyarchivedialog_title(String name);

    @Default(lngs = { "en" }, values = { "Close" })
    String close();

    @Default(lngs = { "en" }, values = { "Filename" })
    String filename();

    @Default(lngs = { "en" }, values = { "Link Status" })
    String exists();

    @Default(lngs = { "en" }, values = { "Unknown" })
    String unknown();

    @Default(lngs = { "en" }, values = { "Link available" })
    String online();

    @Default(lngs = { "en" }, values = { "File offline or missing" })
    String offline();

    @Default(lngs = { "en" }, values = { "There is no Link for this part in this package, or the Link is offline" })
    String offline_tt();

    @Default(lngs = { "en" }, values = { "Link available" })
    String online_tt();

    @Default(lngs = { "en" }, values = { "A Link for this part is available, but it may be offline." })
    String unknown_tt();

    @Default(lngs = { "en" }, values = { "File Status" })
    String local();

    @Default(lngs = { "en" }, values = { "File already exists" })
    String downloadedok();

    @Default(lngs = { "en" }, values = { "File must be downloaded" })
    String downloadedbad();

    @Default(lngs = { "en" }, values = { "File exists" })
    String file_exists();

    @Default(lngs = { "en" }, values = { "File exists not" })
    String file_exists_not();

    @Default(lngs = { "en" }, values = { "Extraction of %s1 failed" })
    String extraction_failed(String name);

    @Default(lngs = { "en" }, values = { "Subpath Properties" })
    String properties();

    @Default(lngs = { "en" }, values = { "Packagename" })
    String packagename();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String hoster();

    @Default(lngs = { "en" }, values = { "Downloadpath Subfolder" })
    String subfolder();

    @Default(lngs = { "en" }, values = { "Date" })
    String date();

    @Default(lngs = { "en" }, values = { "Archivename" })
    String archivename();

    @Default(lngs = { "en" }, values = { "Extract Files" })
    String menu_tools_extract_files();

    @Default(lngs = { "en" }, values = { "Extract Archive(s) to..." })
    String extract_to2();

    @Default(lngs = { "en" }, values = { "Set Archive Password" })
    String contextmenu_password();

    @Default(lngs = { "en" }, values = { "'Delete Archive Files' enabled" })
    String contextmenu_autodeletefiles();

    @Default(lngs = { "en" }, values = { "'Remove Links from Downloadlist' enabled" })
    String contextmenu_autodeletelinks();

    @Default(lngs = { "en" }, values = { "Cleanup after Extraction" })
    String context_cleanup();

    @Default(lngs = { "en" }, values = { "Extraction Password" })
    String context_password();

    @Default(lngs = { "en" }, values = { "Set Password to extract %s1." })
    String context_password_msg(String name);

    @Default(lngs = { "en" }, values = { "Set Password to extract %s1.\r\nSuggested password(s):\r\n%s2" })
    String context_password_msg2(String name, String passwords);

    @Default(lngs = { "en" }, values = { "Set Password" })
    String context_password_msg_multi();

    @Default(lngs = { "en" }, values = { "Set Password.\r\nSuggested password(s):\r\n%s1" })
    String context_password_msg2_multi(String passwords);

    @Default(lngs = { "en" }, values = { "Validate Archives" })
    String ValidateArchiveAction_ValidateArchiveAction_multi();

    @Default(lngs = { "en" }, values = { "Auto extract is now enabled" })
    String set_autoextract_true();

    @Default(lngs = { "en" }, values = { "Auto extract is now disabled" })
    String set_autoextract_false();

    @Default(lngs = { "en" }, values = { "Auto remove Files is now enabled" })
    String set_autoremovefiles_true();

    @Default(lngs = { "en" }, values = { "Auto remove Files is now disabled" })
    String set_autoremovefiles_false();

    @Default(lngs = { "en" }, values = { "Auto remove Downloadlinks is now enabled" })
    String set_autoremovelinks_true();

    @Default(lngs = { "en" }, values = { "Auto remove Downloadlinks is now disabled" })
    String set_autoremovelinks_false();

    @Default(lngs = { "en" }, values = { "Extraction failed: %s1" })
    String failed(String message);

    @Default(lngs = { "en" }, values = { "Extraction failed" })
    String failed_no_details();

    @Default(lngs = { "en" }, values = { "Extracting... thus disabled" })
    String extracting();

    @Default(lngs = { "en" }, values = { "Extracting Archive: %s1" })
    String extracting_archive(String name);

    @Default(lngs = { "en" }, values = { "Please wait: Extracting Archive: %s1" })
    String extracting_wait(String name);

    @Default(lngs = { "en" }, values = { "Extracted %s1: %s2 files" })
    String extractprogress_label(String percent, String extractedFiles);

    @Default(lngs = { "en" }, values = { "Extract after Download" })
    String contextmenu_disable_auto_extract2();

    @Default(lngs = { "en" }, values = { "Extract Archives after Download (Defaultvalue)" })
    String settings_auto_extract_default();

    @Default(lngs = { "en" }, values = { "JDownloader crashed!" })
    String crash_title();

    @Default(lngs = { "en" }, values = { "It seems that there was a crash while extracting an archive.\r\nCheck the details below. Do you want to send a bug report?" })
    String crash_message();

    @Default(lngs = { "en" }, values = { "Show Extraction Log" })
    String contextmenu_extract_show_result();

    @Default(lngs = { "en" }, values = { "Extract Log for %s1" })
    String showextractlog(String name);

    @Default(lngs = { "en" }, values = { "folder(s)" })
    String folders();

    @Default(lngs = { "en" }, values = { "file(s) or folder(s)" })
    String files_and_folders();

    @Default(lngs = { "en" }, values = { "and at least" })
    String and();

    @Default(lngs = { "en" }, values = { "Archive Password required!" })
    String ask_for_password();

    @Default(lngs = { "en" }, values = { "an archive extraction is in progress" })
    String bubbletype();

    @Default(lngs = { "en" }, values = { "Archive:" })
    String archive();

    @Default(lngs = { "en" }, values = { "Status:" })
    String archive_status();

    @Default(lngs = { "en" }, values = { "Extracting archive!" })
    String bubble_text();

    @Default(lngs = { "en" }, values = { "File:" })
    String archive_file();

    @Default(lngs = { "en" }, values = { "Extract to:" })
    String archive_folder();

    @Default(lngs = { "en" }, values = { "Password required!" })
    String password_required();

    @Default(lngs = { "en" }, values = { "Finding Password.." })
    String password_search();

    @Default(lngs = { "en" }, values = { "Extracting..." })
    String extracting_in_progress();

    @Default(lngs = { "en" }, values = { "Progress Icon" })
    String bubblecontent_progress();

    @Default(lngs = { "en" }, values = { "Move to Trash" })
    String delete_to_trash();

    @Default(lngs = { "en" }, values = { "Don't delete anything" })
    String dont_delete();

    @Default(lngs = { "en" }, values = { "Delete files finally" })
    String final_delete();

}