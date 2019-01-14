package org.jdownloader.api.accounts.v2;

import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.myjdownloader.client.bindings.AccountQuery;
import org.jdownloader.myjdownloader.client.bindings.BasicAuthenticationStorable.Type;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.AccountInterface.NAMESPACE)
public interface AccountAPIV2 extends RemoteAPIInterface {
    @APIParameterNames({ "premiumHoster", "username", "password" })
    public void addAccount(String premiumHoster, String username, String password);

    @AllowNonStorableObjects(clazz = { AccountAPIStorableV2.class, AccountQuery.class })
    @APIParameterNames({ "query" })
    public List<AccountAPIStorableV2> listAccounts(AccountQuery query);

    public List<BasicAuthenticationAPIStorable> listBasicAuth();

    public List<String> listPremiumHoster();

    @APIParameterNames({ "ids" })
    public void removeAccounts(long[] ids);

    @APIParameterNames({ "ids" })
    public void enableAccounts(long[] ids);

    @APIParameterNames({ "ids" })
    public void disableAccounts(long[] ids);

    HashMap<String, String> listPremiumHosterUrls();

    @APIParameterNames({ "accountId", "username", "password" })
    boolean setUserNameAndPassword(long accountId, String username, String password);

    @APIParameterNames({ "hoster" })
    String getPremiumHosterUrl(String hoster);

    @APIParameterNames({ "ids" })
    void refreshAccounts(long[] ids);

    @APIParameterNames({ "type", "hostmask", "username", "password" })
    long addBasicAuth(Type type, String hostmask, String username, String password) throws BadParameterException;

    @APIParameterNames({ "ids" })
    boolean removeBasicAuths(final long[] ids) throws BadParameterException;

    @APIParameterNames({ "updatedEntry" })
    boolean updateBasicAuth(BasicAuthenticationAPIStorable updatedEntry) throws BadParameterException;
}
