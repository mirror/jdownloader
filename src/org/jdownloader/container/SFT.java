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
        super("SFT", "file:/.+\\.sft$", "$Revision$");
    }

    public SFT newPluginInstance() {
        return new SFT();
    }

    @Override
    public ContainerStatus callDecryption(File file) {
        final ContainerStatus cs = new ContainerStatus(file);
        cs.setStatus(ContainerStatus.STATUS_FAILED);
        try {
            final sftContainer container = sftBinary.load(file);
            final FileInfoDialog dialog = new FileInfoDialog(container);
            Dialog.getInstance().showDialog(dialog);
            if (container.isDecrypted()) {
                final ArrayList<String> linkList = container.getFormatedLinks();
                if (!linkList.isEmpty()) {
                    for (String element : linkList) {
                        cls.add(new CrawledLink(element));
                    }
                    cs.setStatus(ContainerStatus.STATUS_FINISHED);
                } else {
                    throw new Exception("container didn't contain any ftp links");
                }
            }
        } catch (DialogNoAnswerException e) {
            cs.setStatus(ContainerStatus.STATUS_ABORT);
        } catch (Exception e) {
            logger.log(e);
            cs.setStatus(ContainerStatus.STATUS_FAILED);
            if ((e.getMessage() != null) | (e.getMessage().length() > 0)) {
                UserIO.getInstance().requestMessageDialog(e.getMessage());
            } else {
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