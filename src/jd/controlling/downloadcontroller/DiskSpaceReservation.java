package jd.controlling.downloadcontroller;

import java.io.File;

import org.appwork.utils.logging2.LogInterface;

public abstract class DiskSpaceReservation {
    abstract public Object getOwner();

    abstract public LogInterface getLogger();

    abstract public File getDestination();

    abstract public long getSize();
}
