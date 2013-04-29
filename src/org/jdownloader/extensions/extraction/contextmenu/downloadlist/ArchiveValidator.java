package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.ValidateArchiveAction;
import org.jdownloader.gui.views.SelectionInfo;

public class ArchiveValidator {

    public static ExtractionExtension EXTENSION;

    public static ValidateArchiveAction<FilePackage, DownloadLink> validate(SelectionInfo<FilePackage, DownloadLink> selection) {
        final ValidateArchiveAction<FilePackage, DownloadLink> validation = new ValidateArchiveAction<FilePackage, DownloadLink>(EXTENSION, (SelectionInfo<FilePackage, DownloadLink>) selection);

        return validation;
    }

}
