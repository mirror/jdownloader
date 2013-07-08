package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.io.JDFileFilter;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.plugins.controller.container.ContainerPluginController;

public class AddContainerAction extends AppAction {
    /**
     * @param selection
     *            TODO
     * 
     */
    public AddContainerAction(SelectionInfo<CrawledPackage, CrawledLink> selection) {
        setIconKey("addContainer");
        setName(_GUI._.AddContainerAction());
        setAccelerator(KeyEvent.VK_O);
    }

    private static final long serialVersionUID = -1758454550263991986L;

    public void actionPerformed(ActionEvent e) {

        try {
            String exts = ContainerPluginController.getInstance().getContainerExtensions(null);

            ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.AddContainerAction_actionPerformed_(), null, null);
            d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
            d.setFileFilter(new JDFileFilter(_GUI._.AddContainerAction_actionPerformed_extensions(exts), exts, true));
            d.setType(FileChooserType.OPEN_DIALOG);
            d.setMultiSelection(true);
            Dialog.I().showDialog(d);

            File[] filterFiles = d.getSelection();
            if (filterFiles == null) return;

            final StringBuilder sb = new StringBuilder();
            final StringBuilder list = new StringBuilder();
            for (File r : filterFiles) {
                if (sb.length() > 0) {
                    sb.append("\r\n");
                    list.append("\r\n");
                }
                sb.append("file://");
                list.append(r.getAbsolutePath());
                sb.append(r.getAbsolutePath());
            }
            LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(sb.toString()));

        } catch (DialogNoAnswerException e1) {
        }
    }

}
