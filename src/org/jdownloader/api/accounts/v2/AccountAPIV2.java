package org.jdownloader.api.accounts.v2;

import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.jdownloader.myjdownloader.client.bindings.AccountQuery;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.AccountAPIV2.NAMESPACE)
public interface AccountAPIV2 extends RemoteAPIInterface {
    public boolean addAccount(String premiumHoster, String username, String password);

    @AllowNonStorableObjects
    public List<AccountAPIStorableV2> listAccounts(AccountQuery query);

    public List<String> listPremiumHoster();

    public AccountAPIStorableV2 getAccountInfo(long id);

    public boolean removeAccounts(long[] ids);

    public boolean enableAccounts(long[] ids);

    public boolean disableAccounts(long[] ids);

    HashMap<String, String> listPremiumHosterUrls();

    boolean setUserNameAndPassword(Long accountId, String username, String password);

    String getPremiumHosterUrl(String hoster);
}
