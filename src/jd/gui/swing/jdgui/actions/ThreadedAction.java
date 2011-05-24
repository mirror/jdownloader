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

package jd.gui.swing.jdgui.actions;

import java.awt.event.ActionEvent;

import jd.controlling.JDLogger;

public abstract class ThreadedAction extends ToolBarAction {

    private boolean actionrunning          = false;
    private boolean multiplethreadsallowed = false;

    public ThreadedAction(String name, String menukey, String iconkey) {
        super(name, menukey, iconkey);
    }

    public ThreadedAction(String name, String menukey, String iconkey, boolean allowmultiplethreads) {
        super(name, menukey, iconkey);
        multiplethreadsallowed = allowmultiplethreads;
    }

    private static final long serialVersionUID = -1483816271981451352L;

    /**
     * this action is performed in its own thread, explicit edt queueing is
     * needed
     */
    public void onAction(final ActionEvent e) {
        if (actionrunning && !multiplethreadsallowed) return;
        actionrunning = true;
        new Thread() {
            public void run() {
                this.setName(getID());
                try {
                    threadedActionPerformed(e);
                } catch (Exception e) {
                    JDLogger.exception(e);
                }
                actionrunning = false;
            }
        }.start();
    }

    public abstract void threadedActionPerformed(ActionEvent e);

}
