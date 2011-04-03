package jd.config;

public class AboutConfig {
    private static final AboutConfig INSTANCE = new AboutConfig();

    public static AboutConfig getInstance() {
        return AboutConfig.INSTANCE;
    }

    private AboutConfig() {

    }
}
