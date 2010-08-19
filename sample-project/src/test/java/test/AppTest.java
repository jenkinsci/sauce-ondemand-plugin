package test;

import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;

public class AppTest extends TestCase
{
    public void test1() throws Exception {
        Selenium s = SeleniumFactory.create("http://www.google.com/");
        s.start();
        s.open("/");
        assertEquals("Google",s.getTitle());
        s.stop();
    }

    public void test2() throws Exception {
        Selenium s = SeleniumFactory.create("http://www.yahoo.com/");
        s.start();
        s.open("/");
        assertEquals("Yahoo!",s.getTitle());
        s.stop();
    }
}
