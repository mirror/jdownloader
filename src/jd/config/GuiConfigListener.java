package jd.config;

import java.beans.PropertyChangeListener;

public interface GuiConfigListener extends PropertyChangeListener {

    public Object getText();

    public void setData(Object data);

}
