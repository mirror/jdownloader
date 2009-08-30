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

package tests.singletests;

import jd.gui.UserIO;
import jd.nutils.JDFlags;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class IO {

    @Before
    public void setUp() {
        TestUtils.initJD();
    }

    @Test
    public void dontshowagain() {
//        int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_OK, "My title 2", "Message", null, null, null);
//
//        out(ret);
        
//        int status = UserIO.getInstance().requestHelpDialog(UserIO.NO_CANCEL_OPTION|UserIO.NO_COUNTDOWN, JDL.LF("system.update.message.title", "Updated to version %s", JDUtilities.getRevision()), JDL.L("system.update.message", "Update successfull"), JDL.L("system.update.showchangelogv2", "What's new?"), "http://jdownloader.org/changes/index");
//      out(status);        
//       String ret= UserIO.getInstance().requestInputDialog("Please enter anything");
       String ret2= UserIO.getInstance().requestInputDialog(0, "message", "defaultmessage");
       System.out.println(ret2);

    }

    private void out(int ret) {
        System.out.println("Cancel: " + JDFlags.hasAllFlags(ret, UserIO.RETURN_CANCEL));
        System.out.println("OK: " + JDFlags.hasAllFlags(ret, UserIO.RETURN_OK));
        System.out.println("Timeout: " + JDFlags.hasAllFlags(ret, UserIO.RETURN_COUNTDOWN_TIMEOUT));
        System.out.println("Dontshowagain: " + JDFlags.hasAllFlags(ret, UserIO.RETURN_DONT_SHOW_AGAIN));
        System.out.println("Skippedby dontshow: " + JDFlags.hasAllFlags(ret, UserIO.RETURN_SKIPPED_BY_DONT_SHOW));
    }

    @After
    public void tearDown() throws Exception {
        JDUtilities.getController().exit();
    }
}