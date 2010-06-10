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

package jd.gui.swing.jdgui.components.premiumbar;

import java.awt.Cursor;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.swing.jdgui.components.JDProgressBar;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

public class TinyProgressBar extends JPanel {

    private static final long serialVersionUID = 8385631080915257786L;
    private final JLabel lbl;
    private final JDProgressBar prg;
    private PluginForHost plugin;

    public TinyProgressBar() {
        super(new MigLayout("ins 0", "[grow,fill]1[10!]", "[grow,fill]"));

        lbl = new JLabel();
        lbl.setOpaque(false);

        prg = new JDProgressBar();
        prg.setOpaque(false);
        prg.setOrientation(JDProgressBar.VERTICAL);
        prg.setBorder(null);

        this.add(lbl);
        this.add(prg);

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        lbl.setEnabled(b);
        prg.setEnabled(b);
    }

    public void setIcon(ImageIcon hosterIcon) {
        lbl.setIcon(hosterIcon);
    }

    public void setMaximum(long max) {
        prg.setMaximum(max);
    }

    public void setValue(long left) {
        prg.setValue(left);
    }

    public void setPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    public PluginForHost getPlugin() {
        return plugin;
    }

}
