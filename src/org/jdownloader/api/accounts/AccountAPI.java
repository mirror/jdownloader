package org.jdownloader.api.accounts;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.jdownloader.myjdownloader.client.json.JsonMap;

@ApiNamespace("accounts")
@Deprecated
public interface AccountAPI extends RemoteAPIInterface {
    @APIParameterNames({ "premiumHoster", "username", "password" })
    public boolean addAccount(String premiumHoster, String username, String password);

    @APIParameterNames({ "query" })
    public List<AccountAPIStorable> queryAccounts(APIQuery query);

    public List<String> listPremiumHoster();

    @APIParameterNames({ "id" })
    public AccountAPIStorable getAccountInfo(long id);

    @APIParameterNames({ "ids" })
    public boolean removeAccounts(Long[] ids);

    @APIParameterNames({ "ids" })
    public boolean enableAccounts(List<Long> ids);

    @APIParameterNames({ "ids" })
    public boolean disableAccounts(List<Long> ids);

    @APIParameterNames({ "enabled", "ids" })
    public boolean setEnabledState(boolean enabled, Long[] ids);

    @APIParameterNames({ "request", "response", "premiumHoster" })
    void premiumHosterIcon(RemoteAPIRequest request, RemoteAPIResponse response, String premiumHoster) throws InternalApiException;

    JsonMap listPremiumHosterUrls();

    @APIParameterNames({ "accountId", "username", "password" })
    boolean updateAccount(Long accountId, String username, String password);

    @APIParameterNames({ "hoster" })
    String getPremiumHosterUrl(String hoster);
}
