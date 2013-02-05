package jd.plugins;

import java.util.EventListener;

import jd.plugins.LinkStatus;

public interface LinkStatusEventListener extends EventListener {

    public void LinkStatusChanged(LinkStatus linkStatus);

}
