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

package jd.controlling;

import java.util.Iterator;
import java.util.Map.Entry;

import jd.config.Property;

public class DataBox extends Property {
    /**
     * 
     */
    private static final long serialVersionUID = 5147254150196577471L;

    public Entry<String, Object> getEntry(int id) {
        for (final Iterator<Entry<String, Object>> it = this.getProperties().entrySet().iterator(); it.hasNext();) {
            if (id < 0) return null;

            if (id == 0) {
                return it.next();
            } else {
                it.next();
            }
            id--;
        }
        return null;
    }
}
