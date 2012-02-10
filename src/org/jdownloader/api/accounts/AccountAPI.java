package org.jdownloader.api.accounts;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("accounts")
public interface AccountAPI extends RemoteAPIInterface {

    public List<AccountStorable> list();
}
