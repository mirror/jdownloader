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

package jd.dynamics;

import jd.config.Configuration;
import jd.controlling.DynamicPluginInterface;
import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

public class UpdateFix extends DynamicPluginInterface {

    @Override
    public void execute() {
        long revision = Long.parseLong(JDUtilities.getRevision().replaceAll(",|\\.", ""));
        if (revision < 6035) {
            JDLogger.getLogger().info("UpdateFix: workaround enabled!");
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false);
        }
    }
}
