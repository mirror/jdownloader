package org.jdownloader.extensions.extraction;

import java.io.File;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;

public class Test {
    public static void main(String[] args) throws StartException, DialogCanceledException, DialogClosedException {

        final ExtractionExtension extension = new ExtractionExtension();
        extension.init();
        File[] files = Dialog.getInstance().showFileChooser("test", "Choose archive", FileChooserSelectionMode.FILES_ONLY, null, true, null, null);
        for (File f : files) {

            final Archive archive = extension.getArchiveByFactory(new FileArchiveFactory(f));
            new Thread() {
                @Override
                public void run() {
                    if (archive.isComplete()) extension.addToQueue(archive);
                }
            }.start();

        }

    }
}
