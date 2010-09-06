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
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.nutils.io.JDIO;

import org.appwork.utils.event.Eventsender;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNCommitParameters;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class Subversion implements ISVNEventHandler {

    private SVNRepository repository;
    private SVNURL svnurl;
    private ISVNAuthenticationManager authManager;
    private SVNClientManager clientManager;
    private SVNUpdateClient updateClient;
    private SVNCommitClient commitClient;
    private SVNWCClient wcClient;

    private final Eventsender<MessageListener, MessageEvent> broadcaster = new Eventsender<MessageListener, MessageEvent>() {

        @Override
        protected void fireEvent(MessageListener listener, MessageEvent event) {
            listener.onMessage(event);
        }

    };

    public Subversion(String url) throws SVNException {
        setupType(url);
        checkRoot();
    }

    public Subversion(String url, String user, String pass) throws SVNException {
        setupType(url);
        authManager = SVNWCUtil.createDefaultAuthenticationManager(user, pass);
        ((DefaultSVNAuthenticationManager) authManager).setAuthenticationForced(true);
        repository.setAuthenticationManager(authManager);
        checkRoot();
    }

    public Eventsender<MessageListener, MessageEvent> getBroadcaster() {
        return broadcaster;
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
        Collection<SVNLogEntry> log = repository.log(new String[] { "" }, null, start, end, true, true);

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
            updateClient.setEventHandler(this);
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

    public SVNWCClient getWCClient() {
        if (wcClient == null) {
            wcClient = getClientManager().getWCClient();
            wcClient.setEventHandler(this);
        }

        return wcClient;
    }

    public void showInfo(File wcPath, SVNRevision revision, boolean isRecursive) throws SVNException {
        if (revision == null) revision = SVNRevision.HEAD;

        getWCClient().doInfo(wcPath, SVNRevision.UNDEFINED, revision, SVNDepth.getInfinityOrEmptyDepth(isRecursive), null, new InfoEventHandler());
    }

    public void resolveConflicts(File file, final ResolveHandler handler) throws SVNException {

        getWCClient().doInfo(file, SVNRevision.UNDEFINED, SVNRevision.WORKING, SVNDepth.getInfinityOrEmptyDepth(true), null, new ISVNInfoHandler() {

            public void handleInfo(SVNInfo info) {
                File file = info.getConflictWrkFile();
                if (file != null) {
                    try {
                        resolveConflictedFile(info, info.getFile(), handler);
                        getWCClient().doResolve(info.getFile(), SVNDepth.INFINITY, null);
                        System.out.println(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Returns an ArrayLIst with Info for all files found in file.
     * 
     * @param file
     * @return
     */
    public ArrayList<SVNInfo> getInfo(File file) {
        final ArrayList<SVNInfo> ret = new ArrayList<SVNInfo>();
        try {
            getWCClient().doInfo(file, SVNRevision.UNDEFINED, SVNRevision.WORKING, SVNDepth.getInfinityOrEmptyDepth(true), null, new ISVNInfoHandler() {

                public void handleInfo(SVNInfo info) {
                    ret.add(info);
                }

            });
        } catch (SVNException e) {
            e.printStackTrace();
        }
        return ret;

    }

    public void resolveConflictedFile(SVNInfo info, File file, ResolveHandler handler) throws Exception {
        final String mine = "<<<<<<< .mine";
        final String delim = "=======";
        final String theirs = ">>>>>>> .r";
        String txt = JDIO.readFileToString(file);
        String pre, post;
        while (true) {
            int mineStart = txt.indexOf(mine);

            if (mineStart < 0) break;
            mineStart += mine.length();
            int delimStart = txt.indexOf(delim, mineStart);
            int theirsEnd = txt.indexOf(theirs, delimStart + delim.length());
            int end = theirsEnd + theirs.length();
            while (txt.charAt(end) != '\r' && txt.charAt(end) != '\n') {
                end++;
            }

            pre = txt.substring(0, mineStart - mine.length());
            post = txt.substring(end);
            while (pre.endsWith("\r") || pre.endsWith("\n"))
                pre = pre.substring(0, pre.length() - 1);
            while (post.startsWith("\r") || post.startsWith("\n"))
                post = post.substring(1);
            pre += "\r\n";
            post = "\r\n" + post;
            if (pre.trim().length() == 0) pre = pre.trim();
            if (post.trim().length() == 0) post = post.trim();
            String solve = handler.resolveConflict(info, file, txt, mineStart, delimStart, delimStart + delim.length(), theirsEnd);
            if (solve == null) throw new Exception("Could not resolve");
            txt = pre + solve.trim() + post;
        }
        JDIO.writeLocalFile(file, txt);

    }

    public void showStatus(File wcPath, boolean isRecursive, boolean isRemote, boolean isReportAll, boolean isIncludeIgnored, boolean isCollectParentExternals) throws SVNException {
        getClientManager().getStatusClient().doStatus(wcPath, SVNRevision.HEAD, SVNDepth.fromRecurse(isRecursive), isRemote, isReportAll, isIncludeIgnored, isCollectParentExternals, new StatusEventHandler(isRemote), null);
    }

    private SVNCommitClient getCommitClient() {

        if (commitClient == null) {
            commitClient = getClientManager().getCommitClient();
            commitClient.setEventHandler(this);
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
        try {
            getWCClient().doRevert(new File[] { dstPath }, SVNDepth.INFINITY, null);
        } catch (Exception e) {
            e.printStackTrace();
            cleanUp(dstPath, false);
        }
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

    /**
     * WCClientHanlder
     * 
     * @param event
     * @param progress
     * @throws SVNException
     */
    public void handleEvent(SVNEvent event, double progress) throws SVNException {
        /* WCCLient */
        String nullString = " ";
        SVNEventAction action = event.getAction();
        String pathChangeType = nullString;
        if (action == SVNEventAction.ADD) {
            /*
             * The item is scheduled for addition.
             */
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "A     " + event.getFile()));
            return;
        } else if (action == SVNEventAction.COPY) {
            /*
             * The item is scheduled for addition with history (copied, in other
             * words).
             */
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "A  +  " + event.getFile()));
            return;
        } else if (action == SVNEventAction.DELETE) {
            /*
             * The item is scheduled for deletion.
             */
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "D     " + event.getFile()));
            return;
        } else if (action == SVNEventAction.LOCKED) {
            /*
             * The item is locked.
             */
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "L     " + event.getFile()));
            return;
        } else if (action == SVNEventAction.LOCK_FAILED) {
            /*
             * Locking operation failed.
             */
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "failed to lock    " + event.getFile()));
            return;
        }

        /* Updatehandler */

        if (action == SVNEventAction.UPDATE_ADD) {
            /*
             * the item was added
             */
            pathChangeType = "A";
        } else if (action == SVNEventAction.UPDATE_DELETE) {
            /*
             * the item was deleted
             */
            pathChangeType = "D";
        } else if (action == SVNEventAction.UPDATE_UPDATE) {
            /*
             * Find out in details what state the item is (after having been
             * updated).
             * 
             * Gets the status of file/directory item contents. It is
             * SVNStatusType who contains information on the state of an item.
             */
            SVNStatusType contentsStatus = event.getContentsStatus();
            if (contentsStatus == SVNStatusType.CHANGED) {
                /*
                 * the item was modified in the repository (got the changes from
                 * the repository
                 */
                pathChangeType = "U";
            } else if (contentsStatus == SVNStatusType.CONFLICTED) {
                /*
                 * The file item is in a state of Conflict. That is, changes
                 * received from the repository during an update, overlap with
                 * local changes the user has in his working copy.
                 */

                pathChangeType = "C";
            } else if (contentsStatus == SVNStatusType.MERGED) {
                /*
                 * The file item was merGed (those changes that came from the
                 * repository did not overlap local changes and were merged into
                 * the file).
                 */
                pathChangeType = "G";
            }
        } else if (action == SVNEventAction.UPDATE_EXTERNAL) {
            /*
             * for externals definitions
             */
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "Fetching external item into '" + event.getFile().getAbsolutePath() + "'"));
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "External at revision " + event.getRevision()));
            return;
        } else if (action == SVNEventAction.UPDATE_COMPLETED) {
            /*
             * Working copy update is completed. Prints out the revision.
             */
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "At revision " + event.getRevision()));
            return;
        }

        /*
         * Status of properties of an item. SVNStatusType also contains
         * information on the properties state.
         */
        SVNStatusType propertiesStatus = event.getPropertiesStatus();
        String propertiesChangeType = nullString;
        if (propertiesStatus == SVNStatusType.CHANGED) {
            /*
             * Properties were updated.
             */
            propertiesChangeType = "U";
        } else if (propertiesStatus == SVNStatusType.CONFLICTED) {
            /*
             * Properties are in conflict with the repository.
             */
            propertiesChangeType = "C";
        } else if (propertiesStatus == SVNStatusType.MERGED) {
            /*
             * Properties that came from the repository were merged with the
             * local ones.
             */
            propertiesChangeType = "G";
        }

        /*
         * Gets the status of the lock.
         */
        String lockLabel = nullString;
        SVNStatusType lockType = event.getLockStatus();

        if (lockType == SVNStatusType.LOCK_UNLOCKED) {
            /*
             * The lock is broken by someone.
             */
            lockLabel = "B";
        }
        if (pathChangeType != nullString || propertiesChangeType != nullString || lockLabel != nullString) {
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), pathChangeType + propertiesChangeType + lockLabel + "       " + event.getFile()));
        }

        /*
         * Comitghandler
         */

        if (action == SVNEventAction.COMMIT_MODIFIED) {
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "Sending   " + event.getFile()));
        } else if (action == SVNEventAction.COMMIT_DELETED) {
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "Deleting   " + event.getFile()));
        } else if (action == SVNEventAction.COMMIT_REPLACED) {
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "Replacing   " + event.getFile()));
        } else if (action == SVNEventAction.COMMIT_DELTA_SENT) {
            broadcaster.fireEvent(new MessageEvent(this, action.getID(), "Transmitting file data...."));
        } else if (action == SVNEventAction.COMMIT_ADDED) {
            /*
             * Gets the MIME-type of the item.
             */
            String mimeType = event.getMimeType();
            if (SVNProperty.isBinaryMimeType(mimeType)) {
                /*
                 * If the item is a binary file
                 */
                broadcaster.fireEvent(new MessageEvent(this, action.getID(), "Adding  (bin)  " + event.getFile()));
            } else {
                broadcaster.fireEvent(new MessageEvent(this, action.getID(), "Adding         " + event.getFile()));
            }
        }

    }

    /**
     * WCClient
     */
    public void checkCancelled() throws SVNCancelException {
    }

    /**
     * checks wether logins are correct or not
     * 
     * @param url
     * @param user
     * @param pass
     * @return
     */
    public static boolean checkLogin(String url, String user, String pass) {
        try {
            new Subversion(url, user, pass);
            return true;
        } catch (SVNException e) {
        }
        return false;
    }

    public void dispose() {
        try {
            this.getClientManager().dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
