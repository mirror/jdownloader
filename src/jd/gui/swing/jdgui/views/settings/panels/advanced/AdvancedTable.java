package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.uio.UIOManager;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvandedValueEditor;

public class AdvancedTable extends BasicJDTable<AdvancedConfigEntry> {
    private static final long serialVersionUID = 1L;

    public AdvancedTable(AdvancedConfigTableModel model) {
        super(model);
    }

    public AdvancedTable() {
        super(new AdvancedConfigTableModel("AdvancedTable"));
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, AdvancedConfigEntry contextObject, final List<AdvancedConfigEntry> selection, ExtColumn<AdvancedConfigEntry> column, MouseEvent mouseEvent) {
        final JPopupMenu p = new JPopupMenu();
        p.add(new AppAction() {
            {
                setSmallIcon(new AbstractIcon(IconKey.ICON_RESET, 20));
                setName(_GUI.T.AdvancedTable_onContextMenu_reset_selection());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (UIOManager.I().showConfirmDialog(0, _GUI.T.lit_are_you_sure(), _GUI.T.AdvancedTablecontextmenu_reset(selection.size()))) {
                    for (AdvancedConfigEntry ce : selection) {
                        ce.setValue(ce.getDefault());
                    }
                    repaint();
                }
            }
        });
        if (selection.size() == 1) {
            final AdvancedConfigEntry item = selection.get(0);
            final Class<? extends AdvandedValueEditor> advancedValueEditor = item.getAdvancedValueEditor();
            if (advancedValueEditor != null) {
                p.add(new AppAction() {
                    {
                        setSmallIcon(new AbstractIcon(IconKey.ICON_EDIT, 20));
                        setName(_GUI.T.AdvancedTable_onContextMenu_advanced_value_edit());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            final Object newValue = advancedValueEditor.newInstance().edit(item.getValue());
                            item.setValue(newValue);
                            repaint();
                        } catch (Throwable throwable) {
                            LogController.CL().log(throwable);
                        }
                    }
                });
            }
        }
        return p;
    }

    @Override
    public boolean isSearchEnabled() {
        return true;
    }

    public void filter(String text) {
        ((AdvancedConfigTableModel) this.getModel()).refresh(text);
    }
}
