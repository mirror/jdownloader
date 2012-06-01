package jd.controlling.packagecontroller;

public interface AbstractNode {

    String getName();

    boolean isEnabled();

    void setEnabled(boolean b);

    long getCreated();

    long getFinishedDate();

}
