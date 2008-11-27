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
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class DDLWarez extends PluginForDecrypt {
    static class DDLWarez_Linkgrabber extends Thread {
        public final static int THREADFAIL = 1;
        public final static int THREADPASS = 0;
        int _status;
        private String downloadlink;
        private Form form;
        private boolean gotjob;
        protected ProgressController progress;
        private int Worker_ID;
        private Browser br;

        public DDLWarez_Linkgrabber(ProgressController progress, int id, Browser br) {
            downloadlink = null;
            gotjob = false;
            _status = THREADFAIL;
            Worker_ID = id;
            this.br = br;
            this.progress = progress;
        }

        public String getlink() {
            return downloadlink;
        }

        @Override
        public void run() {
            if (gotjob == true) {
                logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " started!");
                String base = br.getBaseURL();
                String action = form.getAction(base);

                if (action.contains("get_file")) {
                    Browser clone = br.cloneBrowser();
                    for (int retry = 1; retry <= 10; retry++) {
                        try {
                            clone.submitForm(form);

                            downloadlink = clone.getRegex(Pattern.compile("<frame\\s.*?src=\"(.*?)\r?\n?\" (?=(NAME=\"second\"))", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            break;
                        } catch (Exception e) {
                            logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " PostRequest-Error, try again!");
                            synchronized (DDLWarez.Worker_Delay) {
                                DDLWarez.Worker_Delay = 1000;
                            }
                        }
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                        }
                    }
                } else {
                    logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " finished! (NO DOWNLOAD FORM!)");
                    _status = THREADFAIL;
                    progress.increase(1);
                    return;
                }
            }
            logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " finished!");
            _status = THREADPASS;
            progress.increase(1);
        }

        public void setjob(Form form) {
            this.form = form;

            gotjob = true;
        }

        public int status() {
            return _status;
        }
    }

    public static Integer Worker_Delay = 250;

    public DDLWarez(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        for (int retry = 1; retry <= 10; retry++) {
            try {

                br.setReadTimeout(5 * 60 * 1000);

                br.setConnectTimeout(5 * 60 * 1000);
                br.getPage(parameter);

                String pass = br.getRegex(Pattern.compile("<td>Passwort:</td>.*?<td style=\"padding-left:10px;\">(.*?)</td>.*?</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                Vector<String> passwords = new Vector<String>();
                passwords.add("ddl-warez");
                if (pass != null && !pass.equals("kein Passwort")) {
                    passwords.add(pass);
                }

                Form[] forms = br.getForms();
                progress.setRange(forms.length);
                DDLWarez_Linkgrabber DDLWarez_Linkgrabbers[] = new DDLWarez_Linkgrabber[forms.length];
                for (int i = 0; i < forms.length; ++i) {
                    synchronized (Worker_Delay) {
                        Thread.sleep(Worker_Delay);
                    }
                    DDLWarez_Linkgrabbers[i] = new DDLWarez_Linkgrabber(progress, i, br.cloneBrowser());
                    DDLWarez_Linkgrabbers[i].setjob(forms[i]);
                    DDLWarez_Linkgrabbers[i].start();
                }
                for (int i = 0; i < forms.length; ++i) {
                    try {
                        DDLWarez_Linkgrabbers[i].join();
                        if (DDLWarez_Linkgrabbers[i].status() == DDLWarez_Linkgrabber.THREADPASS) {
                            DownloadLink link = createDownloadlink(DDLWarez_Linkgrabbers[i].getlink());
                            link.setSourcePluginPasswords(passwords);
                            decryptedLinks.add(link);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return decryptedLinks;
            } catch (Exception e) {
                logger.finest("DDLWarez: PostRequest-Error, try again!");
            }
        }
        return null;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
