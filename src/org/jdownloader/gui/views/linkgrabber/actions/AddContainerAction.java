package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.filechooser.FileFilter;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.plugins.controller.container.ContainerPluginController;

public class AddContainerAction extends CustomizableAppAction {
    /**
     * @param selection
     *            TODO
     *
     */
    public AddContainerAction() {
        setSmallIcon(new BadgeIcon("logo/dlc", "add", 32, 24, 2, 6));
        setName(_GUI.T.AddContainerAction());
        setAccelerator(KeyEvent.VK_O);
    }

    private static final long serialVersionUID = -1758454550263991986L;

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            final FileFilter[] fileFilter = ContainerPluginController.getInstance().getContainerFileFilter(null);
            ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI.T.AddContainerAction_actionPerformed_(), null, null);
            d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
            d.setFileFilter(fileFilter);
            d.setType(FileChooserType.OPEN_DIALOG);
            d.setMultiSelection(true);
            Dialog.I().showDialog(d);
            final File[] files = d.getSelection();
            if (files == null || files.length == 0) {
                return;
            }
            final StringBuilder sb = new StringBuilder();
            for (final File file : files) {
                if (sb.length() > 0) {
                    sb.append("\r\n");
                }
                sb.append(file.toURI().toString());
            }
            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.ADD_CONTAINER_ACTION.getLinkOriginDetails(), sb.toString()));
        } catch (DialogNoAnswerException e1) {
        }
    }
}
