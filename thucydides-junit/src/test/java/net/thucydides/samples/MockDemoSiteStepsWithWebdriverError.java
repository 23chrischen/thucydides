package net.thucydides.samples;

import net.thucydides.core.annotations.Step;
import org.openqa.selenium.NoSuchElementException;

public class MockDemoSiteStepsWithWebdriverError {

	@Step
    public void enter_values(String selectValue, boolean checkboxValue) {
    }

    @Step
    public void fields_should_be_displayed() {
    }

    @Step
    public void should_display(String selectValue) {
    }
    
    @Step
    public void should_have_selected_value(String selectValue) {
        throw new NoSuchElementException("This is not the element you're looking for");
    }

    @Step
    public void should_not_have_selected_value(String selectValue) {
    }

    @Step
    public void should_have_no_selected_value() {
    }

    @Step
    public void do_something() {}

    @Step
    public void do_something_else() {}
}