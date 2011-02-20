package test;

import com.saucelabs.selenium.client.factory.SeleniumFactory;
import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;

public class AppTest extends TestCase
{
    public void test1() throws Exception {
        Selenium s = SeleniumFactory.create("http://www.w3c.org/");
        s.start();
        s.open("/");
        assertEquals("World Wide Web Consortium (W3C)",s.getTitle());
        s.stop();
    }

    public void test2() throws Exception {
        Selenium s = SeleniumFactory.create("http://en.wikipedia.org/wiki/Main_Page");
        s.start();
        s.open("/");
        assertEquals("Wikipedia, the free encyclopedia",s.getTitle());
        s.stop();
    }
}
