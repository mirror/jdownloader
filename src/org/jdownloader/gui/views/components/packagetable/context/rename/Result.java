package org.jdownloader.gui.views.components.packagetable.context.rename;

public class Result {

    private String newName;

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    private String oldName;
    private Object object;

    public Result(String name, String newName, Object l) {
        this.oldName = name;
        this.newName = newName;
        this.object = l;
    }

}
