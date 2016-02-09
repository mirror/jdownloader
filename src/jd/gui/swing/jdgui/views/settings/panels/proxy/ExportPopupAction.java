package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ExportPopupAction extends AppAction {
    /**
     *
     */
    private static final long serialVersionUID = -1041794723138925672L;
    private JButton           positionComp;
    private ProxyTable        table;

    public ExportPopupAction(JButton addLinks, ProxyTable table) {
        setSmallIcon(new AbstractIcon(IconKey.ICON_POPUPSMALL, -1));
        setTooltipText(_GUI.T.AddOptionsAction_AddOptionsAction_tt());
        positionComp = addLinks;
        this.table = table;
    }

    public void actionPerformed(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();

        // ala.putValue(AbstractAction.NAME, _GUI.T.AddOptionsAction_actionPerformed_addlinks());
        popup.add(new JMenuItem(new ExportPlainTextAction(table)));
        popup.add(new JMenuItem(new SaveAsProxyProfileAction(table)));
        // popup.add(new JMenuItem(new AddContainerAction()));
        Insets insets = LAFOptions.getInstance().getExtension().customizePopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        pref.width = Math.max(pref.width, positionComp.getWidth() + ((Component) e.getSource()).getWidth() + insets.left + insets.right);
        popup.setPreferredSize(pref);

        popup.show(positionComp, -insets.left - 1, -popup.getPreferredSize().height + insets.bottom);
    }

}
