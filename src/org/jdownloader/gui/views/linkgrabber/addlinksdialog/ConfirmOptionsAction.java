package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


import org.jdownloader.gui.laf.jddefault.LAFOptions;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmOptionsAction extends AbstractAction {

    private JButton        defaultOK;
    private AddLinksDialog dialog;

    {
        putValue(SMALL_ICON, NewTheme.I().getIcon("popupButton", -1));

    }

    public ConfirmOptionsAction(JButton okButton, AddLinksDialog addLinksDialog) {
        defaultOK = okButton;
        dialog = addLinksDialog;

    }

    public void actionPerformed(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();

        popup.add(new JMenuItem(new AbstractAction() {
            {
                putValue(NAME, _GUI._.ConfirmOptionsAction_actionPerformed_deep());
            }

            public void actionPerformed(ActionEvent e) {
                dialog.setDeepAnalyse(true);
                ActionEvent e2 = new ActionEvent(defaultOK, e.getID(), e.getActionCommand());
                for (ActionListener a : defaultOK.getActionListeners())
                    a.actionPerformed(e2);
            }

        }));

        popup.add(new JMenuItem(new AbstractAction() {
            {
                putValue(NAME, _GUI._.ConfirmOptionsAction_actionPerformed_normale());
            }

            public void actionPerformed(ActionEvent e) {
                dialog.setDeepAnalyse(false);
                ActionEvent e2 = new ActionEvent(defaultOK, e.getID(), e.getActionCommand());
                for (ActionListener a : defaultOK.getActionListeners())
                    a.actionPerformed(e2);
            }
        }));
        // selected.add(new JMenuItem(new ConfirmAction(false,
        // table.getModel().getSelectedObjects())));
        // selected.add(new JMenuItem(new ConfirmAction(true,
        // table.getModel().getSelectedObjects())));
        int[] insets = LAFOptions.getInstance().getPopupBorderInsets();
        JComponent comp = (JComponent) e.getSource();

        popup.show(comp, -popup.getPreferredSize().width + comp.getWidth() + insets[3], -popup.getPreferredSize().height + insets[2]);
    }
}