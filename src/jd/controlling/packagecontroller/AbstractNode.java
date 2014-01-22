package jd.controlling.packagecontroller;

import org.jdownloader.controlling.UniqueAlltimeID;

public interface AbstractNode {

    String getName();

    boolean isEnabled();

    void setEnabled(boolean b);

    long getCreated();

    long getFinishedDate();

    UniqueAlltimeID getUniqueID();

}
