package org.jdownloader.gui.toolbar;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class ShowInputDialogDebugAction extends AppAction implements CachableInterface {
    {
        setIconKey("menu");
        setTooltipText(getClass().getName());
        setName(getTooltipText());

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // DownloadsTableModel.getInstance().getTable().scrollToRow(10, -1);
        DownloadsTableModel.getInstance().getTable().scrollToSelection(-1);
    }

    @Override
    public void setData(String data) {
    }

}
