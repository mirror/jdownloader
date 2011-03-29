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

package org.jdownloader.extensions.schedule;

import java.io.Serializable;

import jd.controlling.JDLogger;

public class Executions implements Serializable {
    private static final long serialVersionUID = 8873752967587288865L;
    private SchedulerModuleInterface module;
    private String parameter;

    public Executions(SchedulerModuleInterface module, String parameter) {
        this.module = module;
        this.parameter = parameter;
    }

    public SchedulerModuleInterface getModule() {
        return module;
    }

    public String getParameter() {
        return parameter;
    }

    public void exceute() {
        try {
            module.execute(parameter);
        } catch (Exception e) {
            JDLogger.exception(e);
            /* catch exceptions here, so we do not kill our scheduler */
        }
    }
}
