package jd.nutils.svn;


import java.io.File;

import org.tmatesoft.svn.core.wc.SVNInfo;


public interface ResolveHandler {

    String resolveConflict(SVNInfo info, File file, String contents, int startMine, int endMine, int startTheirs, int endTheirs);

}
