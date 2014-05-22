package org.jdownloader.statistics;

import jd.http.Browser;

public interface AbstractTrackEntry extends StatsLogInterface {

    public void send(Browser br);

}
