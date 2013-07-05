package org.jdownloader.extensions.extraction.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DecimalFormat;

import javax.swing.filechooser.FileFilter;

import jd.gui.UserIO;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent;
import org.jdownloader.extensions.extraction.ExtractionListener;
import org.jdownloader.extensions.extraction.IExtraction;
import org.jdownloader.extensions.extraction.ValidateArchiveAction;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class ExtractAction extends AbstractExtractionAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1612595219577059496L;

    public ExtractAction(SelectionInfo<?, ?> selection) {
        super(selection);
        setName(T._.menu_tools_extract_files());
        setIconKey("unpack");
        this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
        FileFilter ff = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return true;

                for (IExtraction extractor : _getExtension().getExtractors()) {
                    if (extractor.isArchivSupported(new FileArchiveFactory(pathname))) { return true; }
                }

                return false;
            }

            @Override
            public String getDescription() {
                return org.jdownloader.extensions.extraction.translate.T._.plugins_optional_extraction_filefilter();
            }
        };

        File[] files = UserIO.getInstance().requestFileChooser("_EXTRATION_", null, UserIO.FILES_ONLY, ff, true, null, null);
        if (files == null || files.length == 0) return;

        for (final File archiveStartFile : files) {

            try {
                final Archive archive = _getExtension().buildArchive(new FileArchiveFactory(archiveStartFile));
                if (_getExtension().getSettings().isCustomExtractionPathEnabled()) {

                    File path = DownloadFolderChooserDialog.open(new File(_getExtension().getSettings().getCustomExtractionPath()), false, "Extract To");
                    archive.getSettings().setExtractPath(path.getAbsolutePath());
                } else {
                    File path = DownloadFolderChooserDialog.open(archiveStartFile.getParentFile(), false, "Extract To");
                    archive.getSettings().setExtractPath(path.getAbsolutePath());
                }

                ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                    private ExtractionController controller;

                    @Override
                    public void run() throws Exception {
                        if (archive.isComplete()) {
                            controller = _getExtension().addToQueue(archive);
                            if (controller != null) {
                                final ExtractionListener listener = new ExtractionListener() {

                                    @Override
                                    public void onExtractionEvent(ExtractionEvent event) {
                                        if (event.getCaller() == controller) {
                                            switch (event.getType()) {
                                            case CLEANUP:
                                                _getExtension().getEventSender().removeListener(this);
                                                break;
                                            case EXTRACTION_FAILED:
                                            case EXTRACTION_FAILED_CRC:

                                                if (controller.getException() != null) {
                                                    Dialog.getInstance().showExceptionDialog(org.jdownloader.extensions.extraction.translate.T._.extraction_failed(archiveStartFile.getName()), controller.getException().getLocalizedMessage(), controller.getException());
                                                } else {
                                                    Dialog.getInstance().showErrorDialog(org.jdownloader.extensions.extraction.translate.T._.extraction_failed(archiveStartFile.getName()));
                                                }
                                                break;
                                            }

                                        }
                                    }

                                };
                                try {
                                    _getExtension().getEventSender().addListener(listener);
                                    while (!controller.isFinished()) {
                                        Thread.sleep(1000);
                                    }
                                } catch (InterruptedException e) {
                                    controller.kill();
                                    throw e;
                                }

                            }
                        } else {

                            new ValidateArchiveAction(_getExtension(), archive).actionPerformed(null);
                        }
                    }

                    private DecimalFormat format = new DecimalFormat("00.00 %");

                    @Override
                    public String getString() {

                        if (controller != null) {
                            return T._.extractprogress_label(format.format(controller.getProgress() / 100d), controller.getArchiv().getExtractedFiles().size() + "");
                        } else {
                            return format.format(0d);
                        }
                    }

                    @Override
                    public int getProgress() {
                        if (controller == null) return 0;
                        int ret = (int) (controller.getProgress());

                        return Math.min(99, ret);
                    }

                    @Override
                    public String getLabelString() {

                        return null;
                    }
                }, 0, T._.extracting(archive.getName()), T._.extracting_wait(archive.getName()), NewTheme.I().getIcon(IconKey.ICON_ARCHIVE_RUN, 32), null, null);

                // UIOManager.I().show(class1, impl)
                Dialog.getInstance().showDialog(dialog);
            } catch (ArchiveException e1) {
                _getExtension().getLogger().log(e1);
            } catch (DialogNoAnswerException e1) {
            }
        }
    }
}
