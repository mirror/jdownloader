package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RenamePackageAction extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = -8654376644826310521L;
    private CrawledPackage    pkg;

    public RenamePackageAction(CrawledPackage pkg) {
        this.pkg = pkg;
        setName(_GUI._.RenamePackageAction_RenamePackageAction_());
        setIconKey("edit");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            final String newName = Dialog.getInstance().showInputDialog(0, _GUI._.RenamePackageAction_RenamePackageAction_(), null, pkg.getName(), NewTheme.I().getIcon("edit", 32), null, null);
            if (newName == null || newName.trim().length() == 0 || newName.equalsIgnoreCase(pkg.getName())) return;
            IOEQ.add(new Runnable() {

                public void run() {
                    pkg.setName(newName.trim());
                }

            }, true);
        } catch (DialogNoAnswerException e1) {
        }

    }

    @Override
    public boolean isEnabled() {
        return pkg != null;
    }

}
