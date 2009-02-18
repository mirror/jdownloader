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

package jd.controlling.interaction;

import java.io.Serializable;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Interaktion beendet den JDownloader.
 * 
 * @author JD-Team
 */
public class JDExit extends Interaction implements Serializable {

    private static final long serialVersionUID = -4825002404662625527L;

    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting Exit");
        CountdownConfirmDialog shutDownMessage = new CountdownConfirmDialog(((SimpleGUI) JDUtilities.getGUI()).getFrame(), "JD will close itself in 10 secs!", 10, true, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);
        if (shutDownMessage.result) {
            JDUtilities.getController().exit();
        }
        return true;
    }

    @Override
    public String getInteractionName() {
        return JDLocale.L("interaction.jdexit.name", "JD Beenden");
    }

    @Override
    public void initConfig() {
    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.jdexit.name", "JD Beenden");
    }
}
