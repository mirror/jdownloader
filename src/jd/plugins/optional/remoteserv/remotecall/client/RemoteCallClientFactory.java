package jd.plugins.optional.remoteserv.remotecall.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RemoteCallClientFactory {

    private final RemoteCallClient client;

    public RemoteCallClientFactory(final RemoteCallClient remoteCallClient) {
        this.client = remoteCallClient;
    }

    @SuppressWarnings("unchecked")
    public <T> T newInstance(final Class<T> class1) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] { class1 }, new InvocationHandler() {

            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

                final TypeConverter converter = TypeConverterFactory.getInstance(method.getReturnType());
                final Object returnValue = RemoteCallClientFactory.this.client.call(class1.getSimpleName(), method.getName(), args);
                return converter.convert(returnValue);

            }
        });
    }

}
