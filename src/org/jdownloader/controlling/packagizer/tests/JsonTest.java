package org.jdownloader.controlling.packagizer.tests;

import java.lang.reflect.Type;

import org.appwork.storage.JSONMapper;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonSerializer;
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.TypeRef;
import org.appwork.testframework.AWTest;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.myjdownloader.client.json.JSonHandler;
import org.jdownloader.myjdownloader.client.json.JsonFactoryInterface;
import org.jdownloader.myjdownloader.client.json.MyJDJsonMapper;

public class JsonTest extends AWTest {
    public static void main(String[] args) {
        run();
    }

    @Override
    public void runTest() throws Exception {
        MyJDJsonMapper.HANDLER = new JSonHandler<Type>() {
            // set MyJDownloaderCLient JsonHandler
            @Override
            public String objectToJSon(Object payload) {
                return JSonStorage.serializeToJson(payload);
            }

            @Override
            public <T> T jsonToObject(String dec, final Type clazz) {
                return JSonStorage.restoreFromString(dec, new TypeRef<T>(clazz) {
                });
            }
        };
        testMapper(new SimpleMapper());
    }

    private void testMapper(final JSONMapper mapper) throws Exception {
        JSonStorage.setMapper(mapper);
        mapper.putSerializer(JsonFactoryInterface.class, new JsonSerializer() {
            @Override
            public String toJSonString(Object object, Object mapper) {
                return ((JsonFactoryInterface) object).toJsonString();
            }
        });
        PackagizerRule rule = new PackagizerRule();
        String json = JSonStorage.serializeToJson(rule);
        final PackagizerRule clone = mapper.stringToObject(json, new TypeRef<PackagizerRule>() {
        });
        rule.setCreated(0);
        clone.setCreated(0);
        assertEqualsDeep(rule, clone);
    }
}
