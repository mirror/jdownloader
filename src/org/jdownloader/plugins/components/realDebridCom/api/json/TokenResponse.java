package org.jdownloader.plugins.components.realDebridCom.api.json;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;

public class TokenResponse implements Storable {
    public static final org.appwork.storage.TypeRef<TokenResponse> TYPE       = new org.appwork.storage.TypeRef<TokenResponse>(TokenResponse.class) {
                                                                              };
    private String                                                 access_token;

    private long                                                   createTime = System.currentTimeMillis();

    private long                                                   expires_in;

    private String                                                 refresh_token;
    private String                                                 token_type;

    public TokenResponse(/* Storable */) {
    }

    public String getAccess_token() {
        return access_token;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getExpires_in() {
        return expires_in;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setExpires_in(long expires_in) {
        this.expires_in = expires_in;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public boolean validate() {
        return StringUtils.isNotEmpty(access_token) && StringUtils.isNotEmpty(refresh_token);
    }

}