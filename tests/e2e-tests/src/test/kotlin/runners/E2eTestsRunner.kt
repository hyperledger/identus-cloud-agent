package runners

import io.cucumber.junit.CucumberOptions
import net.serenitybdd.cucumber.CucumberWithSerenity
import org.junit.runner.RunWith

@RunWith(CucumberWithSerenity::class)
@CucumberOptions(
    features = [
        "src/test/resources/features"
    ],
    glue = ["features"],
    snippets = CucumberOptions.SnippetType.CAMELCASE,
    plugin = [
        "pretty",
        "json:target/serenity-reports/cucumber_report.json"
    ],
)
class E2eTestsRunner
