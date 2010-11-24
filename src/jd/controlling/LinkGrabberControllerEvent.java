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

import org.appwork.utils.event.DefaultIntEvent;

public class LinkGrabberControllerEvent extends DefaultIntEvent {
    public LinkGrabberControllerEvent(Object source, int ID) {
        super(source, ID);
    }

    public LinkGrabberControllerEvent(Object source, int ID, Object param) {
        super(source, ID, param);
    }

    public static final int NEW_LINKS          = 0;

    public static final int REFRESH_STRUCTURE  = 1;

    public static final int ADD_FILEPACKAGE    = 2;

    public static final int REMOVE_FILEPACKAGE = 3;

    public static final int ADDED              = 5;

    public static final int EMPTY              = 4;

    public static final int FILTER_CHANGED     = 9;

    public static final int FINISHED           = 10;
}
