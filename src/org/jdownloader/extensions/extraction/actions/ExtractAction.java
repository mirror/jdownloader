package org.jdownloader.extensions.extraction.actions;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DecimalFormat;

import javax.swing.filechooser.FileFilter;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.UserIO;

import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.controlling.contextmenu.Customizer;
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
import org.jdownloader.images.NewTheme;

public class ExtractAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AbstractExtractionAction<PackageType, ChildrenType> {

    /**
     * 
     */
    private static final long serialVersionUID = 1612595219577059496L;

    public static enum ExtractToPathLogic {
        @EnumLabel("Ask for every archive")
        ASK_FOR_FOR_EVERY_ARCHIVE,
        @EnumLabel("Do not ask. Extract to Archive Location")
        EXTRACT_TO_ARCHIVE_PARENT,
        @EnumLabel("Ask once for all archives")
        ASK_ONCE,
        @EnumLabel("Use Custom Extraction Path Setup")
        USE_CUSTOMEXTRACTIONPATH
    }

    private ExtractToPathLogic extractToPathLogic = ExtractToPathLogic.EXTRACT_TO_ARCHIVE_PARENT;

    public void setExtractToPathLogic(ExtractToPathLogic extractToPathLogic) {
        this.extractToPathLogic = extractToPathLogic;
    }

    @Customizer(name = "Extract path logic")
    public ExtractToPathLogic getExtractToPathLogic() {
        return extractToPathLogic;
    }

    public ExtractAction() {
        super(null);
        setItemVisibleForEmptySelection(true);
        setItemVisibleForSelections(true);
        setName(T._.menu_tools_extract_files());
        setIconKey("unpack");

    }

    public boolean isEnabled() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        new Thread("Extracting") {
            public void run() {

                FileFilter ff = new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        if (pathname.isDirectory()) return true;

                        for (IExtraction extractor : _getExtension().getExtractors()) {
                            if (extractor.isArchivSupported(new FileArchiveFactory(pathname), false)) { return true; }
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
                try {
                    File extractTo = null;
                    if (getExtractToPathLogic() == ExtractToPathLogic.ASK_ONCE) {

                        extractTo = DownloadFolderChooserDialog.open(null, false, "Extract To");

                    }
                    for (final File archiveStartFile : files) {

                        try {
                            final Archive archive = _getExtension().buildArchive(new FileArchiveFactory(archiveStartFile));

                            switch (getExtractToPathLogic()) {

                            case USE_CUSTOMEXTRACTIONPATH:
                                archive.getSettings().setExtractPath(_getExtension().getSettings().getCustomExtractionPath());
                                break;
                            case ASK_FOR_FOR_EVERY_ARCHIVE:
                                if (_getExtension().getSettings().isCustomExtractionPathEnabled()) {

                                    File path = DownloadFolderChooserDialog.open(new File(_getExtension().getSettings().getCustomExtractionPath()), false, "Extract To");
                                    archive.getSettings().setExtractPath(path.getAbsolutePath());
                                } else {
                                    File path = DownloadFolderChooserDialog.open(archiveStartFile.getParentFile(), false, "Extract To");
                                    archive.getSettings().setExtractPath(path.getAbsolutePath());
                                }
                                break;
                            case ASK_ONCE:
                                archive.getSettings().setExtractPath(extractTo.getAbsolutePath());

                                break;

                            case EXTRACT_TO_ARCHIVE_PARENT:
                                archive.getSettings().setExtractPath(archiveStartFile.getParentFile().getAbsolutePath());
                                break;

                            }
                            ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                                private ExtractionController controller;

                                @Override
                                public void run() throws Exception {
                                    if (_getExtension().isComplete(archive)) {
                                        controller = _getExtension().addToQueue(archive, true);
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
                            }, 0, T._.extracting_archive(archive.getName()), T._.extracting_wait(archive.getName()), NewTheme.I().getIcon(IconKey.ICON_ARCHIVE_RUN, 32), null, null) {
                                @Override
                                public ModalityType getModalityType() {
                                    return ModalityType.MODELESS;
                                }
                            };

                            // UIOManager.I().show(class1, impl)
                            UIOManager.I().show(null, dialog);
                            dialog.throwCloseExceptions();

                        } catch (ArchiveException e1) {
                            _getExtension().getLogger().log(e1);
                        }
                    }
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
