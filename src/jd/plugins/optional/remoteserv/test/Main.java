package jd.plugins.optional.remoteserv.test;

public class Main {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        // setup
        RemoteCallServerImpl.getInstance().addHandler(new ServiceImpl());
        // start
        RemoteCallServerImpl.getInstance().start();

        final Service inst = RemoteCallClientImpl.getInstance().getFactory().newInstance(Service.class);
        
        // call method
        System.out.println(inst.substract(3, 1));

        final DataImpl data = new DataImpl();
        data.setObj(new DataImpl());
        org.eclipse.jetty.io.UpgradeConnectionException.class
        inst.setData(data);

    }
}
