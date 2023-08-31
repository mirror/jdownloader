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


import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.SVNInfo;

public class InfoEventHandler implements ISVNInfoHandler {

    public void handleInfo(SVNInfo info) {
              org.appwork.loggingv3.LogV3.fine("-----------------INFO-----------------");
              org.appwork.loggingv3.LogV3.fine("Local Path: " + info.getPath());
              org.appwork.loggingv3.LogV3.fine("URL: " + info.getURL());

        if (info.isRemote() && info.getRepositoryRootURL() != null) {
                  org.appwork.loggingv3.LogV3.fine("Repository Root URL: " + info.getRepositoryRootURL());
        }

        if (info.getRepositoryUUID() != null) {
                  org.appwork.loggingv3.LogV3.fine("Repository UUID: " + info.getRepositoryUUID());
        }

              org.appwork.loggingv3.LogV3.fine("Revision: " + info.getRevision().getNumber());
              org.appwork.loggingv3.LogV3.fine("Node Kind: " + info.getKind().toString());

        if (!info.isRemote()) {
                  org.appwork.loggingv3.LogV3.fine("Schedule: " + (info.getSchedule() != null ? info.getSchedule() : "normal"));
        }

              org.appwork.loggingv3.LogV3.fine("Last Changed Author: " + info.getAuthor());
              org.appwork.loggingv3.LogV3.fine("Last Changed Revision: " + info.getCommittedRevision().getNumber());
              org.appwork.loggingv3.LogV3.fine("Last Changed Date: " + info.getCommittedDate());

        if (info.getPropTime() != null) {
                  org.appwork.loggingv3.LogV3.fine("Properties Last Updated: " + info.getPropTime());
        }

        if (info.getKind() == SVNNodeKind.FILE && info.getChecksum() != null) {
            if (info.getTextTime() != null) {
                      org.appwork.loggingv3.LogV3.fine("Text Last Updated: " + info.getTextTime());
            }
                  org.appwork.loggingv3.LogV3.fine("Checksum: " + info.getChecksum());
        }

        if (info.getLock() != null) {
            if (info.getLock().getID() != null) {
                      org.appwork.loggingv3.LogV3.fine("Lock Token: " + info.getLock().getID());
            }

                  org.appwork.loggingv3.LogV3.fine("Lock Owner: " + info.getLock().getOwner());
                  org.appwork.loggingv3.LogV3.fine("Lock Created: " + info.getLock().getCreationDate());

            if (info.getLock().getExpirationDate() != null) {
                      org.appwork.loggingv3.LogV3.fine("Lock Expires: " + info.getLock().getExpirationDate());
            }

            if (info.getLock().getComment() != null) {
                      org.appwork.loggingv3.LogV3.fine("Lock Comment: " + info.getLock().getComment());
            }
        }
    }
}
