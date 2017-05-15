//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.jdownloader.extensions.extraction;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import jd.controlling.TaskQueue;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.images.NewTheme;

/**
 * Updates the Extractionprogess for archives in the downloadlist
 *
 * @author botzi
 *
 */
public class ExtractionListenerList implements ExtractionListener {
    public static final class ExtractPasswordDialog extends InputDialog implements ExtractPasswordDialogInterface {
        private final ExtractionController controller;

        public ExtractPasswordDialog(int flag, String title, String message, String defaultMessage, Icon icon, String okOption, String cancelOption, ExtractionController controller) {
            super(flag, title, message, defaultMessage, icon, okOption, cancelOption);
            this.controller = controller;
            setTimeout(Math.max(1000, CFG_EXTRACTION.CFG.getAskForPasswordDialogTimeoutInMS()));
        }

        @Override
        protected String createReturnValue() {
            return super.createReturnValue();
        }

        @Override
        public boolean isCountdownPausable() {
            return false;
        }

        @Override
        public JComponent layoutDialogContent() {
            final JPanel p = new MigPanel("ins 0,wrap 1", "[]", "[][]");
            if (!StringUtils.isEmpty(message)) {
                textField = new JTextPane() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean getScrollableTracksViewportWidth() {
                        return !BinaryLogic.containsAll(flagMask, Dialog.STYLE_LARGE);
                    }

                    public boolean getScrollableTracksViewportHeight() {
                        return true;
                    }
                };
                textField.setContentType("text/plain");
                textField.setText(message);
                textField.setEditable(false);
                textField.setBackground(null);
                textField.setOpaque(false);
                textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
                textField.setCaretPosition(0);
                p.add(textField, "pushx, growx");
                // inout dialog can become too large(height) if we do not limit the
                // prefered textFIled size here.
                textField.setPreferredSize(textField.getPreferredSize());
            }
            try {
                p.add(SwingUtils.toBold(new JLabel(_GUI.T.ExtractionListenerList_layoutDialogContent_archivename())), "split 2,sizegroup left,alignx left");
                p.add(leftLabel(controller.getArchive().getName()));
                p.add(SwingUtils.toBold(new JLabel(_GUI.T.ExtractionListenerList_layoutDialogContent_filename())), "split 2,sizegroup left,alignx left");
                p.add(leftLabel(controller.getArchive().getArchiveFiles().get(0).getName()));
                p.add(SwingUtils.toBold(new JLabel(_GUI.T.IfFileExistsDialog_layoutDialogContent_filesize2())), "split 2,sizegroup left,alignx left");
                long archiveSize = 0;
                for (ArchiveFile af : controller.getArchive().getArchiveFiles()) {
                    archiveSize += Math.max(0, af.getFileSize());
                }
                p.add(leftLabel(SizeFormatter.formatBytes(archiveSize)));
                for (final ArchiveFile archiveFile : controller.getArchive().getRootArchive().getArchiveFiles()) {
                    if (archiveFile instanceof DownloadLinkArchiveFile) {
                        final FilePackage parentNode = ((DownloadLinkArchiveFile) archiveFile).getDownloadLinks().get(0).getParentNode();
                        if (!FilePackage.isDefaultFilePackage(parentNode)) {
                            p.add(SwingUtils.toBold(new JLabel(_GUI.T.IfFileExistsDialog_layoutDialogContent_package())), "split 2,sizegroup left,alignx left");
                            p.add(leftLabel(parentNode.getName()));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            input = getSmallInputComponent();
            // this.input.setBorder(BorderFactory.createEtchedBorder());
            input.setText(defaultMessage);
            p.add(SwingUtils.toBold(new JLabel(_GUI.T.ExtractionListenerList_layoutDialogContent_password())), "split 2,sizegroup left,alignx left");
            p.add((JComponent) input, "w 450,pushx,growx");
            getDialog().addWindowFocusListener(new WindowFocusListener() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                }

                @Override
                public void windowGainedFocus(WindowEvent e) {
                    TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                        @Override
                        protected Void run() throws RuntimeException {
                            final ArrayList<AbstractNode> selection = new ArrayList<AbstractNode>();
                            for (final ArchiveFile faf : controller.getArchive().getRootArchive().getArchiveFiles()) {
                                if (faf instanceof DownloadLinkArchiveFile) {
                                    selection.addAll(((DownloadLinkArchiveFile) faf).getDownloadLinks());
                                }
                            }
                            DownloadsTableModel.getInstance().setSelectedObjects(selection);
                            return null;
                        }
                    });
                }
            });
            return p;
        }

        private Component leftLabel(String name) {
            JLabel ret = new JLabel(name);
            ret.setHorizontalAlignment(SwingConstants.LEFT);
            return ret;
        }

        @Override
        protected void packed() {
            super.packed();
        }

        @Override
        public ModalityType getModalityType() {
            return ModalityType.MODELESS;
        }

        @Override
        public String getArchiveName() {
            return controller.getArchive().getName();
        }

