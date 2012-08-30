package org.jdownloader.extensions.streaming;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.TranslateInterface;

public interface StreamingTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Streaming" })
    String popup_streaming();

    @Default(lngs = { "en" }, values = { "Play" })
    String popup_streaming_playvlc();

    @Default(lngs = { "en" }, values = { "Media Archive" })
    String gui_title();

    @Default(lngs = { "en" }, values = { "Manage all your Streaming Links" })
    String gui_tooltip();

    @Default(lngs = { "en" }, values = { "Play Archive" })
    String unraraction();

    @Default(lngs = { "en" }, values = { "Open Rar Archive" })
    String open_rar();

    @Default(lngs = { "en" }, values = { "Please wait while JDownloader is preparing the Stream" })
    String open_rar_msg();

    @Default(lngs = { "en" }, values = { "Enter Password to open %s1" })
    String enter_password(String name);

    @Default(lngs = { "en" }, values = { "Please enter Passwords. Try one of these:\r\n%s1" })
    String enter_passwordfor(String list);

    @Default(lngs = { "en" }, values = { "Please enter Passwords." })
    String enter_passwordfor2();

    @Default(lngs = { "en" }, values = { "Wrong Password!" })
    String wrong_password();

    @Default(lngs = { "en" }, values = { "Play to %s1" })
    String playto(String displayName);

    @Default(lngs = { "en" }, values = { "Play %s1 to %s2" })
    String playto2(String path, String displayName);

    @Default(lngs = { "en" }, values = { "Videos" })
    String SettingsSidebarModel_video();

    @Default(lngs = { "en" }, values = { "Audio" })
    String SettingsSidebarModel_audio();

    @Default(lngs = { "en" }, values = { "Images" })
    String SettingsSidebarModel_images();

    @Default(lngs = { "en" }, values = { "Categories" })
    String categories();

    @Default(lngs = { "en" }, values = { "Add Media" })
    String add_files_action();

    @Default(lngs = { "en" }, values = { "Add to Media Library" })
    String AddToLibraryAction();

    @Default(lngs = { "en" }, values = { "Title" })
    String gui_video_name();

    @Default(lngs = { "en" }, values = { "Format" })
    String gui_video_format();

    @Default(lngs = { "en" }, values = { "Status" })
    String gui_video_status();

    @Default(lngs = { "en" }, values = { "Open Rar Archive: %s1" })
    String open_rar(String name);

    @Default(lngs = { "en" }, values = { "Scan Information: %s1" })
    String prepare(String name);

    @Default(lngs = { "en" }, values = { "Are your sure?" })
    String mediatable_rly_remove_title();

    @Default(lngs = { "en" }, values = { "Remove %s1 items from the library now?" })
    String mediatable_rly_remove_msg(int size);

    @Default(lngs = { "en" }, values = { "Artist" })
    String gui_video_artist();

    @Default(lngs = { "en" }, values = { "Duration" })
    String gui_video_duration();

    @Default(lngs = { "en" }, values = { "Album" })
    String gui_video_album();

    @Default(lngs = { "en" }, values = { "Size" })
    String gui_video_size();

    @Default(lngs = { "en" }, values = { "Video" })
    String nodename_video();

    @Default(lngs = { "en" }, values = { "Audio" })
    String nodename_audio();

    @Default(lngs = { "en" }, values = { "Image" })
    String nodename_image();
}
