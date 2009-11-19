package jd.plugins.optional.routerdbeditor;

public class Router {
    private String hersteller;
    private String name;
    private String username;
    private String pass;
    private String regex;
    private String script;
    public Router(String hersteller, String name, String username, String pass, String regex, String script) {
        super();
        this.hersteller = hersteller;
        this.name = name;
        this.pass = pass;
        this.regex = regex;
        this.script = script;
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHersteller() {
        return hersteller;
    }
    public void setHersteller(String hersteller) {
        this.hersteller = hersteller;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPass() {
        return pass;
    }
    public void setPass(String pass) {
        this.pass = pass;
    }
    public String getRegex() {
        return regex;
    }
    public void setRegex(String regex) {
        this.regex = regex;
    }
    public String getScript() {
        return script;
    }
    public void setScript(String script) {
        this.script = script;
    }
    
}
