//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Xlinkin extends PluginForDecrypt {

    static private String host = "xlink.in";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?xlink\\.in/\\?v=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    public static Integer Worker_Delay = 250;

    static class Xlinkin_Linkgrabber extends Thread {
        public final static int THREADFAIL = 1;
        public final static int THREADPASS = 0;
        int _status;
        private String downloadlink;
        private boolean gotjob;

        private int Worker_ID;
        private Browser br;
        private String page;
        private String link;

        public Xlinkin_Linkgrabber(int id, Browser br) {
            this.downloadlink = null;
            this.link = null;
            this.page = null;
            this.gotjob = false;
            this._status = THREADFAIL;
            this.Worker_ID = id;
            this.br = br;
            this.br.setFollowRedirects(true);
        }

        public String getlink() {
            return this.downloadlink;
        }

        @Override
        public void run() {
            if (this.gotjob == true) {
                logger.finest("Xlinkin_Linkgrabber: id=" + new Integer(this.Worker_ID) + " started!");

                for (int retry = 1; retry <= 10; retry++) {
                    try {
                        this.page = decodepage(this.br.getPage("http://xlink.in/?go=" + this.link));
                        this.downloadlink = new Regex(this.page, "<iframe src='(.*?)'", Pattern.CASE_INSENSITIVE).getMatch(0);
                        break;
                    } catch (Exception e) {
                        logger.finest("Xlinkin_Linkgrabber: id=" + new Integer(this.Worker_ID) + " GetRequest-Error, try again!");
                        synchronized (Xlinkin.Worker_Delay) {
                            Xlinkin.Worker_Delay = 1000;
                        }
                    }
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                    }
                }
            }
            logger.finest("Xlinkin_Linkgrabber: id=" + new Integer(this.Worker_ID) + " finished!");
            this._status = THREADPASS;
        }

        public void setjob(String link) {
            this.link = link;
            this.gotjob = true;
        }

        public int status() {
            return this._status;
        }
    }

    public Xlinkin() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        for (int retry = 1; retry <= 10; retry++) {
            try {
                String links[] = new Regex(br.getPage(parameter), "window.open\\('\\?go=(.*?)','.*?'\\)", Pattern.CASE_INSENSITIVE).getColumn(0);
                progress.setRange(links.length);
                Xlinkin_Linkgrabber Xlinkin_Linkgrabbers[] = new Xlinkin_Linkgrabber[links.length];
                for (int i = 0; i < links.length; ++i) {
                    synchronized (Worker_Delay) {
                        Thread.sleep(Worker_Delay);
                    }
                    Xlinkin_Linkgrabbers[i] = new Xlinkin_Linkgrabber(i, br.cloneBrowser());
                    Xlinkin_Linkgrabbers[i].setjob(links[i]);
                    Xlinkin_Linkgrabbers[i].start();
                }
                for (int i = 0; i < links.length; ++i) {
                    try {
                        Xlinkin_Linkgrabbers[i].join();
                        if (Xlinkin_Linkgrabbers[i].status() == Xlinkin_Linkgrabber.THREADPASS) {
                            decryptedLinks.add(createDownloadlink(Xlinkin_Linkgrabbers[i].getlink()));
                        }
                        progress.increase(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                progress.finalize();
                return decryptedLinks;
            } catch (Exception e) {
                logger.finest("Xlinkin: GetRequest-Error, try again!");
            }
        }
        return null;
    }

    private static String decodepage(String page) {
        if (page == null) return null;
        StringBuffer sb = new StringBuffer();
        String pattern = "(document\\.write\\(\".*?\"\\);)";
        Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(page);
        while (r.find()) {
            if (r.group(1).length() > 0) {
                String content = r.group(1).replaceAll("^document\\.write\\(\"", "").replaceAll("\"\\);$", "");
                r.appendReplacement(sb, content);
            }
        }
        r.appendTail(sb);
        return sb.toString();
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2393 $", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}