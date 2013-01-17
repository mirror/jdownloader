package org.jdownloader.container;

import java.io.File;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.UserIO;
import jd.plugins.ContainerStatus;
import jd.plugins.PluginsC;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.container.sft.FileInfoDialog;
import org.jdownloader.container.sft.sftBinary;
import org.jdownloader.container.sft.sftContainer;

public class SFT extends PluginsC {

    public SFT() {
        super("SFT", "file://.+\\.sft", "$Revision: $");
    }

    @Override
    public ContainerStatus callDecryption(File file) {
        ContainerStatus cs = new ContainerStatus(file);

        try {
            sftContainer container = sftBinary.load(file);

            FileInfoDialog dialog = new FileInfoDialog(container);
            Dialog.getInstance().showDialog(dialog);

            if (container.isDecrypted()) {
                ArrayList<String> linkList = container.getFormatedLinks();

                if (!linkList.isEmpty()) {
                    for (String element : linkList) {
                        cls.add(new CrawledLink(element));
                    }
                    cs.setStatus(ContainerStatus.STATUS_FINISHED);
                } else {
                    throw new Exception("container didn't contain any ftp links");
                }
            } else {
                cs.setStatus(ContainerStatus.STATUS_FAILED);
            }
        } catch (DialogNoAnswerException e) {
            cs.setStatus(ContainerStatus.STATUS_FAILED);
        } catch (Exception e) {
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            if ((e.getMessage() != null) | (e.getMessage().length() > 0))
                UserIO.getInstance().requestMessageDialog(e.getMessage());
            else {
                UserIO.getInstance().requestMessageDialog("sft decrypt error");
            }
        }

        return cs;
    }

    @Override
    public String[] encrypt(String plain) {
        return null;
    }

}