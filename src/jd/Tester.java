package jd;

import java.io.IOException;

import jd.http.Browser;
import jd.http.JDProxy;

import org.tmatesoft.svn.core.SVNException;

public class Tester {

    public static void main(String[] args) throws SVNException, IOException {
        Browser.setVerbose(true);
        Browser.init();
        Browser br = new Browser();
        br.setProxy(new JDProxy("update2.jdownloader.org", 7761, "coalado", "viktor"));
        br.getPage("http://uploaded.to/?id=d6hrsi");
        br.openGetConnection(br.getRedirectLocation());
    }
}
