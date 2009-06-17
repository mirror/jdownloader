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

import jd.controlling.JDLogger;
import jd.nutils.io.JDIO;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
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
import org.tmatesoft.svn.core.wc.ISVNCommitParameters;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class Subversion {

    private SVNRepository repository;
    private SVNURL svnurl;
    private String user;
    private String pass;
    private ISVNAuthenticationManager authManager;
    private SVNClientManager clientManager;
    private SVNUpdateClient updateClient;
    private SVNCommitClient commitClient;
    private SVNWCClient wcClient;

    public Subversion(String url) throws SVNException {
        setupType(url);
        checkRoot();
    }

    public Subversion(String url, String user, String pass) throws SVNException {
        setupType(url);
        this.user = user;
        this.pass = pass;
        authManager = SVNWCUtil.createDefaultAuthenticationManager(this.user, this.pass);
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
        JDIO.removeDirectoryOrFile(file);
        file.mkdirs();

        ISVNEditor exportEditor = new ExportEditor(file);
        long rev = latestRevision();
        ISVNReporterBaton reporterBaton = new ExportReporterBaton(rev);

        repository.update(rev, null, true, reporterBaton, exportEditor);

        return rev;
    }

    public long latestRevision() throws SVNException {
        return repository.getLatestRevision();
    }

    /**
     * Returns all changesets between revision start and end
     * 
     * @param start
     * @param end
     * @return
     * @throws SVNException
     */
    @SuppressWarnings("unchecked")
    public ArrayList<SVNLogEntry> getChangeset(int start, int end) throws SVNException {
        Collection log = repository.log(new String[] { "" }, null, start, end, true, true);

        ArrayList<SVNLogEntry> list = new ArrayList<SVNLogEntry>();
        list.addAll(log);
        return list;
    }

    /**
     * Updates the repo to file. if there is no repo at file, a checkout is
     * performed
     * 
     * @param file
     * @param revision
     * @throws SVNException
     * @return revision
     */
    public long update(File file, SVNRevision revision) throws SVNException {
        // JDIO.removeDirectoryOrFile(file);
        file.mkdirs();

        SVNUpdateClient updateClient = this.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        if (revision == null) revision = SVNRevision.HEAD;

        try {
            System.out.println("SVN Update at " + file);
            return updateClient.doUpdate(file, revision, SVNDepth.INFINITY, false, true);
        } catch (Exception e) {
            JDLogger.getLogger().finer(e.getMessage());
            try {
                System.out.println("SVN Checkout at " + file);
                return updateClient.doCheckout(svnurl, file, SVNRevision.HEAD, revision, SVNDepth.INFINITY, true);
            } catch (Exception e2) {
                e2.printStackTrace();
                return -1;
            } finally {
                System.out.println("SVN Update finished");
            }
        } finally {
            System.out.println("SVN Update finished");
        }

    }

    /**
     * Return repo for external actions
     * 
     * @return
     */
    public SVNRepository getRepository() {
        return this.repository;
    }

    private SVNUpdateClient getUpdateClient() {
        if (updateClient == null) {
            updateClient = getClientManager().getUpdateClient();
            updateClient.setEventHandler(new UpdateEventHandler());
        }

        return updateClient;
    }

    private synchronized SVNClientManager getClientManager() {
        if (clientManager == null) {
            clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), authManager);
        }
        return clientManager;
    }

    /**
     * Commits the wholepath and KEEPS locks
     * 
     * @param dstPath
     * @param message
     * @return
     * @throws SVNException
     */
    public SVNCommitInfo commit(File dstPath, String message) throws SVNException {
        getWCClient().doAdd(dstPath, true, false, true, SVNDepth.INFINITY, false, false);

        SVNCommitPacket packet = getCommitClient().doCollectCommitItems(new File[] { dstPath }, false, false, SVNDepth.INFINITY, null);

        getCommitClient().doCommit(packet, true, false, message, null);

        return null;
    }

    private SVNWCClient getWCClient() {
        if (wcClient == null) {
            wcClient = getClientManager().getWCClient();
            wcClient.setEventHandler(new WCEventHandler());
        }

        return wcClient;
    }

    @SuppressWarnings("deprecation")
    public void showInfo(File wcPath, SVNRevision revision, boolean isRecursive) throws SVNException {
        if (revision == null) revision = SVNRevision.HEAD;
        getWCClient().doInfo(wcPath, revision, isRecursive, new InfoEventHandler());
    }

    @SuppressWarnings("deprecation")
    public void showStatus(File wcPath, boolean isRecursive, boolean isRemote, boolean isReportAll, boolean isIncludeIgnored, boolean isCollectParentExternals) throws SVNException {
        getClientManager().getStatusClient().doStatus(wcPath, isRecursive, isRemote, isReportAll, isIncludeIgnored, isCollectParentExternals, new StatusEventHandler(isRemote));
    }

    private SVNCommitClient getCommitClient() {

        if (commitClient == null) {
            commitClient = getClientManager().getCommitClient();
            commitClient.setEventHandler(new CommitEventHandler());
            commitClient.setCommitParameters(new ISVNCommitParameters() {

                public boolean onDirectoryDeletion(File directory) {
                    return false;
                }

                public boolean onFileDeletion(File file) {
                    return false;
                }

                public Action onMissingDirectory(File file) {
                    return ISVNCommitParameters.DELETE;
                }

                public Action onMissingFile(File file) {
                    return ISVNCommitParameters.DELETE;
                }
            });
        }
        return commitClient;

    }

    /**
     * Cleans up the file or doirectory
     * 
     * @param dstPath
     * @param deleteWCProperties
     * @throws SVNException
     */
    public void cleanUp(File dstPath, boolean deleteWCProperties) throws SVNException {
        getWCClient().doCleanup(dstPath, deleteWCProperties);

    }

    /**
     * Reverts the file or directory
     * 
     * @param dstPath
     * @throws SVNException
     */
    public void revert(File dstPath) throws SVNException {
        getWCClient().doRevert(new File[] { dstPath }, SVNDepth.INFINITY, null);

    }

    /**
     * Locks a file or directory as long as it it not locked by someone else
     * 
     * @param dstPath
     * @param message
     * @throws SVNException
     */
    public void lock(File dstPath, String message) throws SVNException {
        getWCClient().doLock(new File[] { dstPath }, false, message);

    }

    /**
     * Unlocks this file only if it is locked by you
     * 
     * @param dstPath
     * @param message
     * @throws SVNException
     */
    public void unlock(File dstPath) throws SVNException {
        getWCClient().doUnlock(new File[] { dstPath }, false);

    }
}
