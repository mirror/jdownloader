package jd.controlling.downloadcontroller;

import java.io.File;

public abstract class DiskSpaceReservation {

    abstract public File getDestination();

    abstract public long getSize();

}
