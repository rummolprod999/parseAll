package parser.extensions

import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement

fun <T> T.findElementWithoutException(by: By): WebElement?
        where T : SearchContext {
    return try {
        this.findElement(by)
    } catch (e: Exception) {
        null
    }
}