package org.jdownloader.container;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.config.DatabaseConnector;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;
import jd.gui.UserIO;
import jd.nutils.JDHash;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginsC;
import jd.utils.locale.JDL;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.HexInputStream;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class JD1Import extends PluginsC {
    public JD1Import() {
        super("JD1 Import", "file:/.+(\\.jdc|database\\.script)$", "$Revision: 21176 $");
    }

    public JD1Import newPluginInstance() {
        return new JD1Import();
    }

    @Override
    public ArrayList<CrawledLink> decryptContainer(final CrawledLink source) {
        final LinkOriginDetails origin = source.getOrigin();
        if (origin != null && LinkOrigin.CLIPBOARD.equals(origin.getOrigin())) {
            return null;
        } else {
            return super.decryptContainer(source);
        }
    }

    @SuppressWarnings("unchecked")
    public ContainerStatus callDecryption(File jdcFile) {
        final ContainerStatus cs = new ContainerStatus(jdcFile);
        cls = new ArrayList<CrawledLink>();
        FileInputStream fis = null;
        try {
            List<FilePackage> packages = null;
            final boolean jd1Database;
            if (jdcFile.getName().endsWith(".jdc")) {
                jd1Database = false;
                fis = new FileInputStream(jdcFile);
                String hexString = (JDHash.getMD5(UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, JDL.L("jd.gui.swing.jdgui.menu.actions.BackupLinkListAction.password", "Enter Encryption Password"), "jddefault")));
                if (StringUtils.isEmpty(hexString)) {
                    logger.info("No pw entered!");
                    cs.setStatus(ContainerStatus.STATUS_FAILED);
                    return cs;
                }
                byte[] decryptKey = HexFormatter.hexToByteArray(hexString);
                final IvParameterSpec ivSpec = new IvParameterSpec(decryptKey);
                final SecretKeySpec skeySpec = new SecretKeySpec(decryptKey, "AES");
                final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                ObjectInputStream in = null;
                try {
                    in = new ObjectInputStream(new HexInputStream(new CipherInputStream(fis, cipher)));
                } catch (Throwable e) {
                    logger.severe("PW wrong?");
                    throw e;
                }
                packages = (ArrayList<FilePackage>) in.readObject();
            } else {
                jd1Database = true;
                packages = (List<FilePackage>) new DatabaseConnector(jdcFile.getAbsolutePath()).getLinks();
            }
            if (packages != null && packages.size() > 0) {
                if (jd1Database) {
                    try {
                        int links = 0;
                        for (final FilePackage p : packages) {
                            p.getUniqueID().refresh();
                            for (final DownloadLink downloadLink : p.getChildren()) {
                                downloadLink.getUniqueID().refresh();
                            }
                            links += p.size();
                        }
                        final ConfirmDialog d = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, _GUI.T.jd1_import_title(), _GUI.T.jd1_import_message(packages.size(), links), new AbstractIcon(IconKey.ICON_QUESTION, 16), _GUI.T.jd_gui_swing_jdgui_views_downloadview_tab_title(), _GUI.T.jd_gui_swing_jdgui_views_linkgrabberview_tab_title()) {
                            @Override
                            public ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            }
                        };
                        UIOManager.I().show(ConfirmDialogInterface.class, d).throwCloseExceptions();
                        final LinkedList<FilePackage> fps = new LinkedList<FilePackage>(packages);
                        DownloadController.getInstance().importList(fps);
                        cs.setStatus(ContainerStatus.STATUS_FINISHED);
                        return cs;
                    } catch (DialogNoAnswerException e) {
                        if (e.isCausedbyESC() || e.isCausedByTimeout()) {
                            cs.setStatus(ContainerStatus.STATUS_ABORT);
                            return cs;
                        }
                    }
                }
                for (final FilePackage p : packages) {
                    final PackageInfo packageInfo = new PackageInfo();
                    packageInfo.setName(p.getName());
                    if (StringUtils.isNotEmpty(p.getDownloadDirectory()) && new File(p.getDownloadDirectory()).isDirectory()) {
                        packageInfo.setDestinationFolder(CrossSystem.fixPathSeparators(p.getDownloadDirectory() + File.separator));
                    }
                    for (final DownloadLink dl : p.getChildren()) {
                        final CrawledLink cl = new CrawledLink(dl);
                        if (packageInfo.isNotEmpty()) {
                            cl.setDesiredPackageInfo(packageInfo.getCopy());
                        }
                        cls.add(cl);
                    }
                }
                cs.setStatus(ContainerStatus.STATUS_FINISHED);
                return cs;
            }
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            return cs;
        } catch (Throwable e) {
            logger.log(e);
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            return cs;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }
}
