package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class ExportPlainTextAction extends AppAction {

    private ProxyTable table;

    public ExportPlainTextAction(ProxyTable table) {
        setName(_GUI._.LinkgrabberFilter_LinkgrabberFilter_export());
        setIconKey("export");
        this.table = table;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ProxyTable.export(table.getModel().getElements());
    }

}
