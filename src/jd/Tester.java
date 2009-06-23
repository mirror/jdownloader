package jd;

import java.io.IOException;

import jd.http.Browser;
import jd.http.JDProxy;
import jd.nutils.svn.Subversion;

import org.tmatesoft.svn.core.SVNException;

public class Tester {

    private static Subversion svn;

    public static void main(String[] args) throws SVNException, IOException {

        Browser.init();
        Browser.setVerbose(true);
        Browser br = new Browser();
        br.setProxy(new JDProxy("85.133.65.113:80"));
        br.getPage("http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=checkfiles_v1&files=");

    }
}
