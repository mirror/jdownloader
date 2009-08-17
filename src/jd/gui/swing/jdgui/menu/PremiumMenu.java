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

package jd.gui.swing.jdgui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import jd.config.Configuration;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.gui.swing.menu.HosterMenu;
import jd.nutils.JDFlags;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class PremiumMenu extends JStartMenu implements ActionListener {

    private static final long serialVersionUID = 5075413754334671773L;

    private static PremiumMenu INSTANCE;

    private JCheckBoxMenuItem premium;

    private JMenuItem config;

    // private ConfigPanelView panel;

    private PremiumMenu() {
        super("gui.menu.premium", "gui.images.taskpanes.premium");

        updateMenu();
    }

    public static PremiumMenu getInstance() {
        if (INSTANCE == null) INSTANCE = new PremiumMenu();
        return INSTANCE;
    }

    private void updateMenu() {
        premium = new JCheckBoxMenuItem(JDL.L("gui.menu.action.premium.desc", "Enable Premiumusage globally"));
        premium.addActionListener(this);
        premium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
        this.add(premium);
        this.addSeparator();

        HosterMenu.update(this);
        this.addSeparator();

        config = new JMenuItem(JDL.L("gui.menu.action.config.desc", "Premium Settings"));
        config.addActionListener(this);
        // config.setSelected(false);
        this.add(config);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == config) {
            // if (config.isSelected()) {
            // if (panel == null) {
            // panel = new ConfigPanelView(new Premium(null),
            // JDL.L("gui.menu.action.config.desc", "Premium Settings"),
            // JDTheme.II("gui.images.configuration", 16, 16));
            // panel.getBroadcaster().addListener(new SwitchPanelListener() {
            //
            // @Override
            // public void onPanelEvent(SwitchPanelEvent event) {
            // switch (event.getID()) {
            // case SwitchPanelEvent.ON_REMOVE:
            // config.setSelected(false);
            // break;
            // }
            // }
            //
            // });
            // }
            // SwingGui.getInstance().setContent(panel);
            // } else {
            // panel.close();
            // }
            SwingGui.getInstance().requestPanel(UserIF.Panels.PREMIUMCONFIG, null);
        } else if (e.getSource() == premium) {
            if (!premium.isSelected()) {
                int answer = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN, JDL.L("dialogs.premiumstatus.global.title", "Disable Premium?"), JDL.L("dialogs.premiumstatus.global.message", "Do you really want to disable all premium accounts?"), JDTheme.II("gui.images.warning", 32, 32), JDL.L("gui.btn_yes", "Yes"), JDL.L("gui.btn_no", "No"));
                if (JDFlags.hasAllFlags(answer, UserIO.RETURN_CANCEL) && !JDFlags.hasAllFlags(answer, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                    premium.setSelected(true);
                    return;
                }
            }

            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.isSelected());
            JDUtilities.getConfiguration().save();
        }
    }

    public void update() {
        this.removeAll();
        updateMenu();
    }

}
