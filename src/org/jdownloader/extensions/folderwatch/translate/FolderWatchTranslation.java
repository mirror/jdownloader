package org.jdownloader.extensions.folderwatch.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface FolderWatchTranslation extends TranslateInterface {
    @Default(lngs = { "en" }, values = { "Add Links to JDownloader just by putting Linklist files (*.crawljob) in a special folder on your harddisk." })
    String description();

    @Default(lngs = { "en" }, values = { "Folder Watch" })
    String title();

}