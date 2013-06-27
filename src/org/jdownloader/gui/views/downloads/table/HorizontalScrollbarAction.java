package org.jdownloader.gui.views.downloads.table;

import java.awt.event.ActionEvent;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.swing.exttable.columnmenu.ResetColumns;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class HorizontalScrollbarAction extends AppAction implements GenericConfigEventListener<Boolean> {

    private ExtTable          table;
    private BooleanKeyHandler keyHandler;

    public HorizontalScrollbarAction(ExtTable downloadsTable, BooleanKeyHandler horizontalScrollbarsInDownloadTableEnabled) {
        table = downloadsTable;
        keyHandler = horizontalScrollbarsInDownloadTableEnabled;
        setName(_GUI._.HorizontalScrollbarAction_columnControlMenu_scrollbar_());
        setSelected(keyHandler.isEnabled());
        keyHandler.getEventSender().addListener(this, true);
        setup(keyHandler, table);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        keyHandler.toggle();
        setup(keyHandler, table);
        new ResetColumns(table).actionPerformed(e);

    }

    public static void setup(final BooleanKeyHandler keyHandler, final ExtTable table) {

        JScrollPane sp = (JScrollPane) table.getParent().getParent();

        if (keyHandler.isEnabled()) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        } else {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        }

    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        setSelected(this.keyHandler.isEnabled());
    }

}
