package jd.controlling.linkchecker;

import jd.controlling.linkcrawler.CheckableLink;

public interface LinkCheckerHandler<E extends CheckableLink> {

    public void linkCheckDone(E link);
}
