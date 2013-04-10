package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.uio.UIOManager;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class AdvancedTable extends BasicJDTable<AdvancedConfigEntry> {
    private static final long serialVersionUID = 1L;

    public AdvancedTable() {
        super(new AdvancedTableModel("AdvancedTable"));
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, AdvancedConfigEntry contextObject, final List<AdvancedConfigEntry> selection, ExtColumn<AdvancedConfigEntry> column, MouseEvent mouseEvent) {
        JPopupMenu p = new JPopupMenu();
        p.add(new AppAction() {
            {
                setSmallIcon(NewTheme.I().getIcon("reset", 20));
                setName(_GUI._.AdvancedTable_onContextMenu_reset_selection());
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                if (UIOManager.I().showConfirmDialog(0, _GUI._.lit_are_you_sure(), _GUI._.AdvancedTablecontextmenu_reset(selection.size()))) {
                    for (AdvancedConfigEntry ce : selection) {
                        ce.setValue(ce.getDefault());
                    }
                    repaint();
                }
            }
        });

        return p;
    }

    @Override
    public boolean isSearchEnabled() {
        return true;
    }

    public void filter(String text) {
        ((AdvancedTableModel) this.getExtTableModel()).refresh(text);
    }

}
