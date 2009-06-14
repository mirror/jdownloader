package tests.testsuits;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import tests.singletests.ConnectionControl;
import tests.singletests.ProxyAuthTest;

@RunWith(Suite.class)
@SuiteClasses({ConnectionControl.class, ProxyAuthTest.class})
public class BrowserTests {}