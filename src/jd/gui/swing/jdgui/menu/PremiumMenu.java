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

import jd.config.Configuration;
import jd.gui.UserIO;
import jd.gui.swing.menu.HosterMenu;
import jd.nutils.JDFlags;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class PremiumMenu extends JStartMenu implements ActionListener {

    private static final long serialVersionUID = 5075413754334671773L;

    private JCheckBoxMenuItem premium;

    public PremiumMenu() {
        super("gui.menu.premium", "gui.images.taskpanes.premium");

        updateMenu();
    }

    private void updateMenu() {
        premium = new JCheckBoxMenuItem(JDL.L("gui.menu.action.premium.desc", "Enable Premiumusage globally"));
        premium.addActionListener(this);
        premium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
        this.add(premium);
        this.addSeparator();

        HosterMenu.update(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == premium) {
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

}
