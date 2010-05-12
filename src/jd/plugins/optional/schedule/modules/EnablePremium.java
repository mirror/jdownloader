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

package jd.plugins.optional.schedule.modules;

import jd.config.Configuration;
import jd.plugins.optional.schedule.SchedulerModule;
import jd.plugins.optional.schedule.SchedulerModuleInterface;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@SchedulerModule
public class EnablePremium implements SchedulerModuleInterface {

    private static final long serialVersionUID = 8621543260803605634L;

    public boolean checkParameter(String parameter) {
        return false;
    }

    public void execute(String parameter) {
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
        JDUtilities.getConfiguration().save();
    }

    public String getTranslation() {
        return JDL.L("jd.plugins.optional.schedule.modules.enablePremium", "Enable Premium");
    }

    public boolean needParameter() {
        return false;
    }

}
