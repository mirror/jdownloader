package jd.nutils.svn;

import org.tmatesoft.svn.core.wc.SVNInfo;

public interface ResolveHandler {

    String resolveConflict(String contents, int startMine, int endMine, int startTheirs, int endTheirs);

}
