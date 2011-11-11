package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class EditFilenameAction extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = -5379433013870923363L;
    private CrawledLink       link;

    public EditFilenameAction(CrawledLink link) {
        this.link = link;
        setName(_GUI._.EditFilenameAction_EditFilenameAction_());
        setIconKey("edit");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            final String newName = Dialog.getInstance().showInputDialog(0, _GUI._.EditFilenameAction_EditFilenameAction_(), null, link.getName(), NewTheme.I().getIcon("edit", 32), null, null);
            if (newName == null || newName.trim().length() == 0 || newName.equalsIgnoreCase(link.getName())) return;
            IOEQ.add(new Runnable() {

                public void run() {
                    link.setForcedName(newName.trim());
                }

            }, true);
        } catch (DialogNoAnswerException e1) {
        }
    }

    @Override
    public boolean isEnabled() {
        return link != null;
    }

}
