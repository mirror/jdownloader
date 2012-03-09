//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org  http://jdownloader.org
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
//

package jd;

public class Main {
    public static void main(String[] args) {
        try {
            jd.Launcher.main(args);
        } catch (Throwable e) {
            e.printStackTrace();
            try {
                org.appwork.utils.logging.Log.exception(e);
            } catch (Throwable e2) {
            }
            try {
                org.appwork.utils.swing.dialog.Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);
            } catch (Throwable e2) {
            }
            try {
                org.appwork.update.inapp.RestartController.getInstance().restartViaUpdater(false);
            } catch (Throwable e2) {
            }
        }
    }

}