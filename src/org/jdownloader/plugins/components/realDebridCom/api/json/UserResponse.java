package org.jdownloader.plugins.components.realDebridCom.api.json;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;

public class UserResponse implements Storable {
    public static final TypeRef<UserResponse> TYPE = new TypeRef<UserResponse>(UserResponse.class) {
                                                   };
    private String                            avatar;

    private String                            email;
    private String                            expiration;

    private long                              id;

    private String                            locale;

    private long                              polongs;

    private long                              premium;

    private String                            type;

    private String                            username;

    public UserResponse(/* Storable */) {
    }

    public String getAvatar() {
        return avatar;
    }

    public String getEmail() {
        return email;
    }

    public String getExpiration() {
        return expiration;
    }

    public long getId() {
        return id;
    }

    public String getLocale() {
        return locale;
    }

    public long getPolongs() {
        return polongs;
    }

    public long getPremium() {
        return premium;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setPolongs(long polongs) {
        this.polongs = polongs;
    }

    public void setPremium(long premium) {
        this.premium = premium;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}