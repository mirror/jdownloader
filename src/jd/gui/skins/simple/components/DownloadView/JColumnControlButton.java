//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.skins.simple.components.DownloadView;

import java.util.List;

import javax.swing.Action;
import javax.swing.table.TableColumn;

import jd.config.SubConfiguration;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.ColumnControlButton;

public class JColumnControlButton extends ColumnControlButton {

    private static final long serialVersionUID = 3234874733654121548L;
    private SubConfiguration config;

    public JColumnControlButton(JXTable table) {
        super(table);
    }

    protected List<Action> getAdditionalActions() {
        List<Action> actions = super.getAdditionalActions();
        actions.remove(0);
        return actions;
    }

    public class JColumnVisibilityAction extends ColumnVisibilityAction {

        private static final long serialVersionUID = -3849908321036020341L;

        private int id;

        public JColumnVisibilityAction(TableColumn column) {
            super(column);
        }

        public void setActionCommand(Object key) {
            this.id = DownloadTreeTableModel.getIDFormHeaderLabel(key.toString());
            putValue(Action.ACTION_COMMAND_KEY, key);
        }

        public synchronized void setSelected(boolean newValue) {
            super.setSelected(newValue);
            if (config.getBooleanProperty("VISABLE_COL_" + id, true) != newValue) {
                config.setProperty("VISABLE_COL_" + id, newValue);
                config.save();
            }
        }

    }
}
