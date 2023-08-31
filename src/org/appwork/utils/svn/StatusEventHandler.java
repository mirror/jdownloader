/**
 * 
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany   
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 	
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header. 	
 * 	
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the 
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 	
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.svn;


import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

public class StatusEventHandler implements ISVNStatusHandler, ISVNEventHandler {
    private final boolean myIsRemote;

    public StatusEventHandler(final boolean isRemote) {
        this.myIsRemote = isRemote;
    }

    public void checkCancelled() throws SVNCancelException {
    }

    public void handleEvent(final SVNEvent event, final double progress) {
        final SVNEventAction action = event.getAction();
        /*
         * Print out the revision against which the status was performed. This
         * event is dispatched when the SVNStatusClient.doStatus() was invoked
         * with the flag remote set to true - that is for a local status it
         * won't be dispatched.
         */
        if (action == SVNEventAction.STATUS_COMPLETED) {
                  org.appwork.loggingv3.LogV3.fine("Status against revision:  " + event.getRevision());
        }

    }

    public void handleStatus(final SVNStatus status) {
        /*
         * Gets the status of file/directory/symbolic link text contents. It is
         * SVNStatusType who contains information on the state of an item.
         */
        final SVNStatusType contentsStatus = status.getContentsStatus();

        String pathChangeType = " ";

        final boolean isAddedWithHistory = status.isCopied();
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
        final SVNStatusType propertiesStatus = status.getPropertiesStatus();

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
        final boolean isLocked = status.isLocked();
        /*
         * Whether the item is switched to a different URL (branch).
         */
        final boolean isSwitched = status.isSwitched();
        /*
         * If the item is a file it may be locked.
         */
        final SVNLock localLock = status.getLocalLock();
        /*
         * If doStatus() was run with remote = true and the item is a file,
         * checks whether a remote lock presents.
         */
        final SVNLock remoteLock = status.getRemoteLock();
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
                if (this.myIsRemote) {
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
        final long workingRevision = status.getRevision().getNumber();
        /*
         * Obtains the number of the revision when the item was last changed.
         */
        final long lastChangedRevision = status.getCommittedRevision().getNumber();
        final String offset = "                                ";
        final String[] offsets = new String[3];
        offsets[0] = offset.substring(0, 6 - String.valueOf(workingRevision).length());
        offsets[1] = offset.substring(0, 6 - String.valueOf(lastChangedRevision).length());
        offsets[2] = offset.substring(0, offset.length() - (status.getAuthor() != null ? status.getAuthor().length() : 1));
        /*
         * status is shown in the manner of the native Subversion command line
         * client's command "svn status"
         */

              org.appwork.loggingv3.LogV3.fine(pathChangeType + propertiesChangeType + (isLocked ? "L" : " ") + (isAddedWithHistory ? "+" : " ") + (isSwitched ? "S" : " ") + lockLabel + "  " + remoteChangeType + "  " + workingRevision + offsets[0] + (lastChangedRevision >= 0 ? String.valueOf(lastChangedRevision) : "?") + offsets[1] + (status.getAuthor() != null ? status.getAuthor() : "?") + offsets[2] + status.getFile().getPath());
    }
}