        @Override
        public ArchiveLinkStructure getArchiveLinkIds() {
            final ArchiveLinkStructure ret = new ArchiveLinkStructure();
            final ArrayList<long[]> allFiles = new ArrayList<long[]>();
            for (final ArchiveFile archiveFile : controller.getArchive().getRootArchive().getArchiveFiles()) {
                if (archiveFile instanceof DownloadLinkArchiveFile) {
                    final List<DownloadLink> downloadLinks = ((DownloadLinkArchiveFile) archiveFile).getDownloadLinks();
                    long[] array = new long[downloadLinks.size()];
                    for (int i = 0; i < downloadLinks.size(); i++) {
                        final DownloadLink downloadLink = downloadLinks.get(i);
                        array[i] = downloadLink.getUniqueID().getID();
                    }
                    allFiles.add(array);
                }
            }
            ret.setAllFiles(allFiles.toArray(new long[][] {}));
            ret.setFirstFile(allFiles.get(0));
            return ret;
        }
    }

    private final ExtractionExtension ex;
    final private Color               green  = Color.GREEN.darker();
    final private Color               yellow = Color.YELLOW.darker();

    protected ExtractionListenerList() {
        this.ex = ExtractionExtension.getInstance();
    }

    private void setStatus(ExtractionController controller, Archive archive, ExtractionStatus status) {
        for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
            archiveFile.setStatus(controller, status);
        }
    }

    private void setMessage(ExtractionController controller, Archive archive, String message) {
        for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
            archiveFile.setMessage(controller, message);
        }
    }

    private void setProgress(ExtractionController controller, Archive archive, long value, long max, Color color) {
        for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
            archiveFile.setProgress(controller, value, max, color);
        }
    }

    private void removePluginProgress(ExtractionController controller, Archive archive) {
        for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
            archiveFile.removePluginProgress(controller);
        }
    }

    public void onExtractionEvent(ExtractionEvent event) {
        final ExtractionController controller = event.getCaller();
        final LogSource logger = controller.getLogger();
        final Archive currentArchive = controller.getArchive();
        final Archive rootArchive = currentArchive.getRootArchive();
        switch (event.getType()) {
        case QUEUED:
            setStatus(controller, rootArchive, ExtractionStatus.IDLE);
            setMessage(controller, rootArchive, T.T.plugins_optional_extraction_status_queued());
            break;
        case EXTRACTION_FAILED:
            logger.warning("Extraction failed");
            setStatus(controller, rootArchive, ExtractionStatus.ERROR);
            String errorMsg = T.T.failed_no_details();
            ArchiveFile lastAccessArchiveFile = null;
            if (controller.getException() != null) {
                errorMsg = T.T.failed(controller.getException().getMessage());
                if (controller.getException() instanceof ExtractionException) {
                    lastAccessArchiveFile = ((ExtractionException) controller.getException()).getLatestAccessedArchiveFile();
                }
            }
            if (currentArchive == rootArchive) {
                for (final ArchiveFile archiveFile : currentArchive.getArchiveFiles()) {
                    if (lastAccessArchiveFile != null && lastAccessArchiveFile == archiveFile) {
                        lastAccessArchiveFile.setStatus(controller, ExtractionStatus.ERROR_CRC);
                    } else {
                        if (currentArchive.getCrcError().contains(archiveFile)) {
                            archiveFile.setStatus(controller, ExtractionStatus.ERROR_CRC);
                        } else {
                            archiveFile.setMessage(controller, errorMsg);
                        }
                    }
                }
            } else {
                for (final ArchiveFile archiveFile : rootArchive.getArchiveFiles()) {
                    archiveFile.setMessage(controller, errorMsg);
                }
            }
            cleanupIncompleteExtraction(controller, currentArchive);
            break;
        case PASSWORD_NEEDED_TO_CONTINUE:
            if (ex.getSettings().isAskForUnknownPasswordsEnabled() || controller.isAskForUnknownPassword()) {
                final ExtractPasswordDialog id = new ExtractPasswordDialog(UIOManager.LOGIC_COUNTDOWN, T.T.ask_for_password(), T.T.plugins_optional_extraction_askForPassword2(), "", NewTheme.I().getIcon(IconKey.ICON_RAR, 32), null, null, controller);
                final String pass = UIOManager.I().show(ExtractPasswordDialogInterface.class, id).getText();
                if (StringUtils.isEmpty(pass)) {
                    setStatus(controller, rootArchive, ExtractionStatus.ERROR_PW);
                } else {
                    currentArchive.setFinalPassword(pass);
                }
            }
            break;
        case START_CRACK_PASSWORD:
            try {
                setMessage(controller, rootArchive, T.T.plugins_optional_extraction_status_crackingpass_progress(((10000 * controller.getCrackProgress()) / Math.max(1, controller.getPasswordListSize())) / 100.00));
            } catch (Throwable e) {
                setMessage(controller, rootArchive, T.T.plugins_optional_extraction_status_crackingpass_progress(0.00d));
            }
            break;
        case START:
            setStatus(controller, rootArchive, ExtractionStatus.RUNNING);
            break;
        case OPEN_ARCHIVE_SUCCESS:
            break;
        case PASSWORD_FOUND:
            setMessage(controller, rootArchive, T.T.plugins_optional_extraction_status_passfound());
            setProgress(controller, rootArchive, 0, 0, green);
            break;
        case PASSWORT_CRACKING:
            final int x = controller.getCrackProgress();
            final int y = Math.max(1, controller.getPasswordListSize());
            try {
                setMessage(controller, rootArchive, T.T.plugins_optional_extraction_status_crackingpass_progress(((10000 * x) / y) / 100.00));
            } catch (Throwable e) {
                setMessage(controller, rootArchive, T.T.plugins_optional_extraction_status_crackingpass_progress(0.00d));
            }
            setProgress(controller, rootArchive, x, y, yellow);
            break;
        case EXTRACTING:
            setMessage(controller, rootArchive, T.T.plugins_optional_extraction_status_extracting2());
            setProgress(controller, rootArchive, controller.getProcessedBytes(), controller.getCompleteBytes(), green);
            break;
        case EXTRACTION_FAILED_CRC:
            logger.warning("Extraction failed(CRC)");
            setStatus(controller, rootArchive, ExtractionStatus.ERROR);
            if (currentArchive == rootArchive) {
                if (currentArchive.getCrcError().size() != 0) {
                    for (final ArchiveFile archiveFile : currentArchive.getCrcError()) {
                        archiveFile.setStatus(controller, ExtractionStatus.ERROR_CRC);
                    }
                } else {
                    final String message = T.T.plugins_optional_extraction_error_extrfailedcrc();
                    for (final ArchiveFile archiveFile : currentArchive.getArchiveFiles()) {
                        archiveFile.setMessage(controller, message);
                    }
                }
            } else {
                final String message = T.T.plugins_optional_extraction_error_extrfailedcrc();
                for (final ArchiveFile archiveFile : rootArchive.getArchiveFiles()) {
                    archiveFile.setMessage(controller, message);
                }
            }
            cleanupIncompleteExtraction(controller, currentArchive);
            break;
        case FINISHED:
            setStatus(controller, rootArchive, ExtractionStatus.SUCCESSFUL);
            final ArrayList<File> files = new ArrayList<File>(currentArchive.getExtractedFiles());
            files.addAll(currentArchive.getSkippedFiles());
            FileCreationManager.getInstance().getEventSender().fireEvent(new FileCreationEvent(controller, FileCreationEvent.Type.NEW_FILES, files.toArray(new File[files.size()])));
            if (ex.getSettings().isDeleteInfoFilesAfterExtraction() && currentArchive.getArchiveType().name().startsWith("RAR")) {
                final File fileOutput = new File(currentArchive.getArchiveFiles().get(0).getFilePath());
                final File infoFiles = new File(fileOutput.getParentFile(), fileOutput.getName().replaceFirst("(?i)(\\.pa?r?t?\\.?[0-9]+\\.rar|\\.rar)$", "") + ".info");
                if (infoFiles.exists() && infoFiles.delete()) {
                    logger.info(infoFiles.getName() + " removed");
                }
            }
            break;
        case NOT_ENOUGH_SPACE:
            setStatus(controller, rootArchive, ExtractionStatus.ERROR_NOT_ENOUGH_SPACE);
            break;
        case CLEANUP:
            try {
                logger.warning("Cleanup");
                if (controller.gotKilled()) {
                    setStatus(controller, rootArchive, null);
                    cleanupIncompleteExtraction(controller, currentArchive);
                }
            } finally {
                removePluginProgress(controller, rootArchive);
                if (controller.gotKilled()) {
                    setStatus(controller, rootArchive, null);
                } else if (controller.isSuccessful()) {
                    for (final ArchiveFile archiveLink : currentArchive.getArchiveFiles()) {
                        archiveLink.onCleanedUp(controller);
                    }
                    controller.removeArchiveFiles();
                }
            }
            break;
        case FILE_NOT_FOUND:
            logger.warning("FileNotFound");
            if (currentArchive.getCrcError().size() != 0) {
                setStatus(controller, rootArchive, ExtractionStatus.ERROR_CRC);
            } else {
                setStatus(controller, rootArchive, ExtractionStatus.ERRROR_FILE_NOT_FOUND);
            }
            cleanupIncompleteExtraction(controller, currentArchive);
            break;
        }
    }

    private void cleanupIncompleteExtraction(ExtractionController controller, Archive archive) {
        final LogSource logger = controller.getLogger();
        for (final File extractedFile : archive.getExtractedFiles()) {
            if (extractedFile.exists()) {
                if (!FileCreationManager.getInstance().delete(extractedFile, null)) {
                    logger.warning("Could not delete file " + extractedFile.getAbsolutePath());
                } else {
                    logger.warning("Deleted file " + extractedFile.getAbsolutePath());
                }
            }
        }
    }
}