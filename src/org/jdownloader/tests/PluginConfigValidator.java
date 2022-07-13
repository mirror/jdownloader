package org.jdownloader.tests;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.simplejson.mapper.ClassCache;
import org.appwork.storage.simplejson.mapper.ClassCache.Rules;
import org.appwork.storage.tests.ClassPathScanner;
import org.appwork.storage.tests.StorableValidatorTest;
import org.appwork.testframework.IgnoreInAWTest;
import org.jdownloader.plugins.config.PluginConfigInterface;

public class PluginConfigValidator extends StorableValidatorTest {
    public static void main(String[] args) {
        run();
    }

    @Override
    public void runTest() throws Exception {
        final List<Class<?>> tested = new ArrayList<Class<?>>();
        new ClassPathScanner<Exception>() {
            @Override
            public void handle(Class<?> cls) throws Exception {
                if (PluginConfigInterface.class.isAssignableFrom(cls) && PluginConfigInterface.class != cls) {
                    validateClass(cls);
                    tested.add(cls);
                }
            }
        }.run();
    }

    @Override
    protected ClassCache createClassCache(Class<?> cls) throws Exception {
        final Rules rules = new Rules();
        rules.setIgnoreMissingConstructor(true);
        return ClassCache.create(cls, rules);
    }

    @Override
    protected void checkNoGetterSetter(ClassCache classCache, Class<?> cls) throws Exception {
    }

    @Override
    protected boolean skipValidation(Class<?> cls) throws Exception {
        if (org.appwork.storage.config.test.BadTestObject.class == cls) {
            return true;
        } else if (org.appwork.storage.simplejson.mapper.test.TestClass.class == cls) {
            return true;
        } else if (cls.getAnnotation(IgnoreInAWTest.class) != null) {
            return true;
        } else if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {
            return false;
        } else {
            return false;
        }
    }

    @Override
    protected void checkMissingConstructor(Class<?> cls) throws Exception {
    }
}
