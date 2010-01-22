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

package jd.event;

public abstract class JDEvent {

    private final int ID;
    private final Object source;
    private Object parameter;

    public JDEvent(final Object source, final int ID) {
        this.source = source;
        this.ID = ID;
    }

    public JDEvent(final Object source, final int ID, final Object parameter) {
        this(source, ID);
        this.parameter = parameter;
    }

    public int getID() {
        return ID;
    }

    public Object getParameter() {
        return parameter;
    }

    public Object getSource() {
        return source;
    }

    public String toString() {
        return "[source:" + source + ", controlID:" + ID + ", parameter:" + parameter + "]";
    }

}
