package jd.http;

public class Auth {

    private String domain;
    private String pass;
    private String user;

    public Auth(String domain, String user, String pass) {
       this.domain=domain;
       this.pass=pass;
       this.user=user;
       
    }

    public String getAuthHeader() {
        // TODO Auto-generated method stub
        return "Basic "+Encoding.Base64Encode(user+":"+pass);
    }
    

}
