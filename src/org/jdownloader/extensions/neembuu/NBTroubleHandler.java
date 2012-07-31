/*
 * Copyright (C) 2012 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.extensions.neembuu;

import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jpfm.operations.readwrite.ReadRequest;
import neembuu.vfs.connection.NewConnectionParams;
import neembuu.vfs.file.TroubleHandler;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.neembuu.translate._NT;

/**
 * 
 * @author Shashank Tulsyan
 */
public final class NBTroubleHandler implements TroubleHandler {

    private final DownloadSession jdds;

    public NBTroubleHandler(DownloadSession jdds) {
        this.jdds = jdds;
    }

    // @Override
    public final void cannotCreateANewConnection(final NewConnectionParams ncp, final int numberOfRetries) {
        final String mess = _NT._.troubleHandler_retriedConnection() + numberOfRetries + _NT._.troubleHandler_retriedConnection_times();
        final String mess2 = ncp.toString();
        SwingUtilities.invokeLater(new Runnable() {
            // @Override
            public void run() {
                JOptionPane.showMessageDialog(null, mess + "\n" + mess2, _NT._.failed_WatchAsYouDownload_Title() + " " + jdds, JOptionPane.ERROR);
            }
        });

        Log.L.log(Level.SEVERE, mess + " " + mess2);

        try {
            jdds.getWatchAsYouDownloadSession().getVirtualFileSystem().unmountAndEndSessions();
        } catch (Exception a) {
            Log.L.log(Level.SEVERE, "unmounting problem", a);
        }
    }

    // @Override
    public final void readRequestsPendingSinceALongTime(List<ReadRequest> pendingReadRequest, long atleastMillisec) {
        long maxMillisecWait = atleastMillisec;
        for (ReadRequest rr : pendingReadRequest) {
            if (!rr.isCompleted()) {
                long pendingSince = System.currentTimeMillis() - rr.getCreationTime();
                maxMillisecWait = Math.max(maxMillisecWait, pendingSince);
            }
        }

        final String mess = _NT._.troubleHandler_pendingRequests() + (maxMillisecWait / 60000d) + _NT._.troubleHandler_pendingRequests_minutes();
        final String mess2 = _NT._.troubleHandler_pendingRequests_Solution();

        Log.L.log(Level.SEVERE, mess, new Throwable());

        if (maxMillisecWait > 3 * 60 * 1000) { // if a request is pending since
                                               // last 3 mintues ... we better
                                               // quit
            SwingUtilities.invokeLater(new Runnable() {
                // @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, mess + "\n" + mess2, _NT._.failed_WatchAsYouDownload_Title() + " " + jdds, JOptionPane.ERROR);
                }
            });

            Log.L.log(Level.SEVERE, mess2);

            try {
                jdds.getWatchAsYouDownloadSession().getVirtualFileSystem().unmountAndEndSessions();
            } catch (Exception a) {
                Log.L.log(Level.SEVERE, "unmounting problem", a);
            }
        }
    }

}
