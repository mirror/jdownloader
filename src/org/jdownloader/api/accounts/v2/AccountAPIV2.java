package org.jdownloader.api.accounts.v2;

import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.myjdownloader.client.bindings.AccountQuery;
import org.jdownloader.myjdownloader.client.bindings.BasicAuthenticationStorable.Type;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.AccountInterface.NAMESPACE)
public interface AccountAPIV2 extends RemoteAPIInterface {
    public void addAccount(String premiumHoster, String username, String password);

    @AllowNonStorableObjects
    public List<AccountAPIStorableV2> listAccounts(AccountQuery query);

    public List<BasicAuthenticationAPIStorable> listBasicAuth();

    public List<String> listPremiumHoster();

    public void removeAccounts(long[] ids);

    public void enableAccounts(long[] ids);

    public void disableAccounts(long[] ids);

    HashMap<String, String> listPremiumHosterUrls();

    boolean setUserNameAndPassword(long accountId, String username, String password);

    String getPremiumHosterUrl(String hoster);

    void refreshAccounts(long[] ids);

    long addBasicAuth(Type type, String hostmask, String username, String password) throws BadParameterException;

    boolean removeBasicAuths(final long[] ids) throws BadParameterException;

    boolean updateBasicAuth(BasicAuthenticationAPIStorable updatedEntry) throws BadParameterException;
}
