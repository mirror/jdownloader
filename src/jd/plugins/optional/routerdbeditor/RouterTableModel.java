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

package jd.plugins.optional.routerdbeditor;

import jd.gui.swing.components.table.JDTableModel;
import jd.utils.locale.JDL;

public class RouterTableModel extends JDTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 1411827173660950838L;
    private static final String JDL_PREFIX = "jd.plugins.optional.JDRouterEditor.";
    private RouterList router;

    public RouterTableModel(String configname, RouterList router) {
        super(configname);
        this.router = router;
    }

    @Override
    protected void initColumns() {
        this.addColumn(new NameColumn(JDL.L(JDL_PREFIX + "router", "Router Name"), this));

    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            synchronized (RouterList.LOCK) {
                list.clear();
                if (router != null) list.addAll(router.getRouter());
            }
        }

    }

}
