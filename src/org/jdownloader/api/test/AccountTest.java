package org.jdownloader.api.test;

import java.util.ArrayList;

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
        final ArrayList<AccountStorable> list = accounts.listAccounts(new AccountQuery(0, -1, true, false, false, false, false, false));
        System.out.println(list);
    }

    @Override
    public String getName() {
        return "Test Accounts";
    }

}
