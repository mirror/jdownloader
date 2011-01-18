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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.Timer;

import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.utils.Regex;

public class HTACCESSController implements ActionListener, ListController {
    private transient static SubConfiguration CONFIG = null;
    private transient HashMap<String, String[]> LIST;
    private transient static HTACCESSController INSTANCE = null;

    private final Timer asyncSaveIntervalTimer;

    private boolean saveinprogress;

    public static synchronized HTACCESSController getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HTACCESSController();
        }
        return INSTANCE;
    }

    private HTACCESSController() {
        CONFIG = SubConfiguration.getConfig("HTACCESSLIST");
        final HashMap<String, String[]> defaultentry = new HashMap<String, String[]>();
        defaultentry.put("example.com", new String[] { "username", "passwd" });
        LIST = CONFIG.getGenericProperty("LIST2", defaultentry);
        asyncSaveIntervalTimer = new Timer(2000, this);
        asyncSaveIntervalTimer.setInitialDelay(2000);
        asyncSaveIntervalTimer.setRepeats(false);
        asyncSaveIntervalTimer.stop();
        saveSync();
    }

    public void add(final String url, String username, String passwd) {
        if (url != null && url.length() > 0) {
            final String host = Browser.getHost(url.trim()).toLowerCase();
            if (username == null) username = "";
            if (passwd == null) passwd = "";
            synchronized (LIST) {
                LIST.remove(host);
                LIST.put(host, new String[] { username.trim(), passwd.trim() });
            }
        }
    }

    public static String[] getUserDatafromBasicauth(String basicauth) {
        if (basicauth == null || basicauth.length() == 0) return null;
        if (basicauth.startsWith("Basic")) basicauth = new Regex(basicauth, "Basic (.*?)$").getMatch(0);
        basicauth = Encoding.Base64Decode(basicauth);
        final String[] dat = new Regex(basicauth, ("(.*?):(.*?)$")).getRow(0);
        return new String[] { dat[0], dat[1] };
    }

    public void add(final String url, final String basicauth) {
        if (url != null && url.length() > 0) {
            final String host = Browser.getHost(url.trim()).toLowerCase();
            final String[] user = getUserDatafromBasicauth(basicauth);
            if (user == null) return;
            synchronized (LIST) {
                LIST.remove(host);
                LIST.put(host, user);
            }
        }
    }

    public String get(final String url) {
        if (url != null && url.length() > 0) {
            final String host = Browser.getHost(url.trim()).toLowerCase();
            synchronized (LIST) {
                if (!LIST.containsKey(host)) return null;
                return "Basic " + Encoding.Base64Encode(LIST.get(host)[0] + ":" + LIST.get(host)[1]);
            }
        }
        return null;
    }

    public void remove(final String url) {
        if (url != null && url.length() > 0) {
            final String host = Browser.getHost(url.trim()).toLowerCase();
            synchronized (LIST) {
                LIST.remove(host);
            }
        }
    }

    public HashMap<String, String[]> getPasswordList() {
        return LIST;
    }

    public void setList(final String list) {
        final String[] pws = Regex.getLines(list);
        synchronized (LIST) {
            LIST.clear();
            for (final String pw : pws) {
                final String[] dat = new Regex(pw, "(.*?)%%%%(.*?)%%%%(.*?)$").getRow(0);
                if (dat != null) {
                    add(dat[0], dat[1], dat[2]);
                }
            }
        }
    }

    public String getList() {
        synchronized (LIST) {
            final StringBuilder sb = new StringBuilder();
            for (final String host : LIST.keySet()) {
                final String auth[] = LIST.get(host);
                sb.append(host).append(" %%%% ").append(auth[0]).append(" %%%% ").append(auth[1]);
                sb.append(new char[] { '\r', '\n' });
            }
            return sb.toString().trim();
        }
    }

    public void save() {
        asyncSaveIntervalTimer.restart();
    }

    public void saveAsync() {
        if (saveinprogress) return;
        new Thread() {
            @Override
            public void run() {
                this.setName("PasswordList: Saving");
                saveinprogress = true;
                saveSync();
                saveinprogress = false;
            }
        }.start();
    }

    public void saveSync() {
        final String id = JDController.requestDelayExit("htaccesscontroller");
        synchronized (LIST) {
            CONFIG.setProperty("LIST", LIST);
            CONFIG.save();
        }
        JDController.releaseDelayExit(id);
    }

    public void actionPerformed(final ActionEvent event) {
        if (event.getSource() == asyncSaveIntervalTimer) {
            saveAsync();
        }
    }
}
