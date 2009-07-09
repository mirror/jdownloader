package jd.nutils.svn;


public interface ResolveHandler {

    String resolveConflict(String contents, int startMine, int endMine, int startTheirs, int endTheirs);

}
