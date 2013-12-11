package org.jdownloader.api.accounts.v2;

import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.jdownloader.myjdownloader.client.bindings.AccountQuery;

@ApiNamespace("accountsv2")
public interface AccountAPIV2 extends RemoteAPIInterface {
    public boolean addAccount(String premiumHoster, String username, String password);

    @AllowNonStorableObjects
    public List<AccountAPIStorableV2> queryAccounts(AccountQuery query);

    public List<String> listPremiumHoster();

    public AccountAPIStorableV2 getAccountInfo(long id);

    public boolean removeAccounts(Long[] ids);

    public boolean enableAccounts(List<Long> ids);

    public boolean disableAccounts(List<Long> ids);

    public boolean setEnabledState(boolean enabled, Long[] ids);

    void premiumHosterIcon(RemoteAPIRequest request, RemoteAPIResponse response, String premiumHoster) throws InternalApiException;

    HashMap<String, String> listPremiumHosterUrls();

    boolean updateAccount(Long accountId, String username, String password);

    String getPremiumHosterUrl(String hoster);
}
