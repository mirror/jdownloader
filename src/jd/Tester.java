package jd;

import java.io.File;

import javax.swing.JFrame;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.svn.Subversion;
import jd.plugins.optional.langfileeditor.LFEGui;

import org.tmatesoft.svn.core.wc.SVNRevision;

public class Tester extends JFrame {



    /**
     * pings the updateservers to observe them
     * 
     * @param ss
     * @throws Exception
     */
    public static void main(String ss[]) throws Exception {

        String[] list = new String[] { "http://update4ex.jdownloader.org/branches/Synthy/",

        "http://jdupdate.bluehost.to/branches/Synthy/", "http://update2.jdownloader.org/Synthy/", "http://update1.jdownloader.org/Synthy/", "http://update0.jdownloader.org/Synthy/"

        };

        while (true) {
            long time = 0;
            long s, e;
            Browser br = new Browser();
            File file=File.createTempFile("jd"+System.currentTimeMillis(), "dummy");
            for (String serv : list) {
           
                s = System.currentTimeMillis();
                try {
                    URLConnectionAdapter con = br.openGetConnection(serv + "JDownloader.jar");
                   System.out.println("Requesttime= "+(System.currentTimeMillis()-s)+" ms");
                    br.downloadConnection(file, con);
                    e = System.currentTimeMillis();
                    con.disconnect();
                } catch (Exception ee) {
                    e = 100000;
                    ee.printStackTrace();
                }
                System.out.println(serv + ": " + (e - s));
                time += (e - s);

            }

            time /= (list.length);

            System.err.println("Average speed: " + file.length()/time+" kb/s");
            Thread.sleep(10000);
        }

        // SERVERLIST.append("-1:http://update4ex.jdownloader.org/branches/%BRANCH%/\r\n");
        // SERVERLIST.append("-1:http://jdupdate.bluehost.to/branches/%BRANCH%/\r\n");
        // SERVERLIST.append("-1:http://update1.jdownloader.org/%BRANCH%/\r\n");
        // SERVERLIST.append("-1:http://update2.jdownloader.org/%BRANCH%/\r\n");
        // SERVERLIST.append("-1:http://jd.code4everyone.de/%BRANCH%/\r\n");

        // SERVERLIST.append("-1:http://update0.jdownloader.org/%BRANCH%/\r\n");
    }

}
