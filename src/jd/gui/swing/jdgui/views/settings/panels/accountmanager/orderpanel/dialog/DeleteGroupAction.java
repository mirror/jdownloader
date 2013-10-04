package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountInterface;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.GroupWrapper;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.hosterrule.AccountGroup;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class DeleteGroupAction extends AppAction {
    private HosterPriorityTableModel model;

    public DeleteGroupAction(HosterPriorityTableModel model) {
        setName(_GUI._.DeleteGroupAction_DeleteGroupAction());
        setIconKey(IconKey.ICON_DELETE);
        this.model = model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ArrayList<AccountInterface> l = new ArrayList<AccountInterface>();
        l.add(new GroupWrapper(new AccountGroup(null)));
        model.move(null, -1, l);
    }

}
