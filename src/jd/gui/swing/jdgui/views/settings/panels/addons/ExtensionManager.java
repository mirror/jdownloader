//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.settings.panels.addons;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.appwork.utils.swing.table.ExtTable;
import org.jdownloader.extensions.AbstractExtensionWrapper;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;
import org.jdownloader.translate.JDT;

/**
 * @author JD-Team
 */
public class ExtensionManager extends AbstractConfigPanel {

    public ImageIcon getIcon() {
        return Theme.getIcon("extension", 32);
    }

    private static final long                  serialVersionUID = 4145243293360008779L;

    private ExtTable<AbstractExtensionWrapper> table;

    private JTextArea                          txtDescription;

    public ExtensionManager() {
        super();

        init();
    }

    protected void init() {

        this.addHeader(getTitle(), getIcon());
        txtDescription = this.addDescription(JDT._.gui_settings_extensions_description());
        table = new ExtensionsTable();
        this.add(new JScrollPane(table));

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateShowcase();
            }
        });

        // JScrollPane scrollPane = new JScrollPane(txtDescription);
        //
        // // table.getSelectionModel().setSelectionInterval(0, 0);
        // descHeader = new Header("", null);
        // add(descHeader, "spanx,newline,growx,pushx");
        // txtDescription = this.addDescription("");
        // add(scrollPane);

    }

    public void updateShowcase() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        // table.getExtTableModel().fireTableRowsUpdated(row, row);
        AbstractExtensionWrapper opw = table.getExtTableModel().getElementAt(row);
        // descHeader.setIcon(opw._getIcon(32));
        // descHeader.setText(opw.getName());

        txtDescription.setText(opw.getDescription());
        // txtDescription.setCaretPosition(0);
    }

    @Override
    public String getTitle() {
        return T._.extensionManager_title();
    }

    @Override
    public void save() {
        table.getSelectionModel().setSelectionInterval(0, 0);
    }

    @Override
    public void updateContents() {
    }

}