package jd.nutils.svn;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;

public class ExportReporterBaton implements ISVNReporterBaton {
    
    private long exportRevision;
    
    public ExportReporterBaton(long revision){
        exportRevision = revision;
    }
    
    public void report(ISVNReporter reporter) throws SVNException {
        try {
            /*
             * Here empty working copy is reported.
             * 
             * ISVNReporter includes methods that allows to report mixed-rev working copy
             * and even let server know that some files or directories are locally missing or
             * locked. 
             */
            reporter.setPath("", null, exportRevision, SVNDepth.INFINITY, true);
            
            /*
             * Don't forget to finish the report!
             */
            reporter.finishReport(); 
        } catch (SVNException svne) {
            reporter.abortReport();
            System.out.println("Report failed.");
        }
    }
}
