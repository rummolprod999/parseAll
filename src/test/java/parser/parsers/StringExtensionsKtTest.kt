package parser.extensions

import org.junit.Test
import kotlin.test.assertEquals

class StringExtensionsKtTest {

    /**
     * This class contains tests for the `getDataFromRegexp` function.
     *
     * The `getDataFromRegexp` function extracts and returns the first capturing group
     * based on the provided regular expression from a string. If no capturing group is found,
     * an empty string is returned.
     */

    @Test
    fun testGetDataFromRegexp_whenMatchExists_returnsGroup() {
        val input = "My email is test@example.com"
        val regex = "email is (\\S+@\\S+\\.\\S+)"
        val result = input.getDataFromRegexp(regex)
        assertEquals("test@example.com", result)
    }

    @Test
    fun testGetDataFromRegexp_whenNoMatch_returnsEmptyString() {
        val input = "No email here!"
        val regex = "email is (\\S+@\\S+\\.\\S+)"
        val result = input.getDataFromRegexp(regex)
        assertEquals("", result)
    }

    @Test
    fun testGetDataFromRegexp_whenEmptyInput_returnsEmptyString() {
        val input = ""
        val regex = "(\\d+)"
        val result = input.getDataFromRegexp(regex)
        assertEquals("", result)
    }

    @Test
    fun testGetDataFromRegexp_whenRegexHasNoGroup_returnsEmptyString() {
        val input = "Price is 100"
        val regex = "\\d+" // No capturing group
        val result = input.getDataFromRegexp(regex)
        assertEquals("", result)
    }

    @Test
    fun testGetDataFromRegexp_whenMultipleMatches_returnsFirstGroup() {
        val input = "Order #1234, Product #5678"
        val regex = "#(\\d+)"
        val result = input.getDataFromRegexp(regex)
        assertEquals("1234", result)
    }

    @Test
    fun testGetDataFromRegexp_whenSpecialCharactersInRegex_returnsGroup() {
        val input = "Path: /home/user/documents"
        val regex = "Path: (/.*)"
        val result = input.getDataFromRegexp(regex)
        assertEquals("/home/user/documents", result)
    }

    @Test
    fun testGetDataFromRegexp_whenWhitespaceInInput_returnsTrimmedResult() {
        val input = "    Match this value    "
        val regex = "Match (\\w+)"
        val result = input.getDataFromRegexp(regex)
        assertEquals("this", result)
    }

    @Test
    fun testGetDataFromRegexp_whenInputIsNull_returnsEmptyString() {
        val input: String? = null
        val regex = "(.*)"
        val result = input.orEmpty().getDataFromRegexp(regex)
        assertEquals("", result)
    }

    @Test
    fun testGetDataFromRegexp_whenEscapedCharactersHandled_returnsGroup() {
        val input = "File path is C:\\User\\Docs\\file.txt"
        val regex = "C:(\\\\.*)"
        val result = input.getDataFromRegexp(regex)
        assertEquals("\\User\\Docs\\file.txt", result)
    }

    @Test
    fun testGetDataFromRegexp_whenInvalidRegex_returnsEmptyString() {
        val input = "Invalid regex test"
        val regex = "\\K[" // Invalid regex
        val result = input.getDataFromRegexp(regex)
        assertEquals("", result)
    }

    @Test
    fun testDeleteDoubleWhiteSpace_whenSingleSpaces_remainsUntouched() {
        val input = "This is a test"
        val result = input.deleteDoubleWhiteSpace()
        assertEquals("This is a test", result)
    }

    @Test
    fun testDeleteDoubleWhiteSpace_whenDoubleSpaces_removedSuccessfully() {
        val input = "This  is  a  test"
        val result = input.deleteDoubleWhiteSpace()
        assertEquals("This is a test", result)
    }

    @Test
    fun testDeleteDoubleWhiteSpace_whenLeadingTrailingSpaces_removedSuccessfully() {
        val input = "   This is a test   "
        val result = input.deleteDoubleWhiteSpace()
        assertEquals("This is a test", result)
    }

    @Test
    fun testDeleteDoubleWhiteSpace_whenTabsAndNewLines_normalizedToSingleSpace() {
        val input = "This\tis\na\t\ntest"
        val result = input.deleteDoubleWhiteSpace()
        assertEquals("This is a test", result)
    }

    @Test
    fun testDeleteDoubleWhiteSpace_whenComplexWhitespace_normalizedSuccessfully() {
        val input = "  This\tis  a\n test \twith   random \n\t\rspaces  "
        val result = input.deleteDoubleWhiteSpace()
        assertEquals("This is a test with random spaces", result)
    }
}