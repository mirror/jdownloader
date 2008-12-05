package jd.nutils.svn;

import java.io.File;

import jd.utils.JDUtilities;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class Subversion {
    
    public static void main(String[] args) throws Exception {

        Subversion svn = new Subversion("https://www.syncom.org/svn/jdownloader/trunk/src/");

        System.out.println("Exported to revision " + svn.export(new File("c:/testexport")));
    }

    
    private SVNRepository repository;
    private SVNURL svnurl;
    private String user;
    private String pass;

    public Subversion(String url) throws SVNException {
        setupType(url);
        checkRoot();
    }

    private void checkRoot() throws SVNException {
        SVNNodeKind nodeKind = repository.checkPath("", -1);
        if (nodeKind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "No entry at URL ''{0}''", svnurl);
            throw new SVNException(err);
        } else if (nodeKind == SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Entry at URL ''{0}'' is a file while directory was expected", svnurl);
            throw new SVNException(err);
        }

    }

    private void setupType(String url) throws SVNException {
        this.svnurl = SVNURL.parseURIDecoded(url);

        if (url.startsWith("http")) {
            DAVRepositoryFactory.setup();
            repository = DAVRepositoryFactory.create(svnurl);
        } else if (url.startsWith("svn")) {
            SVNRepositoryFactoryImpl.setup();
            repository = SVNRepositoryFactoryImpl.create(svnurl);
        } else {
            FSRepositoryFactory.setup();
            repository = FSRepositoryFactory.create(svnurl);
        }

    }

    public Subversion(String url, String user, String pass) throws SVNException {
        setupType(url);
        this.user = user;
        this.pass = pass;
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(user, pass);
        repository.setAuthenticationManager(authManager);
        checkRoot();

    }


    private long export(File file) throws SVNException {
        JDUtilities.removeDirectoryOrFile(file);
        file.mkdirs();

        ISVNEditor exportEditor = new ExportEditor(file);
        long rev;
        ISVNReporterBaton reporterBaton = new ExportReporterBaton(rev = latestRevision());
        /*
         * Now ask SVNKit to perform generic 'update' operation using our
         * reporter and editor.
         * 
         * We are passing:
         * 
         * - revision from which we would like to export - null as "target"
         * name, to perform export from the URL SVNRepository was created for,
         * not from some child directory. - reporterBaton - exportEditor.
         */
        repository.update(rev, null, true, reporterBaton, exportEditor);

        return rev;
    }

    private long latestRevision() throws SVNException {

        return repository.getLatestRevision();
    }
}
