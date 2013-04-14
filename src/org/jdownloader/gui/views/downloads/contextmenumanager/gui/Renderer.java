package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.Component;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

import org.appwork.utils.swing.renderer.RenderLabel;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainer;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuLink;
import org.jdownloader.gui.views.downloads.contextmenumanager.SeparatorData;
import org.jdownloader.images.NewTheme;

public class Renderer implements TreeCellRenderer {
    private JLabel    renderer;

    private ImageIcon right;

    private Font      bold;

    private Font      defFont;

    public Renderer() {
        renderer = new RenderLabel();
        defFont = renderer.getFont();
        bold = defFont.deriveFont(Font.BOLD);
        right = NewTheme.I().getIcon("right", 20);

    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        MenuItemData mid = ((MenuItemData) value).lazyReal();
        renderer.setFont(defFont);
        // renderer.setPreferredSize(new Dimension(tree.getWidth(), 24));
        // renderer.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
        if (mid instanceof MenuContainer) {
            if (mid.getIconKey() != null) {
                renderer.setIcon(NewTheme.I().getIcon(mid.getIconKey(), 20));
            } else {
                renderer.setIcon(null);

            }
            renderer.setFont(bold);
            renderer.setText(_GUI._.Renderer_getTreeCellRendererComponent_submenu(mid.getName()));
            return renderer;
        } else if (mid instanceof SeparatorData) {
            renderer.setIcon(null);
            renderer.setText(_GUI._.Renderer_getTreeCellRendererComponent_seperator());
            return renderer;
        } else {
            if (mid instanceof MenuLink) {

                renderer.setText(_GUI._.Renderer_getTreeCellRendererComponent_link(mid.getName()));
                if (mid.getIconKey() != null) {
                    renderer.setIcon(NewTheme.I().getIcon(mid.getIconKey(), 20));
                } else {
                    renderer.setIcon(null);
                }
                return renderer;
            } else {
                AppAction action = mid.createAction(null);
                renderer.setText(action.getName());
                renderer.setIcon(action.getSmallIcon());
                return renderer;
            }

        }

    }

}
