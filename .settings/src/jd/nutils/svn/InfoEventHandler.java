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

import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.SVNInfo;

public class InfoEventHandler implements ISVNInfoHandler {

    public void handleInfo(SVNInfo info) {
        System.out.println("-----------------INFO-----------------");
        System.out.println("Local Path: " + info.getPath());
        System.out.println("URL: " + info.getURL());

        if (info.isRemote() && info.getRepositoryRootURL() != null) {
            System.out.println("Repository Root URL: " + info.getRepositoryRootURL());
        }

        if (info.getRepositoryUUID() != null) {
            System.out.println("Repository UUID: " + info.getRepositoryUUID());
        }

        System.out.println("Revision: " + info.getRevision().getNumber());
        System.out.println("Node Kind: " + info.getKind().toString());

        if (!info.isRemote()) {
            System.out.println("Schedule: " + (info.getSchedule() != null ? info.getSchedule() : "normal"));
        }

        System.out.println("Last Changed Author: " + info.getAuthor());
        System.out.println("Last Changed Revision: " + info.getCommittedRevision().getNumber());
        System.out.println("Last Changed Date: " + info.getCommittedDate());

        if (info.getPropTime() != null) {
            System.out.println("Properties Last Updated: " + info.getPropTime());
        }

        if (info.getKind() == SVNNodeKind.FILE && info.getChecksum() != null) {
            if (info.getTextTime() != null) {
                System.out.println("Text Last Updated: " + info.getTextTime());
            }
            System.out.println("Checksum: " + info.getChecksum());
        }

        if (info.getLock() != null) {
            if (info.getLock().getID() != null) {
                System.out.println("Lock Token: " + info.getLock().getID());
            }

            System.out.println("Lock Owner: " + info.getLock().getOwner());
            System.out.println("Lock Created: " + info.getLock().getCreationDate());

            if (info.getLock().getExpirationDate() != null) {
                System.out.println("Lock Expires: " + info.getLock().getExpirationDate());
            }

            if (info.getLock().getComment() != null) {
                System.out.println("Lock Comment: " + info.getLock().getComment());
            }
        }
    }
}
