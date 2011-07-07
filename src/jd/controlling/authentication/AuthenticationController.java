package jd.controlling.authentication;

import java.util.ArrayList;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;

public class AuthenticationController {
    private static final AuthenticationController INSTANCE = new AuthenticationController();

    /**
     * get the only existing instance of AuthenticationController. This is a
     * singleton
     * 
     * @return
     */
    public static AuthenticationController getInstance() {
        return AuthenticationController.INSTANCE;
    }

    private AuthenticationControllerSettings config;
    private ArrayList<AuthenticationInfo>    list;

    /**
     * Create a new instance of AuthenticationController. This is a singleton
     * class. Access the only existing instance by using {@link #getInstance()}.
     */
    private AuthenticationController() {
        config = JsonConfig.create(AuthenticationControllerSettings.class);
        list = config.getList();
        if (list == null) list = new ArrayList<AuthenticationInfo>();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public void run() {
                synchronized (AuthenticationController.this) {
                    config.setList(list);
                }
            }

            @Override
            public String toString() {
                return "save auths...";
            }
        });
    }

    public synchronized ArrayList<AuthenticationInfo> list() {
        return new ArrayList<AuthenticationInfo>(list);
    }

    public void add(AuthenticationInfo a) {
        if (a == null) return;
        synchronized (this) {
            list.add(a);
            config.setList(list);
        }
    }

    public void remove(AuthenticationInfo a) {
        if (a == null) return;
        synchronized (this) {
            list.remove(a);
            config.setList(list);
        }
    }

    public void remove(ArrayList<AuthenticationInfo> selectedObjects) {
        if (selectedObjects == null) return;
        synchronized (this) {
            list.removeAll(selectedObjects);
            config.setList(list);
        }
    }

}
