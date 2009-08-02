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

package jd.gui.swing.jdgui.settings.panels.premium;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import jd.config.Configuration;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.utils.locale.JDL;

public class Premium extends ConfigPanel {

    private static final long serialVersionUID = -39217675978744715L;

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.premium.Premium.";

    public Premium(Configuration configuration) {
        super();

        initPanel();
        load();
    }

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "title", "Premium");
    }

    @Override
    public void initPanel() {
        initPanel(panel);
        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);
        this.add(tabbed);
    }

    private void initPanel(JPanel panel) {
        // Hier kann jiaz sien Zeug reinkleben;-P

    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

    }
}
