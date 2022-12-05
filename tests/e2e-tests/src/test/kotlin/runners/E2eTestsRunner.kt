package runners

import io.cucumber.junit.CucumberOptions
import net.serenitybdd.cucumber.CucumberWithSerenity
import org.junit.runner.RunWith

@CucumberOptions(
    plugin = [
        "pretty",
        "json:target/serenity-reports/cucumber_report.json",
        "html:target/serenity-reports/cucumber_report.html"
    ],
    features = [
        "src/test/resources/features"
    ],
    glue = ["features"],
    snippets = CucumberOptions.SnippetType.CAMELCASE
)
@RunWith(CucumberWithSerenity::class)
class E2eTestsRunner
