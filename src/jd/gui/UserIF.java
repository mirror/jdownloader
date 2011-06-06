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

package jd.gui;

import jd.config.ConfigContainer;
import jd.gui.swing.jdgui.views.downloads.DownloadView;
import jd.gui.swing.jdgui.views.linkgrabber.LinkgrabberView;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.userio.NoUserIF;

public abstract class UserIF {
    protected static UserIF INSTANCE = null;

    public static UserIF getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoUserIF();
        }
        return INSTANCE;
    }

    public static void setInstance(final UserIF instance) {
        INSTANCE = instance;
    }

    /**
     * An Abstract panelrepresentation used in requestPanel(Panels.*,Parameter
     * 
     * @author Coalado
     */
    public static enum Panels {
        /**
         * Represents the {@link DownloadView}.
         */
        DOWNLOADLIST,

        /**
         * Represents the {@link LinkgrabberView}.
         */
        LINKGRABBER,

        /**
         * Represents a configview. Parameter is a {@link ConfigContainer} or
         * the {@link Class} reference to an {@link ConfigPanel}.
         */
        CONFIGPANEL,

        /**
         * Displays the {@link Premium}-ConfigPanel. The parameter is the
         * account which should be selected.
         */
        PREMIUMCONFIG

    }

    /**
     * requests a special view from the gui. example:
     * 
     * JDUtilities.getGUI().requestPanel(Panels.*,parameter);
     * 
     * asks the gui backend to display the downloadlist. IT depends on the
     * guibackend which requests are fullfilled and which not.
     * 
     * @param panelID
     *            {@link Panels}
     * @param parameter
     *            TODO
     * @see Panels
     */
    public abstract void requestPanel(Panels panelID, Object parameter);

    public abstract void displayMiniWarning(String shortWarn, String longWarn);

    /**
     * Minimiert die GUI. als ID können die WINDOW_STATUS_IDS aus
     * {@link UIConstants} verwendet werden
     * 
     * @see UIConstants
     */
    public abstract void setFrameStatus(int id);

}
