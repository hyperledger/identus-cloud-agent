package runners

import io.cucumber.junit.CucumberOptions
import net.serenitybdd.cucumber.CucumberWithSerenity
import org.junit.runner.RunWith

@CucumberOptions(
    features = [
        "src/test/resources/features/did_registrar",
//        "src/test/resources/features/issue_credentials",
//        "src/test/resources/features/present_proof",
//        "src/test/resources/features/system",
    ],
    glue = ["features"],
    snippets = CucumberOptions.SnippetType.CAMELCASE,
    plugin = [
        "pretty",
        "json:target/serenity-reports/cucumber_report.json",
    ],
)
@RunWith(CucumberWithSerenity::class)
class E2eTestsRunner
