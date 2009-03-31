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

package jd.event;

import java.awt.AWTEvent;

import jd.gui.UIInterface;

public class UIEvent extends AWTEvent {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -5178146758475854235L;


    private int actionID;

    private Object parameter;


    public UIEvent(Object source,int actionID, Object parameter) {
        super(source,actionID);
      
        this.actionID = actionID;
        this.parameter = parameter;
    }

    public UIEvent(UIInterface uiInterface, int actionID) {
        this(uiInterface, actionID, null);
    }

    public int getActionID() {
        return actionID;
    }

    public Object getParameter() {
        return parameter;
    }

 

}
