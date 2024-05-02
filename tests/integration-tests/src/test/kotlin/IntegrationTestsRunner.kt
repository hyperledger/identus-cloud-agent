import io.cucumber.junit.CucumberOptions
import net.serenitybdd.cucumber.CucumberWithSerenity
import org.junit.runner.RunWith

@CucumberOptions(
    features = ["src/test/resources/features"],
    snippets = CucumberOptions.SnippetType.CAMELCASE,
    plugin = ["pretty"],
)
@RunWith(CucumberWithSerenity::class)
class IntegrationTestsRunner
