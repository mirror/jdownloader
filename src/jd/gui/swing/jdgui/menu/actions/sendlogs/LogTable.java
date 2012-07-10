package jd.gui.swing.jdgui.menu.actions.sendlogs;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class LogTable extends BasicJDTable<LogFolder> {

    public LogTable(LogModel model) {
        super(model);
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, LogFolder contextObject, final ArrayList<LogFolder> selection, ExtColumn<LogFolder> column, MouseEvent mouseEvent) {
        popup.add(new AppAction() {
            {
                setName(_GUI._.LogTable_onContextMenu_enable_());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                for (LogFolder f : selection) {
                    f.setSelected(true);
                }
            }
        });
        popup.add(new AppAction() {
            {
                setName(_GUI._.LogTable_onContextMenu_disable_());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                for (LogFolder f : selection) {
                    f.setSelected(false);
                }
            }
        });
        return popup;
    }

}
