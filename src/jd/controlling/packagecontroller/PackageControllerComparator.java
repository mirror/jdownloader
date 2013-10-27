package jd.controlling.packagecontroller;

public abstract class PackageControllerComparator<T> implements java.util.Comparator<T> {
    public abstract String getID();

    public abstract boolean isAsc();
}
