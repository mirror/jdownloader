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

import org.appwork.utils.net.throttledconnection.ThrottledConnection;
import org.appwork.utils.net.throttledconnection.ThrottledConnectionHandler;
import org.appwork.utils.speedmeter.SpeedMeterInterface;

/**
 *
 * @author Shashank Tulsyan
 */
public class FakeThrottledConnection implements SpeedMeterInterface, ThrottledConnection{

    
    private final DownloadSession jdds;
    private int limit = 0;
    private ThrottledConnectionHandler handler;
    
    public FakeThrottledConnection(DownloadSession jdds) {
        this.jdds = jdds;
        handler = jdds.getDownloadInterface().getManagedConnetionHandler();
    }
    
    public ThrottledConnectionHandler getHandler() {
        return handler;
    }

    public int getLimit() {
        return limit;
    }

    public void setHandler(ThrottledConnectionHandler manager) {
        this.handler = manager;
    }

    public void setLimit(int kpsLimit) {
        this.limit = kpsLimit;
    }

    public long transfered() {
        return jdds.getWatchAsYouDownloadSession().getTotalDownloaded();
    }
    
    public void resetSpeedMeter() {
        
    }

    public long getSpeedMeter() {
        return (long)(jdds.getWatchAsYouDownloadSession().getSeekableConnectionFile().getTotalFileReadStatistics().getTotalAverageDownloadSpeedProvider().getDownloadSpeed_KiBps()*1024);
    }

    public void putSpeedMeter(long bytes, long time) {
        
    }
    
}
