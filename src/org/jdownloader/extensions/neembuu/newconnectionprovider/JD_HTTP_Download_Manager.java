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
package org.jdownloader.extensions.neembuu.newconnectionprovider;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import neembuu.vfs.connection.NewConnectionParams;
import neembuu.vfs.connection.NewConnectionProvider;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.neembuu.DownloadSession;

/**
 * 
 * @author Shashank Tulsyan
 */
public final class JD_HTTP_Download_Manager implements NewConnectionProvider {
    final DownloadSession                                   jdds;

    private static final Logger                             LOGGER          = Logger.getLogger(JD_HTTP_Download_Manager.class.getName());

    private final ConcurrentLinkedQueue<JD_HTTP_Connection> connection_list = new ConcurrentLinkedQueue<JD_HTTP_Connection>();

    public JD_HTTP_Download_Manager(DownloadSession jdds) {
        this.jdds = jdds;
    }

    private void connectionsRequested() {
        String message = "++++++++++ConnectionsRequested+++++++++\n";
        message = message + "DownloadLink=" + jdds.getDownloadLink() + "\n";
        for (JD_HTTP_Connection e : connection_list) {
            message = message + e.getConnectionParams().toString() + "\n";
        }
        message = message + "---------ConnectionsRequested--------\n";

        LOGGER.info(message);
    }

    // @Override
    public final String getSourceDescription() {
        return jdds.getDownloadLink().getDownloadURL();
    }

    // @Override
    public final void provideNewConnection(final NewConnectionParams connectionParams) {
        class StartNewJDBrowserConnectionThread extends Thread {

            StartNewJDBrowserConnectionThread() {
                // always name thread, otherwise it can be extremely difficult
                // to debug
                super("StartNew[JD_HTTP_Download_Manager]{" + connectionParams + "}");
            }

            @Override
            public final void run() {
                try {
                    JD_HTTP_Connection c = new JD_HTTP_Connection(JD_HTTP_Download_Manager.this, connectionParams);
                    connection_list.add(c);
                    connectionsRequested();
                    c.connectAndSupply();
                } catch (Exception e) {
                    Logger.getGlobal().log(Level.INFO, "Problem in new connection ", e);
                }
            }
        }

        new StartNewJDBrowserConnectionThread().start();
    }

    // @Override
    public final long estimateCreationTime(long offset) {
        int c = jdds.getDownloadLink().getDefaultPlugin().getMaxSimultanFreeDownloadNum();
        if(c<0) c = Integer.MAX_VALUE; // unlimited con allowed
        // TODO : replace above code to get exact time it will take
        // to create a new connection right now with the account user is using.
        if(c < 5 && offset!=0){
            return Integer.MAX_VALUE;
        }else {
            return averageConnectionCreationTime();
        }
    }

    private long averageConnectionCreationTime() {
        int i = 0;
        long totalTime = 0;
        for (JD_HTTP_Connection connection : connection_list) {
            if (connection.succeededInCreation()) {
                totalTime += connection.timeTakenForCreation();
                i++;
            }
        }
        if (i == 0) { return 0;// creation time is unknown
        }
        return ((totalTime) / i);
    }

    long[] totalProgress = { 0 };

    @Override
    public String toString() {
        return JD_HTTP_Download_Manager.class.getName()+"{"+jdds.getDownloadLink()+ "}";
    }

}
