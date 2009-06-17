package jd;

import java.io.File;

import jd.nutils.svn.Subversion;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class Tester {

    private static Subversion svn;

    public static void main(String[] args) throws SVNException {

        File dstPath = new File("C:\\Users\\thomas\\Desktop\\containerex\\Neuer Ordner\\tests\\tests\\TestUtils.java");
        SVNURL url;

        svn = new Subversion("https://www.syncom.org/svn/jdownloader/trunk/src/", "user", "pass");
        // svn.showInfo(dstPath, null, true);

        svn.update(dstPath, SVNRevision.HEAD);
//        svn.lock(dstPath, "This file is locked for testing purposes");
        svn.unlock(dstPath);
//         svn.cleanUp(dstPath,true);
        // svn.revert(dstPath);
//        SVNCommitInfo re = svn.commit(dstPath, "just a test");
        // re=re;
    }
}
