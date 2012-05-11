package jd;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import jd.controlling.AccountController;
import jd.plugins.Account;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.jackson.JacksonMapper;

public class PWRecovery {
    static {
        // USe Jacksonmapper in this project
        JSonStorage.setMapper(new JacksonMapper());
    }

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {
        // JDUtilities.setDB_CONNECT(new DatabaseConnector("/home/daniel/PWRecover/"));

        List<Account> accs = AccountController.getInstance().list();
        Iterator<Account> it = accs.iterator();
        while (it.hasNext()) {
            Account acc = it.next();
            System.out.println("Hoster: " + acc.getHoster() + "---->User:" + acc.getUser() + " Pass:" + acc.getPass());
        }

        System.exit(0);
    }
}
