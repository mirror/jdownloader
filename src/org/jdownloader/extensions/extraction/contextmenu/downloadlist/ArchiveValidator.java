package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.util.WeakHashMap;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.ValidateArchiveAction;
import org.jdownloader.gui.views.SelectionInfo;

public class ArchiveValidator {

    public static ExtractionExtension                                EXTENSION;

    private static WeakHashMap<SelectionInfo, ValidateArchiveAction> VALIDATIONCACHE = new WeakHashMap<SelectionInfo, ValidateArchiveAction>();
    private static WeakHashMap<SelectionInfo, Object>                VALIDATIONLOCKS = new WeakHashMap<SelectionInfo, Object>();

    public static ValidateArchiveAction<FilePackage, DownloadLink> validate(SelectionInfo<FilePackage, DownloadLink> selection) {
        Object lock = null;
        ValidateArchiveAction validation = null;
        synchronized (VALIDATIONCACHE) {
            validation = VALIDATIONCACHE.get(selection);
            if (validation != null) return validation;
            synchronized (VALIDATIONLOCKS) {
                lock = VALIDATIONLOCKS.get(selection);
                if (lock == null) {
                    lock = new Object();
                }
            }
        }
        synchronized (lock) {
            try {
                synchronized (VALIDATIONCACHE) {
                    validation = VALIDATIONCACHE.get(selection);
                    if (validation != null) return validation;
                }
                validation = new ValidateArchiveAction<FilePackage, DownloadLink>(EXTENSION, (SelectionInfo<FilePackage, DownloadLink>) selection);
                synchronized (VALIDATIONCACHE) {
                    VALIDATIONCACHE.put(selection, validation);
                }
                return validation;
            } finally {
                synchronized (VALIDATIONLOCKS) {
                    VALIDATIONLOCKS.remove(selection);
                }
            }
        }

    }
}
