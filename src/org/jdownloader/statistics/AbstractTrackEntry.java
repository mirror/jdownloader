package org.jdownloader.statistics;

import java.io.IOException;

import jd.http.Browser;

public interface AbstractTrackEntry extends StatsLogInterface {

    public void send(Browser br) throws IOException;

}
