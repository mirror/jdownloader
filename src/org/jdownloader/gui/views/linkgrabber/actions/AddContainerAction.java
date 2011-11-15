package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.nutils.io.JDFileFilter;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.container.ContainerPluginController;

public class AddContainerAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = -1758454550263991986L;

    {
        putValue(SMALL_ICON, NewTheme.I().getIcon("addContainer", 16));
        putValue(NAME, _GUI._.AddContainerAction());

    }

    public void actionPerformed(ActionEvent e) {
        File[] ret;
        try {
            String exts = ContainerPluginController.getInstance().getContainerExtensions(null);
            ret = Dialog.getInstance().showFileChooser("loaddlc", _GUI._.AddContainerAction_actionPerformed_(), FileChooserSelectionMode.FILES_ONLY, new JDFileFilter(_GUI._.AddContainerAction_actionPerformed_extensions(exts), exts, true), false, FileChooserType.OPEN_DIALOG, null);

            if (ret == null) return;
            StringBuilder sb = new StringBuilder();
            for (File r : ret) {
                if (sb.length() > 0) sb.append("\r\n");
                sb.append("file://");
                sb.append(r.getAbsolutePath());
            }
            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(sb.toString()));
        } catch (DialogNoAnswerException e1) {
        }
    }

}
