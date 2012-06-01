package jd.config;

public interface GuiConfigListener {

    public Object getText();

    public void setData(Object data);

    public void dataChanged(ConfigEntry source, Object newData);

}
