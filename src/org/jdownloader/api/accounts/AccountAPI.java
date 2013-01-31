package org.jdownloader.api.accounts;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("accounts")
public interface AccountAPI extends RemoteAPIInterface {
    public boolean addAccount(String premiumHoster, String username, String password);

    public List<AccountAPIStorable> queryAccounts(APIQuery query);

    public List<String> listPremiumHoster();

    public AccountAPIStorable getAccountInfo(long id);

    public boolean remove(Long[] ids);

    public boolean setEnabledState(boolean enabled, Long[] ids);

    void premiumHosterIcon(RemoteAPIRequest request, RemoteAPIResponse response, String premiumHoster);
}
