//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

public class StatusEventHandler implements ISVNStatusHandler, ISVNEventHandler {
    private boolean myIsRemote;

    public StatusEventHandler(boolean isRemote) {
        myIsRemote = isRemote;
    }

    public void handleStatus(SVNStatus status) {
        /*
         * Gets the status of file/directory/symbolic link text contents. It is
         * SVNStatusType who contains information on the state of an item.
         */
        SVNStatusType contentsStatus = status.getContentsStatus();

        String pathChangeType = " ";

        boolean isAddedWithHistory = status.isCopied();
        if (contentsStatus == SVNStatusType.STATUS_MODIFIED) {
            /*
             * The contents of the file have been Modified.
             */
            pathChangeType = "M";
        } else if (contentsStatus == SVNStatusType.STATUS_CONFLICTED) {
            /*
             * The item is in a state of Conflict.
             */
            pathChangeType = "C";
        } else if (contentsStatus == SVNStatusType.STATUS_DELETED) {
            /*
             * The item has been scheduled for Deletion from the repository.
             */
            pathChangeType = "D";
        } else if (contentsStatus == SVNStatusType.STATUS_ADDED) {
            /*
             * The item has been scheduled for Addition to the repository.
             */
            pathChangeType = "A";
        } else if (contentsStatus == SVNStatusType.STATUS_UNVERSIONED) {
            /*
             * The item is not under version control.
             */
            pathChangeType = "?";
        } else if (contentsStatus == SVNStatusType.STATUS_EXTERNAL) {
            /*
             * The item is unversioned, but is used by an eXternals definition.
             */
            pathChangeType = "X";
        } else if (contentsStatus == SVNStatusType.STATUS_IGNORED) {
            /*
             * The item is Ignored.
             */
            pathChangeType = "I";
        } else if (contentsStatus == SVNStatusType.STATUS_MISSING || contentsStatus == SVNStatusType.STATUS_INCOMPLETE) {
            /*
             * The file, directory or symbolic link item is under version
             * control but is missing or somehow incomplete.
             */
            pathChangeType = "!";
        } else if (contentsStatus == SVNStatusType.STATUS_OBSTRUCTED) {
            /*
             * The item is in the repository as one kind of object, but what's
             * actually in the user's working copy is some other kind.
             */
            pathChangeType = "~";
        } else if (contentsStatus == SVNStatusType.STATUS_REPLACED) {
            /*
             * The item was Replaced in the user's working copy; that is, the
             * item was deleted, and a new item with the same name was added
             * (within a single revision).
             */
            pathChangeType = "R";
        } else if (contentsStatus == SVNStatusType.STATUS_NONE || contentsStatus == SVNStatusType.STATUS_NORMAL) {
            /*
             * The item was not modified (normal).
             */
            pathChangeType = " ";
        }

        /*
         * If SVNStatusClient.doStatus(..) is invoked with remote = true the
         * following code finds out whether the current item has been changed in
         * the repository
         */
        String remoteChangeType = " ";

        if (status.getRemotePropertiesStatus() != SVNStatusType.STATUS_NONE || status.getRemoteContentsStatus() != SVNStatusType.STATUS_NONE) {
            /*
             * the local item is out of date
             */
            remoteChangeType = "*";
        }

        /*
         * Now getting the status of properties of an item. SVNStatusType also
         * contains information on the properties state.
         */
        SVNStatusType propertiesStatus = status.getPropertiesStatus();

        /*
         * Default - properties are normal (unmodified).
         */
        String propertiesChangeType = " ";
        if (propertiesStatus == SVNStatusType.STATUS_MODIFIED) {
            /*
             * Properties were modified.
             */
            propertiesChangeType = "M";
        } else if (propertiesStatus == SVNStatusType.STATUS_CONFLICTED) {
            /*
             * Properties are in conflict with the repository.
             */
            propertiesChangeType = "C";
        }

        /*
         * Whether the item was locked in the .svn working area (for example,
         * during a commit or maybe the previous operation was interrupted, in
         * this case the lock needs to be cleaned up).
         */
        boolean isLocked = status.isLocked();
        /*
         * Whether the item is switched to a different URL (branch).
         */
        boolean isSwitched = status.isSwitched();
        /*
         * If the item is a file it may be locked.
         */
        SVNLock localLock = status.getLocalLock();
        /*
         * If doStatus() was run with remote = true and the item is a file,
         * checks whether a remote lock presents.
         */
        SVNLock remoteLock = status.getRemoteLock();
        String lockLabel = " ";

        if (localLock != null) {
            /*
             * at first suppose the file is locKed
             */
            lockLabel = "K";
            if (remoteLock != null) {
                /*
                 * if the lock-token of the local lock differs from the lock-
                 * token of the remote lock - the lock was sTolen!
                 */
                if (!remoteLock.getID().equals(localLock.getID())) {
                    lockLabel = "T";
                }
            } else {
                if (myIsRemote) {
                    /*
                     * the local lock presents but there's no lock in the
                     * repository - the lock was Broken. This is true only if
                     * doStatus() was invoked with remote=true.
                     */
                    lockLabel = "B";
                }
            }
        } else if (remoteLock != null) {
            /*
             * the file is not locally locked but locked in the repository - the
             * lock token is in some Other working copy.
             */
            lockLabel = "O";
        }

        /*
         * Obtains the working revision number of the item.
         */
        long workingRevision = status.getRevision().getNumber();
        /*
         * Obtains the number of the revision when the item was last changed.
         */
        long lastChangedRevision = status.getCommittedRevision().getNumber();
        String offset = "                                ";
        String[] offsets = new String[3];
        offsets[0] = offset.substring(0, 6 - String.valueOf(workingRevision).length());
        offsets[1] = offset.substring(0, 6 - String.valueOf(lastChangedRevision).length());
        offsets[2] = offset.substring(0, offset.length() - (status.getAuthor() != null ? status.getAuthor().length() : 1));
        /*
         * status is shown in the manner of the native Subversion command line
         * client's command "svn status"
         */
        System.out.println(pathChangeType + propertiesChangeType + (isLocked ? "L" : " ") + (isAddedWithHistory ? "+" : " ") + (isSwitched ? "S" : " ") + lockLabel + "  " + remoteChangeType + "  " + workingRevision + offsets[0] + (lastChangedRevision >= 0 ? String.valueOf(lastChangedRevision) : "?") + offsets[1] + (status.getAuthor() != null ? status.getAuthor() : "?") + offsets[2] + status.getFile().getPath());
    }

    public void handleEvent(SVNEvent event, double progress) {
        SVNEventAction action = event.getAction();
        /*
         * Print out the revision against which the status was performed. This
         * event is dispatched when the SVNStatusClient.doStatus() was invoked
         * with the flag remote set to true - that is for a local status it
         * won't be dispatched.
         */
        if (action == SVNEventAction.STATUS_COMPLETED) {
            System.out.println("Status against revision:  " + event.getRevision());
        }

    }

    public void checkCancelled() throws SVNCancelException {
    }
}
