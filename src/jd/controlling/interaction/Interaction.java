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

import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;

/**
 * Mit dieser Klasse werden Interaktionen (mit dem System) umgesetzt
 * 
 * @author astaldo
 */
public abstract class Interaction extends Property implements Serializable {

    private transient static final long serialVersionUID = -5609631258725998799L;

    private static final String CONFIG_INTERACTIONS = "EVENTS";

    private static final String PARAM_INTERACTIONS = "INTERACTIONS";

    public Interaction() {
    }

    protected abstract boolean doInteraction(Object arg);

    public abstract String getInteractionName();

    public abstract void initConfig();

    @Override
    public abstract String toString();

    public static void deleteInteractions() {
        final SubConfiguration config = SubConfiguration.getConfig(CONFIG_INTERACTIONS);
        if (config.hasProperty(PARAM_INTERACTIONS)) {
            config.setProperty(PARAM_INTERACTIONS, Property.NULL);
            config.save();
            JDLogger.getLogger().finer("deleted old saved interactions");
        }
    }

}
