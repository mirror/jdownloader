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

import javax.swing.ImageIcon;
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
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
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

        public ExtractPasswordDialog(int flag, String title, String message, String defaultMessage, ImageIcon icon, String okOption, String cancelOption, ExtractionController controller) {
            super(flag, title, message, defaultMessage, icon, okOption, cancelOption);
            this.controller = controller;
            setTimeout(Math.max(1000, CFG_EXTRACTION.CFG.getAskForPasswordDialogTimeoutInMS()));
        }

        @Override
        protected String createReturnValue() {

            return super.createReturnValue();

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
                DownloadLink downloadLink = null;
                ArchiveFile faf = controller.getArchiv().getFirstArchiveFile();
                if (faf instanceof DownloadLinkArchiveFile) {
                    downloadLink = ((DownloadLinkArchiveFile) faf).getDownloadLinks().get(0);
                }
                if (downloadLink != null) {
                    String packagename = downloadLink.getParentNode().getName();
                    p.add(SwingUtils.toBold(new JLabel(_GUI._.ExtractionListenerList_layoutDialogContent_archivename())), "split 2,sizegroup left,alignx left");
                    p.add(leftLabel(controller.getArchiv().getName()));
                    p.add(SwingUtils.toBold(new JLabel(_GUI._.ExtractionListenerList_layoutDialogContent_filename())), "split 2,sizegroup left,alignx left");
                    p.add(leftLabel(new File(downloadLink.getFileOutput()).getName()));
                    p.add(SwingUtils.toBold(new JLabel(_GUI._.IfFileExistsDialog_layoutDialogContent_filesize2())), "split 2,sizegroup left,alignx left");
                    long bytes = 0;
                    for (ArchiveFile af : controller.getArchiv().getArchiveFiles()) {
                        bytes += af.getFileSize();
                    }
                    p.add(leftLabel(SizeFormatter.formatBytes(bytes)));
                    p.add(SwingUtils.toBold(new JLabel(_GUI._.IfFileExistsDialog_layoutDialogContent_package())), "split 2,sizegroup left,alignx left");
                    p.add(leftLabel(packagename));
                }
            } catch (Exception e) {
            }
            input = getSmallInputComponent();
            // this.input.setBorder(BorderFactory.createEtchedBorder());
            input.setText(defaultMessage);
            p.add(SwingUtils.toBold(new JLabel(_GUI._.ExtractionListenerList_layoutDialogContent_password())), "split 2,sizegroup left,alignx left");
            p.add((JComponent) input, "w 450,pushx,growx");
            getDialog().addWindowFocusListener(new WindowFocusListener() {

                @Override
                public void windowLostFocus(WindowEvent e) {
                }

                @Override
                public void windowGainedFocus(WindowEvent e) {
                    ArrayList<AbstractNode> links = new ArrayList<AbstractNode>();

                    for (ArchiveFile faf : controller.getArchiv().getArchiveFiles()) {
                        links.addAll(((DownloadLinkArchiveFile) faf).getDownloadLinks());
                    }
                    SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, links, true);

                    ArrayList<AbstractNode> corrected = new ArrayList<AbstractNode>();
                    for (PackageView<FilePackage, DownloadLink> pv : si.getPackageViews()) {
                        if (pv.isFull()) {
                            corrected.add(pv.getPackage());
                        }
                        corrected.addAll(pv.getChildren());
                    }

                    DownloadsTableModel.getInstance().setSelectedObjects(corrected);
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
            return controller.getArchiv().getName();
        }

        @Override
        public ArchiveLinkStructure getArchiveLinkIds() {
            ArchiveLinkStructure ret = new ArchiveLinkStructure();
            ArchiveFile faf = controller.getArchiv().getFirstArchiveFile();
            if (faf instanceof DownloadLinkArchiveFile) {

                List<DownloadLink> links = ((DownloadLinkArchiveFile) faf).getDownloadLinks();
                long[] array = new long[links.size()];
                for (int i = 0; i < links.size(); i++) {
                    DownloadLink dl = links.get(i);
                    array[i] = dl.getUniqueID().getID();

                }
                ret.setFirstFile(array);
            }

            ArrayList<long[]> allFiles = new ArrayList<long[]>();
            for (ArchiveFile af : controller.getArchiv().getArchiveFiles()) {

                if (af instanceof DownloadLinkArchiveFile) {
                    List<DownloadLink> links = ((DownloadLinkArchiveFile) af).getDownloadLinks();

                    long[] array = new long[links.size()];
                    for (int i = 0; i < links.size(); i++) {
                        DownloadLink dl = links.get(i);
                        array[i] = dl.getUniqueID().getID();

                    }
                    allFiles.add(array);

                }
            }

            ret.setAllFiles(allFiles.toArray(new long[][] {}));
            return ret;
        }

    }

    private ExtractionExtension ex;

    final private Color         green  = Color.GREEN.darker();
    final private Color         yellow = Color.YELLOW.darker();

    ExtractionListenerList() {
        this.ex = ExtractionExtension.getInstance();
    }

    public void onExtractionEvent(ExtractionEvent event) {
        final ExtractionController controller = event.getCaller();
        final LogSource logger = controller.getLogger();

        switch (event.getType()) {
        case QUEUED:
            controller.getArchiv().setStatus(controller, ExtractionStatus.IDLE);
            controller.getArchiv().getFirstArchiveFile().setMessage(controller, T._.plugins_optional_extraction_status_queued());
            break;
        case EXTRACTION_FAILED:
            try {
                logger.warning("Extraction failed");
                controller.getArchiv().setStatus(controller, ExtractionStatus.ERROR);
                ArchiveFile af = null;
                String errorMsg = T._.failed_no_details();
                if (controller.getException() != null) {
                    errorMsg = T._.failed(controller.getException().getMessage());
                    if (controller.getException() instanceof ExtractionException) {
                        af = ((ExtractionException) controller.getException()).getLatestAccessedArchiveFile();
                    }
                }
                for (ArchiveFile link : controller.getArchiv().getArchiveFiles()) {
                    if (link == null) continue;
                    if (af != null && af == link) {
                        link.setStatus(controller, ExtractionStatus.ERROR_CRC);
                    } else {
                        link.setStatus(controller, ExtractionStatus.ERROR);
                        link.setMessage(controller, errorMsg);
                    }
                }
                cleanUpIncomplete(controller);
            } finally {
                controller.getArchiv().setActive(false);
                ex.onFinished(controller);
            }
            break;
        case PASSWORD_NEEDED_TO_CONTINUE:
            if (ex.getSettings().isAskForUnknownPasswordsEnabled() || controller.isAskForUnknownPassword()) {

                ExtractPasswordDialog id = new ExtractPasswordDialog(UIOManager.LOGIC_COUNTDOWN, T._.ask_for_password(), T._.plugins_optional_extraction_askForPassword2(), "", NewTheme.I().getIcon(IconKey.ICON_RAR, 32), null, null, controller);

                String pass = UIOManager.I().show(ExtractPasswordDialogInterface.class, id).getText();
                if (pass == null || pass.length() == 0) {
                    controller.getArchiv().getFirstArchiveFile().setStatus(controller, ExtractionStatus.ERROR_PW);
                    ex.onFinished(controller);
                    break;
                }
                controller.getArchiv().setFinalPassword(pass);
            }
            break;
        case START_CRACK_PASSWORD:
            try {
                controller.getArchiv().getFirstArchiveFile().setMessage(controller, T._.plugins_optional_extraction_status_crackingpass_progress(((10000 * controller.getCrackProgress()) / controller.getPasswordListSize()) / 100.00));
            } catch (Throwable e) {
                controller.getArchiv().getFirstArchiveFile().setMessage(controller, T._.plugins_optional_extraction_status_crackingpass_progress(0.00d));
            }
            break;
        case START:
            controller.getArchiv().setStatus(controller, ExtractionStatus.RUNNING);
            break;
        case OPEN_ARCHIVE_SUCCESS:
            break;
        case PASSWORD_FOUND:
            controller.getArchiv().getFirstArchiveFile().setMessage(controller, T._.plugins_optional_extraction_status_passfound());
            controller.getArchiv().getFirstArchiveFile().setProgress(controller, 0, 0, green);
            break;
        case PASSWORT_CRACKING:
            try {
                controller.getArchiv().getFirstArchiveFile().setMessage(controller, T._.plugins_optional_extraction_status_crackingpass_progress(((10000 * controller.getCrackProgress()) / controller.getPasswordListSize()) / 100.00));
            } catch (Throwable e) {
                controller.getArchiv().getFirstArchiveFile().setMessage(controller, T._.plugins_optional_extraction_status_crackingpass_progress(0.00d));
            }
            controller.getArchiv().getFirstArchiveFile().setProgress(controller, controller.getCrackProgress(), controller.getPasswordListSize(), yellow);
            break;
        case EXTRACTING:
            controller.getArchiv().getFirstArchiveFile().setMessage(controller, T._.plugins_optional_extraction_status_extracting2());
            controller.getArchiv().getFirstArchiveFile().setProgress(controller, controller.getProcessedBytes(), controller.getCompleteBytes(), green);
            break;
        case EXTRACTION_FAILED_CRC:
            try {
                controller.getArchiv().setStatus(controller, ExtractionStatus.ERROR);
                logger.warning("Extraction failed(CRC)");
                if (controller.getArchiv().getCrcError().size() != 0) {
                    for (ArchiveFile link : controller.getArchiv().getCrcError()) {
                        if (link == null) continue;
                        link.setStatus(controller, ExtractionStatus.ERROR_CRC);
                    }
                } else {
                    for (ArchiveFile link : controller.getArchiv().getArchiveFiles()) {
                        if (link == null) continue;
                        link.setMessage(controller, T._.plugins_optional_extraction_error_extrfailedcrc());
                    }
                }
                cleanUpIncomplete(controller);
            } finally {
                controller.getArchiv().setActive(false);
                ex.onFinished(controller);
            }
            break;
        case FINISHED:
            try {
                controller.getArchiv().setStatus(controller, ExtractionStatus.SUCCESSFUL);
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        FileCreationManager.getInstance().getEventSender().fireEvent(new FileCreationEvent(controller, FileCreationEvent.Type.NEW_FILES, controller.getArchiv().getExtractedFiles().toArray(new File[controller.getArchiv().getExtractedFiles().size()])));
                        if (ex.getSettings().isDeleteInfoFilesAfterExtraction()) {
                            File fileOutput = new File(controller.getArchiv().getFirstArchiveFile().getFilePath());
                            File infoFiles = new File(fileOutput.getParentFile(), fileOutput.getName().replaceFirst("(?i)(\\.pa?r?t?\\.?[0-9]+\\.rar|\\.rar)$", "") + ".info");
                            if (infoFiles.exists() && infoFiles.delete()) {
                                logger.info(infoFiles.getName() + " removed");
                            }
                        }
                        return null;
                    }
                });
            } finally {
                controller.getArchiv().setActive(false);
                ex.onFinished(controller);
            }
            break;
        case NOT_ENOUGH_SPACE:
            controller.getArchiv().setStatus(controller, ExtractionStatus.ERROR_NOT_ENOUGH_SPACE);
            ex.onFinished(controller);
            break;
        case CLEANUP:
            try {
                logger.warning("Cleanup");
                ArchiveFile af = null;
                if (controller.getException() != null) {
                    if (controller.getException() instanceof ExtractionException) {
                        af = ((ExtractionException) controller.getException()).getLatestAccessedArchiveFile();
                        af.deleteFile(FileCreationManager.DeleteOption.RECYCLE);
                    }
                }
                if (controller.gotKilled()) {
                    controller.getArchiv().getFirstArchiveFile().setMessage(controller, null);
                    for (File f : controller.getArchiv().getExtractedFiles()) {
                        if (f.exists()) {
                            if (!FileCreationManager.getInstance().delete(f, null)) {
                                logger.warning("Could not delete file " + f.getAbsolutePath());
                            } else {
                                logger.warning("Deleted file " + f.getAbsolutePath());
                            }
                        }
                    }
                }
            } finally {
                controller.getArchiv().setActive(false);
                controller.getArchiv().getFirstArchiveFile().setProgress(controller, 0, 0, null);
                ex.removeArchive(controller.getArchiv());
                if (controller.isSuccessful() && !controller.getArchiv().getGotInterrupted()) {

                    for (ArchiveFile link : controller.getArchiv().getArchiveFiles()) {
                        if (link == null) continue;
                        link.onCleanedUp(controller);
                    }
                    controller.removeArchiveFiles();
                }
            }
            break;
        case FILE_NOT_FOUND:
            try {
                logger.warning("FileNotFound");
                if (controller.getArchiv().getCrcError().size() != 0) {
                    controller.getArchiv().setStatus(controller, ExtractionStatus.ERROR_CRC);
                } else {
                    controller.getArchiv().setStatus(controller, ExtractionStatus.ERRROR_FILE_NOT_FOUND);
                }
                for (File f : controller.getArchiv().getExtractedFiles()) {
                    if (f.exists()) {
                        if (!FileCreationManager.getInstance().delete(f, null)) {
                            logger.warning("Could not delete file " + f.getAbsolutePath());
                        } else {
                            logger.warning("Deleted file " + f.getAbsolutePath());
                        }
                    }
                }
            } finally {
                controller.getArchiv().setActive(false);
                ex.onFinished(controller);
            }
            break;
        }
    }

    private void cleanUpIncomplete(ExtractionController controller) {
        final LogSource logger = controller.getLogger();
        for (File f : controller.getArchiv().getExtractedFiles()) {
            if (f.exists()) {
                if (!FileCreationManager.getInstance().delete(f, null)) {
                    logger.warning("Could not delete file " + f.getAbsolutePath());
                } else {
                    logger.warning("Deleted file " + f.getAbsolutePath());
                }
            }
        }
    }
}