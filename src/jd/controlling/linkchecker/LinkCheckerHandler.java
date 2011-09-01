package jd.controlling.linkchecker;

import jd.controlling.linkcrawler.CheckableLink;

public interface LinkCheckerHandler<E extends CheckableLink> {

    public void linkCheckStarted();

    public void linkCheckStopped();

    public void linkCheckDone(E link);
}
