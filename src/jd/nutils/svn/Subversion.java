//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nutils.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import jd.utils.JDUtilities;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
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

    private SVNRepository repository;
    private SVNURL svnurl;
    private String user;
    private String pass;

    public Subversion(String url) throws SVNException {
        setupType(url);
        checkRoot();
    }

    public Subversion(String url, String user, String pass) throws SVNException {
        setupType(url);
        this.user = user;
        this.pass = pass;
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(this.user, this.pass);
        repository.setAuthenticationManager(authManager);
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

    public long export(File file) throws SVNException {
        JDUtilities.removeDirectoryOrFile(file);
        file.mkdirs();

        ISVNEditor exportEditor = new ExportEditor(file);
        long rev = latestRevision();
        ISVNReporterBaton reporterBaton = new ExportReporterBaton(rev);
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

    @SuppressWarnings("unchecked")
    public  ArrayList<SVNLogEntry> getChangeset(int start, int end) throws SVNException {
       Collection log = repository.log(new String[] { "" }, null, start, end, true, true);

        ArrayList<SVNLogEntry> list = new ArrayList<SVNLogEntry>();
        list.addAll(log);
        return list;
        // for (Iterator<SVNLogEntry> entries = logEntries.iterator();
        // entries.hasNext();) {
        // SVNLogEntry logEntry = (SVNLogEntry) entries.next();
        // System.out.println("---------------------------------------------");
        // System.out.println("revision: " + logEntry.getRevision());
        // System.out.println("author: " + logEntry.getAuthor());
        // System.out.println("date: " + logEntry.getDate());
        // System.out.println("log message: " + logEntry.getMessage());
        //
        // if (logEntry.getChangedPaths().size() > 0) {
        // System.out.println();
        // System.out.println("changed paths:");
        // Set changedPathsSet = logEntry.getChangedPaths().keySet();
        //
        // for (Iterator changedPaths = changedPathsSet.iterator();
        // changedPaths.hasNext();) {
        // SVNLogEntryPath entryPath = (SVNLogEntryPath)
        // logEntry.getChangedPaths().get(changedPaths.next());
        // System.out.println(" " + entryPath.getType() + " " +
        // entryPath.getPath() + ((entryPath.getCopyPath() != null) ? " (from "
        // + entryPath.getCopyPath() + " revision " +
        // entryPath.getCopyRevision() + ")" : ""));
        // }
        // }
        // }

    }
}
