package jd.gui.swing.laf;

import java.io.Serializable;

import javax.swing.UIManager.LookAndFeelInfo;

public class LookAndFeelWrapper implements Serializable {

    private static final long serialVersionUID = 8010506524416796786L;
    private String className;
    private String name;

    public LookAndFeelWrapper(LookAndFeelInfo lafi) {
        this.className = lafi.getClassName();
        this.name = lafi.getName();
    }

    public LookAndFeelWrapper(String className) {
        this.className = className;
        name = className.substring(className.lastIndexOf(".") + 1);
    }

    /**
     * Sets a static name. just fort displaying
     * 
     * @param string
     * @return
     */
    public LookAndFeelWrapper setName(String string) {
        this.name = string;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof LookAndFeelWrapper) && ((LookAndFeelWrapper) obj).getClassName() != null && ((LookAndFeelWrapper) obj).getClassName().equals(className);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isJTattoo() {
        return this.className.contains("jtattoo");
    }

    public boolean isSubstance() {
        return this.className.contains("substance");
    }

    public String getName() {
        return this.name;
    }
}
