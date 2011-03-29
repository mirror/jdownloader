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
import java.util.ArrayList;
import java.util.Date;

public class Actions implements Serializable {
    private static final long serialVersionUID = 5218836057229345533L;
    private String name;
    private boolean enabled = true;
    private Date date;
    private int repeat = 0;
    private boolean alreadyhandled = false;
    private ArrayList<Executions> executions = new ArrayList<Executions>();

    public Actions(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean wasAlreadyHandled() {
        return alreadyhandled;
    }

    public void setAlreadyHandled(boolean b) {
        alreadyhandled = b;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void addExecutions(Executions e) {
        executions.add(e);
    }

    public void removeExecution(int row) {
        executions.remove(row);
    }

    public ArrayList<Executions> getExecutions() {
        return executions;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public int getRepeat() {
        return repeat;
    }
}
