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

package org.jdownloader.extensions.schedule.modules;


 import org.jdownloader.extensions.schedule.translate.*;
import org.jdownloader.extensions.schedule.SchedulerModule;
import org.jdownloader.extensions.schedule.SchedulerModuleInterface;

import jd.config.Configuration;
import jd.controlling.JSonWrapper;
import jd.utils.locale.JDL;

@SchedulerModule
public class SetChunk implements SchedulerModuleInterface {

    private static final long serialVersionUID = -986046937528397324L;

    public boolean checkParameter(String parameter) {
        try {
            int i = Integer.parseInt(parameter);
            if (i > 20 || i < 1) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void execute(String parameter) {
        JSonWrapper.get("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, Integer.parseInt(parameter));
        JSonWrapper.get("DOWNLOAD").save();
    }

    public String getTranslation() {
        return T._.jd_plugins_optional_schedule_modules_setChunks();
    }

    public boolean needParameter() {
        return true;
    }

}