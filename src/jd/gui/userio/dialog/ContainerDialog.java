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

package jd.gui.userio.dialog;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class ContainerDialog extends AbstractDialog {

    private static final long serialVersionUID = -348017625663435924L;
    private JPanel panel;

    public ContainerDialog(int flags, String title, JPanel panel, String ok, String cancel) {
        super(flags, title, null, ok, cancel);
        this.panel = panel;
        init();
    }

    @Override
    public JComponent contentInit() {
        return panel;
    }

}
