import org.aspectj.lang.annotation.After;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SampleTest {
    public WebDriver driver;
    public String username = System.getenv("BROWSERSTACK_USERNAME");
    public String accesskey = System.getenv("BROWSERSTACK_ACCESS_KEY");
    public ArrayList<String> sessionIDList;
    @Parameters({"device","browser"})
    @BeforeMethod
    public void setup(@Optional String device, @Optional String browser) throws MalformedURLException {

        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("device",device);
        caps.setCapability("browser",browser);
        caps.setCapability("build","Tradition Test Execution");
        caps.setCapability("project","Parallel runs");
        caps.setCapability("name","parallel_test");
        caps.setCapability("interactiveDebugging","true");
        caps.setCapability("browserstack.debug","true");

        driver = new RemoteWebDriver(new URL("https://"+username+":"+accesskey+"@hub-cloud.browserstack.com/wd/hub"),caps);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));
    }
    @Test
    public void test() throws InterruptedException {

        JavascriptExecutor jse = (JavascriptExecutor)driver;
        driver.get("https://bstackdemo.com/");
        driver.findElement(By.id("signin")).click();
        driver.findElement(By.cssSelector("#username input")).sendKeys("demouser", Keys.ENTER);
        driver.findElement(By.cssSelector("#password input")).sendKeys("testingisfun99", Keys.ENTER);
        driver.findElement(By.id("login-btn")).click();
        jse.executeScript("browserstack_executor: {\"action\": \"annotate\", \"arguments\": {\"data\": \"Logged In\", \"level\": \"info\"}}");

        String productName = "iPhone 12";
        List<WebElement> allProductsList = driver.findElements(By.cssSelector(".shelf-item"));
        for(int i=0;i<allProductsList.size();i++){
            WebElement selectProduct = allProductsList.get(i);
            if(selectProduct.findElement(By.cssSelector(".shelf-item__title")).getText().equals(productName)){
                selectProduct.findElement(By.cssSelector(".shelf-item__buy-btn")).click();
                System.out.println(productName + " added to bag!");
                break;
            }else{
                continue;
            }
        }
        //driver.findElement(By.cssSelector(".float-cart__close-btn")).click();
        driver.findElement(By.cssSelector(".buy-btn")).click();
        driver.findElement(By.id("firstNameInput")).sendKeys("Nithya");
        driver.findElement(By.id("lastNameInput")).sendKeys("Mani");
        driver.findElement(By.id("addressLine1Input")).sendKeys("Mumbai");
        driver.findElement(By.id("provinceInput")).sendKeys("Maharashtra");
        driver.findElement(By.id("postCodeInput")).sendKeys("400080");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        //Log values - info/warn/debug/error
        jse.executeScript("browserstack_executor: {\"action\": \"annotate\", \"arguments\": {\"data\": \"Order placed\", \"level\": \"info\"}}");

        String confirmationMessage = driver.findElement(By.id("confirmation-message")).getText();
        String orderId = driver.findElement(By.cssSelector(".checkout-form div:nth-child(2) strong")).getText();
        Assert.assertTrue(confirmationMessage.contains("successfully"));
    }
    @AfterMethod
    public void teardown(ITestResult result){
        SessionId sessionId = ((RemoteWebDriver)driver).getSessionId();
        JavascriptExecutor jse = (JavascriptExecutor)driver;
        if( result.getStatus() == ITestResult.SUCCESS) {
            jse.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"passed\", \"reason\": \""+result.getName()+" passed!\"}}");
        }
        else{
            jse.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"failed\", \"reason\": \""+result.getThrowable()+"\"}}");
        }
        driver.quit();
        //Generate PDF files for each test session post execution
        GenerateTextLogsToPDF pdf = new GenerateTextLogsToPDF();
        pdf.createPDFReport(sessionId.toString());
    }
}
