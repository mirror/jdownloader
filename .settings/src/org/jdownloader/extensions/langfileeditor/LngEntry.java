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

package org.jdownloader.extensions.langfileeditor;

import java.io.Serializable;

public class LngEntry implements Serializable {

    private static final long serialVersionUID = -4522709262578144738L;

    private String            value;

    private String            key;

    public LngEntry(String key, String value) {
        this.key = key.toLowerCase();
        this.value = value.replace("\\\"", "\"");
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object e) {
        if (!(e instanceof LngEntry)) return false;
        return ((LngEntry) e).getKey().equalsIgnoreCase(getKey());
    }

    @Override
    public String toString() {
        return key + " = " + value;
    }

}
