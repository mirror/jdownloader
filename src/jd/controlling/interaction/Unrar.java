//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.controlling.interaction;

import java.io.Serializable;

public class Unrar extends Interaction implements Serializable {

    private static final long serialVersionUID = 2467582501274722811L;

    public static Unrar getInstance() {
        return null;
    }

    private Unrar() {
        super();
    }

    @Override
    public boolean doInteraction(Object arg) {
        return true;
    }

    @Override
    public String getInteractionName() {
        return null;
    }

    @Override
    public boolean getWaitForTermination() {
        return false;
    }

    @Override
    public void initConfig() {
    }

    @Override
    public void resetInteraction() {
    }

    @Override
    public String toString() {
        return null;
    }
}