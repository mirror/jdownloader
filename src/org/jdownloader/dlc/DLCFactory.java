package org.jdownloader.dlc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.filechooser.FileFilter;

import jd.controlling.linkcollector.LinknameCleaner;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.http.Browser;
import jd.plugins.DownloadLink;

import org.appwork.uio.CloseReason;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.appwork.utils.swing.dialog.MessageDialogImpl;
import org.jdownloader.container.D;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.images.NewTheme;

public class DLCFactory extends D {
    public String encryptDLC(String xml) {
        final String[] encrypt = encrypt(xml);
        if (encrypt == null) {
            logger.severe("Container Encryption failed.");
            return null;
        }
        final String key = encrypt[1];
        xml = encrypt[0];
        final String service = "http://service.jdownloader.org/dlcrypt/service.php";
        try {
            final String dlcKey = callService(service, key);
            if (dlcKey == null) {
                return null;
            }
            return xml + dlcKey;
        } catch (final Exception e) {
            logger.log(e);
        }
        return null;
    }

    private String callService(final String service, final String key) throws Exception {
        logger.finer("Call " + service);
        final Browser br = new Browser();
        br.postPage(service, "jd=1&srcType=plain&data=" + key);
        logger.info("Call re: " + br.toString());
        if (!br.getHttpConnection().isOK() || !br.containsHTML("<rc>")) {
            return null;
        } else {
            final String dlcKey = br.getRegex("<rc>(.*?)</rc>").getMatch(0);
            if (dlcKey.trim().length() < 80) {
                return null;
            }
            return dlcKey;
        }
    }

    public void createDLC(final List<DownloadLink> links) {
        try {
            final String xml = createContainerStringByLinks(links);
            encryptAndWriteXML(xml, getPreSetFolder(links), getPreSetFileName(links));
        } catch (DialogCanceledException e) {
        } catch (DialogClosedException e) {
        } catch (IOException e) {
            Dialog.getInstance().showExceptionDialog("DLC Error", e.getMessage(), e);
        }
    }

    private String getPreSetFileName(final List<? extends AbstractPackageChildrenNode<?>> nodes) {
        if (nodes != null && nodes.size() > 0) {
            if (nodes.size() == 1) {
                final String name = nodes.get(0).getName();
                return LinknameCleaner.cleanFileName(name, false, false, LinknameCleaner.EXTENSION_SETTINGS.REMOVE_ALL, false) + ".dlc";
            } else {
                for (AbstractPackageChildrenNode node : nodes) {
                    final AbstractPackageNode parent = ((AbstractPackageNode) nodes.get(0).getParentNode());
                    if (parent != null) {
                        return parent.getName() + ".dlc";
                    }
                }
            }
        }
        return null;
    }

    private File getPreSetFolder(final List<? extends AbstractPackageChildrenNode<?>> nodes) {
        if (nodes != null && nodes.size() > 0) {
            for (final AbstractPackageChildrenNode node : nodes) {
                final AbstractPackageNode parent = ((AbstractPackageNode) nodes.get(0).getParentNode());
                if (parent != null) {
                    return LinkTreeUtils.getDownloadDirectory(parent);
                }
            }
        }
        return null;
    }

    public void createDLCByCrawledLinks(final List<CrawledLink> links) {
        try {
            final String xml = createContainerStringByLinks(links);
            encryptAndWriteXML(xml, getPreSetFolder(links), getPreSetFileName(links));
        } catch (DialogCanceledException e) {
        } catch (DialogClosedException e) {
        } catch (Exception e) {
            Dialog.getInstance().showExceptionDialog("DLC Error", e.getMessage(), e);
        }
    }

    /**
     * @param xml
     * @throws DialogClosedException
     * @throws DialogCanceledException
     * @throws IOException
     */
    protected void encryptAndWriteXML(String xml, final File preSetFolder, final String preSetFilename) throws DialogClosedException, DialogCanceledException, IOException {
        ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI.T.CreateDLCAction_actionPerformed_title_(), null, null) {
            {
                if (preSetFolder != null && preSetFolder.isDirectory()) {
                    setPreSelection(preSetFolder);
                }
            }

            @Override
            public JComponent layoutDialogContent() {
                final JComponent ret = super.layoutDialogContent();
                final File parent = fc.getCurrentDirectory();
                if (parent != null && StringUtils.isNotEmpty(preSetFilename)) {
                    fc.setSelectedFile(new File(parent, CrossSystem.alleviatePathParts(preSetFilename)));
                }
                return ret;
            }
        };
        d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
        d.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "*.dlc (DownloadLinkContainer)";
            }

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".dlc");
            }
        });
        d.setType(FileChooserType.SAVE_DIALOG);
        d.setMultiSelection(false);
        Dialog.I().showDialog(d);
        File file = d.getSelectedFile();
        if (file == null) {
            new MessageDialogImpl(0, _GUI.T.DLCFactory_createDLCByCrawledLinks_nofile_title(), _GUI.T.DLCFactory_createDLCByCrawledLinks_nofile_msg(), NewTheme.I().getIcon(IconKey.ICON_WARNING, 32), null).show();
            return;
        }
        String fileName = CrossSystem.alleviatePathParts(file.getName());
        if (!StringUtils.endsWithCaseInsensitive(fileName, ".dlc")) {
            fileName = fileName + ".dlc";
        }
        file = new File(file.getParentFile(), fileName);
        if (!writeDLC(file, xml)) {
            logger.severe("Container creation failed");
            Dialog.getInstance().showErrorDialog("Container encryption failed");
        }
    }

    /**
     * @param file
     * @param xml
     * @throws DialogClosedException
     * @throws DialogCanceledException
     * @throws IOException
     */
    protected boolean writeDLC(File file, String xml) throws DialogClosedException, DialogCanceledException, IOException {
        if (xml != null) {
            final String cipher = encryptDLC(xml);
            if (cipher != null) {
                if (file.exists()) {
                    new ConfirmDialog(0, _GUI.T.lit_file_exists(), _GUI.T.lit_file_already_exists_overwrite_question(file.getAbsolutePath())).show().throwCloseExceptions();
                    FileCreationManager.getInstance().delete(file, null);
                }
                IO.writeStringToFile(file, cipher);
                if (new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.DLCFactory_writeDLC_success_ok(), _GUI.T.DLCFactory_createDLC_created_(file.getAbsolutePath()), NewTheme.I().getIcon(IconKey.ICON_LOGO_DLC, 32), _GUI.T.DLCFactory_writeDLC_showpath(), _GUI.T.lit_close()) {
                    public String getDontShowAgainKey() {
                        return "createDLC";
                    };
                }.show().getCloseReason() == CloseReason.OK) {
                    CrossSystem.showInExplorer(file);
                }
                return true;
            }
        }
        return false;
    }
}
