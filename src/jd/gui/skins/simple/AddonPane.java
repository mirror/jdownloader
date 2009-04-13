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

package jd.gui.skins.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import net.miginfocom.swing.MigLayout;

public class AddonPane extends JTabbedPanel implements ActionListener {

    private static final long serialVersionUID = 1511081032101600835L;

    public AddonPane(Logger logger) {
        this.setLayout(new MigLayout("ins 3", "[fill,grow]", "[fill,grow]"));
    }

    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public void onDisplay() {
    }

    @Override
    public void onHide() {
    }

}
