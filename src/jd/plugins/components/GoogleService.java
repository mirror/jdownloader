package jd.plugins.components;

public enum GoogleService {
    YOUTUBE("youtube", "youtube:210:1", "https://www.youtube.com/signin?action_handle_signin=true", "http://www.youtube.com/signin?action_handle_signin=true&nomobiletemp=1&hl=en_US&next=%2Findex", "https://www.youtube.com/signin?action_handle_signin=true&app=desktop&feature=sign_in_button&next=%2F&hl=en");
    public final String serviceName;
    public final String checkConnectionString;
    public final String continueAfterCheckCookie;
    public final String continueAfterServiceLogin;
    public final String continueAfterServiceLoginAuth;

    private GoogleService(String service, String checkConnection, String afterLogin, String continueAfterServiceLogin, String continueAfterServiceLoginAuth) {
        this.serviceName = service;
        this.checkConnectionString = checkConnection;
        this.continueAfterCheckCookie = afterLogin;
        this.continueAfterServiceLogin = continueAfterServiceLogin;
        this.continueAfterServiceLoginAuth = continueAfterServiceLoginAuth;
    }
}