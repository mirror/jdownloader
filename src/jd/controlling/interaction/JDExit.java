//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import jd.utils.JDLocale;

/**
 * Diese Klasse führt eine Test Interaction durch
 * 
 * @author JD-Team
 */
public class JDExit extends Interaction implements Serializable {

    /**
     * serialVersionUID
     */
    private static final String NAME = JDLocale.L("interaction.jdexit.name", "JD Beenden");
    /**
     * Führt die Normale Interaction zurück. Nach dem Aufruf dieser methode
     * läuft der Download wie geowhnt weiter.
     */
    public static String PROPERTY_QUESTION = "INTERACTION_" + NAME + "_QUESTION";
    /**
     * 
     */
    private static final long serialVersionUID = -4825002404662625527L;

    public JDExit() {
    }

    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting Exit");
        System.exit(0);
        return true;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @Override
    public void initConfig() {
    }

    @Override
    public void resetInteraction() {
    }

    /**
     * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
     */
    @Override
    public void run() {
    }

    @Override
    public String toString() {
        return NAME;
    }
}
