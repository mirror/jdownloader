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

package jd.plugins;

import org.appwork.utils.event.DefaultEvent;

public class LinkGrabberFilePackageEvent extends DefaultEvent {

    public LinkGrabberFilePackageEvent(final Object source, final int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    public LinkGrabberFilePackageEvent(final Object source, final int ID, final Object param) {
        super(source, ID, param);
        // TODO Auto-generated constructor stub
    }

    /* ein wichtiger Wert wurde geändert */
    public static final int UPDATE_EVENT = 1;

    public static final int ADD_LINK = 2;
    public static final int REMOVE_LINK = 3;

    /* das FilePackage ist leer */
    public static final int EMPTY_EVENT = 999;

}
