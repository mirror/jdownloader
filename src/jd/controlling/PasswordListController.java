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

package jd.controlling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Timer;

import jd.config.SubConfiguration;

import org.appwork.utils.AwReg;

public class PasswordListController implements ActionListener, ListController {
    private transient static SubConfiguration CONFIG = null;
    private transient ArrayList<String> LIST2;
    private transient static PasswordListController INSTANCE = new PasswordListController();

    private Timer asyncSaveIntervalTimer;

    private boolean saveinprogress;

    public static PasswordListController getInstance() {
        return INSTANCE;
    }

    private PasswordListController() {
        CONFIG = SubConfiguration.getConfig("PASSWORDLIST");
        LIST2 = CONFIG.getGenericProperty("LIST2", new ArrayList<String>());
        asyncSaveIntervalTimer = new Timer(2000, this);
        asyncSaveIntervalTimer.setInitialDelay(2000);
        asyncSaveIntervalTimer.setRepeats(false);
        asyncSaveIntervalTimer.stop();
        saveSync();
    }

    /**
     * <b>Warning: only use single passwords here, not multiple in one
     * string</b><br>
     * <br>
     * 
     * add pw to pwlist<br>
     * 
     * optional: move pw to top (eg by jdunrar)
     */
    public void addPassword(String pw, boolean top) {
        if (pw == null || pw.trim().length() == 0) return;
        synchronized (LIST2) {
            if (LIST2.contains(pw)) {
                if (top) {
                    LIST2.remove(pw);
                    LIST2.add(0, pw);
                }
            } else {
                if (top) {
                    LIST2.add(0, pw);
                } else {
                    LIST2.add(pw);
                }
            }
        }
        save();
    }

    public ArrayList<String> getPasswordList() {
        return LIST2;
    }

    public void setPasswordList(ArrayList<String> list) {
        if (list == null) list = new ArrayList<String>();
        synchronized (LIST2) {
            LIST2.clear();
            addPasswords(list);
            removeDups();
        }
    }

    public void setList(String list) {
        if (list == null) list = "";
        String[] spl = AwReg.getLines(list);
        synchronized (LIST2) {
            LIST2.clear();
            addPasswords(spl);
            removeDups();
        }
    }

    public String getList() {
        synchronized (LIST2) {
            StringBuilder sb = new StringBuilder();
            for (String pw : getPasswordList()) {
                sb.append(pw).append(new char[] { '\r', '\n' });
            }
            return sb.toString().trim();
        }
    }

    private void addPasswords(Collection<String> list) {
        if (list == null || list.size() == 0) return;
        synchronized (LIST2) {
            for (String pw : list) {
                addPassword(pw, false);
            }
        }
    }

    public void addPasswords(String list[]) {
        if (list == null || list.length == 0) return;
        synchronized (LIST2) {
            for (String pw : list) {
                addPassword(pw, false);
            }
        }
    }

    private void removeDups() {
        synchronized (LIST2) {
            Set<String> list = new LinkedHashSet<String>();
            for (String pw : LIST2) {
                list.add(pw);
            }
            LIST2.clear();
            LIST2.addAll(list);
            this.save();
        }
    }

    public void save() {
        if (saveinprogress) return;
        asyncSaveIntervalTimer.restart();
    }

    public void saveAsync() {
        if (saveinprogress) return;
        new Thread() {
            public void run() {
                this.setName("PasswordList: Saving");
                saveSync();
            }
        }.start();
    }

    public void saveSync() {
        asyncSaveIntervalTimer.stop();
        String id = JDController.requestDelayExit("passwordcontroller");
        synchronized (LIST2) {
            saveinprogress = true;
            CONFIG.setProperty("LIST2", LIST2);
            CONFIG.save();
            saveinprogress = false;
        }
        JDController.releaseDelayExit(id);
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == asyncSaveIntervalTimer) {
            saveAsync();
        }
    }

}
