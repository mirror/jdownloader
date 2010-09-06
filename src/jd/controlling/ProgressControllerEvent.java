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

import org.appwork.utils.event.DefaultEvent;

public class ProgressControllerEvent extends DefaultEvent {

    public ProgressControllerEvent(Object source, int ID) {
        super(source, ID);
    }

    public static final int CANCEL   = 1;

    public static final int FINISHED = 2;

    /**
     * needed for current stable 0.9580
     */
    @Deprecated
    public int getID() {
        return getEventID();
    }
}
