package jd.controlling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.Timer;

import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;

public class HTACCESSController implements ActionListener, ListController {
    private transient static SubConfiguration CONFIG = null;
    private transient HashMap<String, String[]> LIST;
    private transient static HTACCESSController INSTANCE = null;

    private Timer asyncSaveIntervalTimer;

    private boolean saveinprogress;

    public static synchronized HTACCESSController getInstance() {
        if (INSTANCE == null) INSTANCE = new HTACCESSController();
        return INSTANCE;
    }

    private HTACCESSController() {
        CONFIG = SubConfiguration.getConfig("HTACCESSLIST");
        HashMap<String, String[]> defaultentry = new HashMap<String, String[]>();
        defaultentry.put("example.com", new String[] { "username", "passwd" });
        LIST = CONFIG.getGenericProperty("LIST2", defaultentry);
        asyncSaveIntervalTimer = new Timer(2000, this);
        asyncSaveIntervalTimer.setInitialDelay(2000);
        asyncSaveIntervalTimer.setRepeats(false);
        asyncSaveIntervalTimer.stop();
        saveSync();
    }

    public void add(String url, String username, String passwd) {
        if (url == null || url.length() == 0) return;
        String host = Browser.getHost(url.trim()).toLowerCase();
        if (username == null) username = "";
        if (passwd == null) passwd = "";
        synchronized (LIST) {
            LIST.remove(host);
            LIST.put(host, new String[] { username.trim(), passwd.trim() });
        }
    }

    public static String[] getUserDatafromBasicauth(String basicauth) {
        if (basicauth == null || basicauth.length() == 0) return null;
        if (basicauth.startsWith("Basic")) basicauth = new Regex(basicauth, "Basic (.*?)$").getMatch(0);
        basicauth = Encoding.Base64Decode(basicauth);
        String[] dat = new Regex(basicauth, ("(.*?):(.*?)$")).getRow(0);
        return new String[] { dat[0], dat[1] };
    }

    public void add(String url, String basicauth) {
        if (url == null || url.length() == 0) return;
        String host = Browser.getHost(url.trim()).toLowerCase();
        String[] user = getUserDatafromBasicauth(basicauth);
        if (user == null) return;
        synchronized (LIST) {
            LIST.remove(host);
            LIST.put(host, user);
        }
    }

    public String get(String url) {
        if (url == null || url.length() == 0) return null;
        String host = Browser.getHost(url.trim()).toLowerCase();
        synchronized (LIST) {
            if (!LIST.containsKey(host)) return null;
            return "Basic " + Encoding.Base64Encode(LIST.get(host)[0] + ":" + LIST.get(host)[1]);
        }
    }

    public void remove(String url) {
        if (url == null || url.length() == 0) return;
        String host = Browser.getHost(url.trim()).toLowerCase();
        synchronized (LIST) {
            LIST.remove(host);
        }
    }

    public HashMap<String, String[]> getPasswordList() {
        return LIST;
    }

    public void setList(String list) {
        String[] pws = Regex.getLines(list);
        synchronized (LIST) {
            LIST.clear();
            for (String pw : pws) {
                String[] dat = new Regex(pw, "(.*?)%%%%(.*?)%%%%(.*?)$").getRow(0);
                if (dat == null) continue;
                add(dat[0], dat[1], dat[2]);
            }
        }
    }

    public String getList() {
        synchronized (LIST) {
            StringBuilder sb = new StringBuilder();
            for (String host : LIST.keySet()) {
                String auth[] = LIST.get(host);
                sb.append(host + " %%%% " + auth[0] + " %%%% " + auth[1]);
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
            public void run() {
                this.setName("PasswordList: Saving");
                saveinprogress = true;
                synchronized (LIST) {
                    CONFIG.setProperty("LIST", LIST);
                    CONFIG.save();
                }
                saveinprogress = false;
            }
        }.start();
    }

    public void saveSync() {
        synchronized (LIST) {
            CONFIG.setProperty("LIST", LIST);
            CONFIG.save();
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == asyncSaveIntervalTimer) {
            saveAsync();
        }
    }
}
