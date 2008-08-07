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

package jd.config;

import java.io.Serializable;
import java.util.HashMap;

import jd.utils.JDUtilities;

public class SubConfiguration extends Property implements Serializable {

    private static final long serialVersionUID = 7803718581558607222L;
    private String name;

    @SuppressWarnings("unchecked")
    public SubConfiguration(String name) {
        this.name = name;
        Object props = JDUtilities.getDatabaseConnector().getData(name);
        if (props != null) {
            this.setProperties((HashMap<String, Object>) props);
        }
    }

    public void save() {
        JDUtilities.getDatabaseConnector().saveConfiguration(name, this.getProperties());
    }
}