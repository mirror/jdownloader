package org.jdownloader.api.test;

import java.util.ArrayList;
import java.util.HashSet;

import org.appwork.storage.Storage;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.bindings.AccountQuery;
import org.jdownloader.myjdownloader.client.bindings.AccountStorable;
import org.jdownloader.myjdownloader.client.bindings.interfaces.AccountInterface;

public class AccountTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        //
        final AccountInterface accounts = api.link(AccountInterface.class, chooseDevice(api));
        final ArrayList<String> premiumhoster = accounts.listPremiumHoster();
        AccountQuery query = new AccountQuery();
        query.setUserName(true);
        // should contain all
        ArrayList<AccountStorable> list = accounts.listAccounts(query);
        long firstID = 0;
        if (list.size() > 0) firstID = list.get(0).getUUID();
        HashSet<Long> accountsToRefresh = new HashSet<Long>();
        accountsToRefresh.add(1l);
        query.setUUIDList(accountsToRefresh);
        // should contain none
        list = accounts.listAccounts(query);
        accountsToRefresh.add(firstID);
        // should contain the first one
        list = accounts.listAccounts(query);
        System.out.println(list);
    }

    @Override
    public String getName() {
        return "Test Accounts";
    }

}
