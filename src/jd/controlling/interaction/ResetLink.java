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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ResetLink extends Interaction implements Serializable {

    private static final long serialVersionUID = -9071890385750062424L;

    private static final String NAME = JDLocale.L("interaction.resetLink.name", "Downloadlink zurücksetzen");

    private static final String PARAM_LAST_OR_ALL = "LAST_OR_ALL";

    public ResetLink() {
    }

    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting Reset Link");
        int type = this.getIntegerProperty(PARAM_LAST_OR_ALL, 1);
        JDController controller = JDUtilities.getController();
        if (type == 0) {
            controller.resetAllLinks();
        } else if (type == 1) {
            DownloadLink link = controller.getLastFinishedDownloadLink();
            if (link != null) {
                link.getLinkStatus().setStatus(LinkStatus.TODO);
                link.getLinkStatus().setStatusText("");
                link.reset();
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, link));
            } else {
                logger.severe("Kein letzter Downloadlink gefunden");
            }

        }

        return true;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @Override
    public void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, this, PARAM_LAST_OR_ALL, new String[] { JDLocale.L("interaction.resetLink.options.all", "all Links"), JDLocale.L("interaction.resetLink.options.lastLink", "only last Link") }, JDLocale.L("interaction.resetLink.whichLink", "Welcher Link soll zurückgesetzt werden?")).setDefaultValue(1));
    }

    @Override
    public void resetInteraction() {
    }

    @Override
    public String toString() {
        return NAME;
    }
}
