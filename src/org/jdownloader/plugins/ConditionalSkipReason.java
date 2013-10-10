package org.jdownloader.plugins;

import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

public interface ConditionalSkipReason {

    public boolean isConditionReached();

    public String getMessage(Object requestor, AbstractNode node);

    public ImageIcon getIcon(Object requestor, AbstractNode node);

    public void finalize(DownloadLink link);

}
