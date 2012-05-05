package jd.controlling.packagecontroller;

public abstract class ChildComparator<T> implements java.util.Comparator<T> {
    public abstract String getID();

    public abstract boolean isAsc();
}
