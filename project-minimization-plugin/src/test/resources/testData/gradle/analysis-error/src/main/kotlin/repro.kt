import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.AssertFactory
import org.assertj.core.api.ListAssert
import org.assertj.core.util.CheckReturnValue
import org.openqa.selenium.By
import org.openqa.selenium.WebElement

class WebElementAssert(
	element: WebElement
) : AbstractObjectAssert<WebElementAssert, WebElement>(element, WebElementAssert::class.java) {

	@CheckReturnValue
	fun descendant(locator: By): WebElementAssert =
		@Suppress("UNCHECKED_CAST")
		extracting({ it.findElement(locator) }, ::WebElementAssert as AssertFactory<WebElement, WebElementAssert>)

	@CheckReturnValue
	fun descendants(locator: By): ListAssert<WebElement> =
		extracting({ it.findElements(locator) }, ::ListAssert)
}
