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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.Timer;

import jd.config.SubConfiguration;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

public class PasswordListController implements ActionListener, DownloadControllerListener {
    private transient static SubConfiguration CONFIG = null;
    private transient ArrayList<String> LIST2;
    private transient static PasswordListController INSTANCE = null;

    public transient static String PASSWORDCONTROLLER = "PASSWORDCONTROLLER";

    private Timer asyncSaveIntervalTimer;

    private boolean saveinprogress;

    public static synchronized PasswordListController getInstance() {
        if (INSTANCE == null) INSTANCE = new PasswordListController();
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private PasswordListController() {
        CONFIG = SubConfiguration.getConfig("PASSWORDLIST");
        LIST2 = (ArrayList<String>) CONFIG.getProperty("LIST2", new ArrayList<String>());
        asyncSaveIntervalTimer = new Timer(2000, this);
        asyncSaveIntervalTimer.setInitialDelay(2000);
        asyncSaveIntervalTimer.setRepeats(false);
        asyncSaveIntervalTimer.stop();
        importOld1();
        importOld2();
        saveSync();
    }

    public void addPassword(String pw) {
        if (pw == null || pw.trim().length() == 0) return;
        synchronized (LIST2) {
            if (LIST2.contains(pw)) LIST2.remove(pw);
            LIST2.add(0, pw);
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

    public void setPasswordList(String list) {
        if (list == null) list = "";
        String[] spl = Regex.getLines(list);
        synchronized (LIST2) {
            LIST2.clear();
            addPasswords(spl);
            removeDups();
        }
    }

    public String getPasswordListasString() {
        synchronized (LIST2) {
            StringBuilder sb = new StringBuilder();
            for (String pw : getPasswordList()) {
                sb.append(pw + "\r\n");
            }
            return sb.toString().trim();
        }
    }

    @SuppressWarnings("unchecked")
    private void importOld1() {
        try {
            LinkedList<String> oldList = (LinkedList<String>) SubConfiguration.getConfig("unrarPasswords").getProperty("PASSWORDLIST");
            if (oldList != null) {
                SubConfiguration.getConfig("unrarPasswords").setProperty("PASSWORDLIST", null);
                addPasswords(oldList);
                SubConfiguration.getConfig("unrarPasswords").save();
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void importOld2() {
        try {
            String[] spl = Regex.getLines(CONFIG.getStringProperty("LIST", ""));
            for (String pw : spl) {
                addPasswords(Regex.getLines(pw));
            }
            CONFIG.setProperty("LIST", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addPasswords(LinkedList<String> list) {
        if (list == null || list.size() == 0) return;
        synchronized (LIST2) {
            for (String pw : list) {
                addPassword(pw);
            }
        }
    }

    private void addPasswords(ArrayList<String> list) {
        if (list == null || list.size() == 0) return;
        synchronized (LIST2) {
            for (String pw : list) {
                addPassword(pw);
            }
        }
    }

    private void addPasswords(String list[]) {
        if (list == null || list.length == 0) return;
        synchronized (LIST2) {
            for (String pw : list) {
                addPassword(pw);
            }
        }
    }

    private void removeDups() {
        synchronized (LIST2) {
            Set<String> list = new HashSet<String>();
            for (String pw : LIST2) {
                list.add(pw);
            }
            LIST2.clear();
            LIST2.addAll(list);
            this.save();
        }
    }

    public void save() {
        asyncSaveIntervalTimer.restart();
    }

    public void saveAsync() {
        if (saveinprogress) return;
        new Thread() {
            public void run() {
                this.setName("PasswordList: Saving");
                saveinprogress = true;
                synchronized (LIST2) {
                    CONFIG.setProperty("LIST2", LIST2);
                    CONFIG.save();
                }
                saveinprogress = false;
            }
        }.start();
    }

    public void saveSync() {
        synchronized (LIST2) {
            CONFIG.setProperty("LIST2", LIST2);
            CONFIG.save();
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == asyncSaveIntervalTimer) {
            saveAsync();
        }
    }

    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getID()) {
        case DownloadControllerEvent.ADD_DOWNLOADLINK:
            this.addPasswords(((DownloadLink) event.getParameter()).getSourcePluginPasswordList());
            break;
        case DownloadControllerEvent.ADD_FILEPACKAGE:
            this.addPasswords(JDUtilities.passwordStringToArray((((FilePackage) event.getParameter()).getPassword())));
            break;
        default:
            break;
        }

    }
}
