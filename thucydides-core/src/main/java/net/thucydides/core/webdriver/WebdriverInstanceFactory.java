package net.thucydides.core.webdriver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.net.URL;

/**
 * Centralize instantiation of WebDriver drivers.
 */
public class WebdriverInstanceFactory {

    public WebdriverInstanceFactory() {
    }

    public WebDriver newInstanceOf(final Class<? extends WebDriver> webdriverClass) throws IllegalAccessException, InstantiationException {
        return webdriverClass.newInstance();
    }

    public WebDriver newRemoteDriver(URL remoteUrl, Capabilities capabilities) {
        return new RemoteWebDriver(remoteUrl, capabilities);
    }

    public WebDriver newFirefoxDriver(Capabilities capabilities) {
        return new FirefoxDriver(capabilities);
    }

    public WebDriver newChromeDriver(Capabilities capabilities) {
        return new ChromeDriver(capabilities);
    }

    public WebDriver newAppiumDriver(URL hub, Capabilities capabilities, MobilePlatform platform) {
        switch (platform) {
            case ANDROID: return new AndroidDriver(hub, capabilities);
            case IOS: return new IOSDriver(hub, capabilities);
        }
        throw new UnsupportedDriverException(platform.name());
    }
    public WebDriver newSafariDriver(Capabilities capabilities) {
        return new SafariDriver(capabilities);
    }

    public WebDriver newInternetExplorerDriver(Capabilities capabilities) {
        return new InternetExplorerDriver(capabilities);
    }

    public WebDriver newHtmlUnitDriver(Capabilities capabilities) {
        return new HtmlUnitDriver(capabilities);
    }

    public WebDriver newPhantomDriver(Capabilities caps) {
        return new PhantomJSDriver(caps);
    }
}