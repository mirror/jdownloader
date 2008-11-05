package jd.config;

public abstract class ConfigurationListener {

    abstract public void onPreSave(SubConfiguration subConfiguration);

    abstract public void onPostSave(SubConfiguration subConfiguration);
}
