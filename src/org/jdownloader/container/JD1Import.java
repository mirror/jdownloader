package org.jdownloader.container;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.config.DatabaseConnector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;
import jd.gui.UserIO;
import jd.nutils.JDHash;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginsC;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.HexInputStream;

public class JD1Import extends PluginsC {

    public JD1Import() {
        super("JD1 Import", "file://.+(\\.jdc$|database\\.script$)", "$Revision: 21176 $");
    }

    @SuppressWarnings("unchecked")
    public ContainerStatus callDecryption(File jdcFile) {
        ContainerStatus cs = new ContainerStatus(jdcFile);
        cls = new ArrayList<CrawledLink>();
        FileInputStream fis = null;
        try {
            List<FilePackage> packages = null;
            if (jdcFile.getName().endsWith(".jdc")) {
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
                    logger.severe("PW wrong?!");
                    throw e;
                }
                packages = (ArrayList<FilePackage>) in.readObject();
            } else {
                packages = (List<FilePackage>) new DatabaseConnector(jdcFile.getAbsolutePath()).getLinks();
            }
            if (packages != null && packages.size() > 0) {
                for (FilePackage p : packages) {
                    PackageInfo packageInfo = new PackageInfo();
                    packageInfo.setComment(p.getComment());
                    packageInfo.setName(p.getName());
                    if (new File(p.getDownloadDirectory()).exists()) packageInfo.setDestinationFolder(p.getDownloadDirectory());
                    for (DownloadLink dl : p.getChildren()) {
                        CrawledLink cl = new CrawledLink(dl);
                        cl.setDesiredPackageInfo(packageInfo);
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
                if (fis != null) fis.close();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }

}
