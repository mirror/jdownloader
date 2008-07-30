//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de

//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.

//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.http.GetRequest;
import jd.http.PostRequest;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class DDLWarez extends PluginForDecrypt {
    private static final String host = "ddl-warez.org";
   
    private static final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?ddl-warez\\.org/detail\\.php\\?id=.+&cat=.+", Pattern.CASE_INSENSITIVE);
    public static Integer Worker_Delay = 250;

    public DDLWarez() {
        super();
        default_password.add("ddl-warez");
    }

    
    public String getCoder() {
        return "Jiaz";
    }

    
    public String getHost() {
        return host;
    }

    

    
    public String getPluginName() {
        return host;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        for (int retry = 1; retry <= 10; retry++) {
            try {
                GetRequest req = new GetRequest(parameter);
                req.setReadTimeout(5 * 60 * 1000);
                req.setConnectTimeout(5 * 60 * 1000);
                String page = req.load();
                String pass = new Regex(page, Pattern.compile("<td>Passwort:</td>.*?<td style=\"padding-left:10px;\">(.*?)</td>.*?</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getFirstMatch();                
                Vector<String> passwords = new Vector<String>();
                if (pass != null && !pass.equals("kein Passwort")) passwords.add(pass);

                Form[] forms = Form.getForms(req.getRequestInfo());
                progress.setRange(forms.length);
                DDLWarez_Linkgrabber DDLWarez_Linkgrabbers[] = new DDLWarez_Linkgrabber[forms.length];
                for (int i = 0; i < forms.length; ++i) {
                    synchronized (Worker_Delay) {
                        Thread.sleep(Worker_Delay);
                    }
                    DDLWarez_Linkgrabbers[i] = new DDLWarez_Linkgrabber(i);
                    DDLWarez_Linkgrabbers[i].setjob(forms[i], parameter);
                    DDLWarez_Linkgrabbers[i].start();
                }
                for (int i = 0; i < forms.length; ++i) {
                    try {
                        DDLWarez_Linkgrabbers[i].join();
                        if (DDLWarez_Linkgrabbers[i].status() == DDLWarez_Linkgrabber.THREADPASS) {
                            DownloadLink link = this.createDownloadlink(DDLWarez_Linkgrabbers[i].getlink());
                            link.setSourcePluginPasswords(passwords);
                            decryptedLinks.add(link);
                        }
                        progress.increase(1);
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

    
    public boolean doBotCheck(File file) {
        return false;
    }

    static class DDLWarez_Linkgrabber extends Thread {
        public final static int THREADPASS = 0;
        public final static int THREADFAIL = 1;
        int _status;
        private Form form;
        private String parameter;
        private String downloadlink;
        private boolean gotjob;
        private int Worker_ID;

        public int status() {
            return this._status;
        }

        public String getlink() {
            return this.downloadlink;
        }

        public DDLWarez_Linkgrabber(int id) {
            this.downloadlink = null;
            this.gotjob = false;
            this._status = THREADFAIL;
            this.Worker_ID = id;
        }

        public void setjob(Form form, String parameter) {
            this.form = form;
            this.parameter = parameter;
            this.gotjob = true;
        }

        public void run() {
            if (this.gotjob == true) {
                logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(this.Worker_ID) + " started!");
                String action = form.getAction();
                if (action.contains("get_file")) {
                    PostRequest r = new PostRequest(action);
                    r.setPostVariable("dont", form.vars.get("dont"));
                    r.setPostVariable("do", form.vars.get("do"));
                    r.setPostVariable("this", form.vars.get("this"));
                    r.setPostVariable("now", form.vars.get("now"));
                    r.getHeaders().put("Referer", parameter);
                    for (int retry = 1; retry <= 10; retry++) {
                        try {
                            r.load();
                            RequestInfo formInfo = r.getRequestInfo();
                            downloadlink = new Regex(formInfo.getHtmlCode(), Pattern.compile("<frame\\s.*?src=\"(.*?)\r?\n?\" (?=(NAME=\"second\"))", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                            break;
                        } catch (Exception e) {
                            logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(this.Worker_ID) + " PostRequest-Error, try again!");
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
                    logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(this.Worker_ID) + " finished! (NO DOWNLOAD FORM!)");
                    this._status = THREADFAIL;
                    return;
                }
            }
            logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(this.Worker_ID) + " finished!");
            this._status = THREADPASS;
        }
    }
}
