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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import jd.utils.locale.JDL;

public class SimpleExecute extends Interaction implements Serializable, ActionListener {

    private static final long serialVersionUID = 1L;

    public void actionPerformed(ActionEvent e) {

    }

    // @Override
    public boolean doInteraction(Object arg) {
        return false;
    }

    // @Override
    public String getInteractionName() {
        return JDL.L("interaction.simpleExecute.name", "Programm/Script ausführen");
    }

    // @Override
    public void initConfig() {
    }

    // @Override
    public String toString() {
        return JDL.L("interaction.simpleExecute.name", "Programm/Script ausführen");
    }

}
