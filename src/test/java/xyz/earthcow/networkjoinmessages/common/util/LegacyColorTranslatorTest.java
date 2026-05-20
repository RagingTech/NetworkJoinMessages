package xyz.earthcow.networkjoinmessages.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LegacyColorTranslator}.
 * This class is pure logic with no dependencies, so no mocking is required.
 */
class LegacyColorTranslatorTest {

    // -----------------------------------------------------------------------
    // translate() -- basic passthrough
    // -----------------------------------------------------------------------

    @Test
    void translate_plainTextIsUnchanged() {
        assertEquals("Hello world", LegacyColorTranslator.translate("Hello world"));
    }

    @Test
    void translate_emptyStringIsUnchanged() {
        assertEquals("", LegacyColorTranslator.translate(""));
    }

    // -----------------------------------------------------------------------
    // translate() -- section-sign to ampersand normalisation
    // -----------------------------------------------------------------------

    @Test
    void translate_sectionSignIsConvertedToAmpersand() {
        // §c should resolve to the same MiniMessage tag as &c
        String viaSectionSign = LegacyColorTranslator.translate("§cRed");
        String viaAmpersand   = LegacyColorTranslator.translate("&cRed");
        assertEquals(viaAmpersand, viaSectionSign,
                "Section-sign and ampersand codes must produce identical output");
    }

    // -----------------------------------------------------------------------
    // translate() -- named color codes -> MiniMessage hex tags
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "code {0} -> MiniMessage color tag")
    @ValueSource(strings = {"&0","&1","&2","&3","&4","&5","&6","&7",
                            "&8","&9","&a","&b","&c","&d","&e","&f"})
    void translate_namedColorCodesProduceMiniMessageColorTags(String code) {
        String result = LegacyColorTranslator.translate(code + "Text");
        assertTrue(result.startsWith("<#"),
                "Named color code " + code + " should be converted to a hex MiniMessage tag, got: " + result);
        assertTrue(result.endsWith(">Text"),
                "Translated output should preserve trailing text, got: " + result);
    }

    // -----------------------------------------------------------------------
    // translate() -- formatting codes
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "formatting code {0} -> correct MiniMessage tag")
    @CsvSource({
        "&k, <obfuscated>",
        "&l, <bold>",
        "&m, <strikethrough>",
        "&n, <underlined>",
        "&o, <italic>",
        "&r, <reset>"
    })
    void translate_formattingCodesProduceCorrectTags(String code, String expectedTag) {
        String result = LegacyColorTranslator.translate(code);
        assertTrue(result.contains(expectedTag),
                "Code " + code + " should produce " + expectedTag + " but got: " + result);
    }

    // -----------------------------------------------------------------------
    // translate() -- inline hex shorthand (&#RRGGBB)
    // -----------------------------------------------------------------------

    @Test
    void translate_inlineHexShorthandIsConverted() {
        String result = LegacyColorTranslator.translate("&#FF0000Red");
        assertEquals("<#FF0000>Red", result);
    }

    @Test
    void translate_inlineHexShorthandLowercaseIsConverted() {
        String result = LegacyColorTranslator.translate("&#ff0000Red");
        assertEquals("<#ff0000>Red", result);
    }

    @Test
    void translate_multipleInlineHexCodesInOneString() {
        String result = LegacyColorTranslator.translate("&#FF0000Red &#00FF00Green");
        assertEquals("<#FF0000>Red <#00FF00>Green", result);
    }

    @Test
    void translate_inlineHexShorthandDoesNotMatchFiveDigits() {
        // &#FFFFF should not be converted -- only exactly six hex digits
        String input = "&#FFFFFShouldBeIgnored";
        String result = LegacyColorTranslator.translate(input);
        // The six-digit match is "FFFFF" + next char, so check the regex does not greedily eat it
        // The important thing: no <# tag should appear for a five-digit sequence
        assertFalse(result.matches(".*<#[0-9a-fA-F]{5}>.*"),
                "Five-digit hex should not be converted: " + result);
    }

    // -----------------------------------------------------------------------
    // translate() -- Essentials hex (§x§r§r§g§g§b§b)
    // -----------------------------------------------------------------------

    @Test
    void translate_essentialsHexIsConverted() {
        // §x§f§b§6§3§f§5 => &#fb63f5 => <#fb63f5>
        String result = LegacyColorTranslator.translate("§x§f§b§6§3§f§5Hello!");
        assertEquals("<#fb63f5>Hello!", result);
    }

    @Test
    void translate_essentialsHexUppercaseDigitsAreHandled() {
        // §x§F§F§0§0§0§0 => &#FF0000 => <#FF0000>
        String result = LegacyColorTranslator.translate("§x§F§F§0§0§0§0Hi");
        assertEquals("<#FF0000>Hi", result);
    }

    @Test
    void translate_essentialsHexMixedWithRegularText() {
        String result = LegacyColorTranslator.translate("Before §x§f§f§0§0§0§0Red After");
        assertEquals("Before <#ff0000>Red After", result);
    }

    // -----------------------------------------------------------------------
    // translate() -- literal newline placeholder
    // -----------------------------------------------------------------------

    @Test
    void translate_literalBackslashNIsConvertedToNewlineTag() {
        // The static map replaces the two-char sequence "\n" (backslash + n)
        String result = LegacyColorTranslator.translate("Line1\\nLine2");
        assertTrue(result.contains("<newline>"),
                "Literal '\\n' in config strings should become <newline> tag, got: " + result);
    }

    // -----------------------------------------------------------------------
    // translate() -- compound strings
    // -----------------------------------------------------------------------

    @Test
    void translate_compoundStringWithMultipleCodes() {
        String result = LegacyColorTranslator.translate("&cRed &aGreen &bAqua &#FF0000Hex");
        // Must not contain any raw & codes after translation
        assertFalse(result.contains("&c"), "Raw &c should have been translated");
        assertFalse(result.contains("&a"), "Raw &a should have been translated");
        assertFalse(result.contains("&b"), "Raw &b should have been translated");
        assertFalse(result.contains("&#FF0000"), "Raw &#FF0000 should have been translated");
    }

    @Test
    void translate_allCodeTypesInSingleStringDoesNotThrow() {
        assertDoesNotThrow(() ->
            LegacyColorTranslator.translate(
                "&cRed &aGreen &bAqua &#FF0000Hex §x§f§f§0§0§0§0EssHex &lBold &r"
            )
        );
    }

    // -----------------------------------------------------------------------
    // ESSENTIALS_HEX_PATTERN (public field)
    // -----------------------------------------------------------------------

    @Test
    void essentialsHexPattern_matchesValidEssentialsCode() {
        Pattern p = LegacyColorTranslator.ESSENTIALS_HEX_PATTERN;
        assertTrue(p.matcher("§x§f§b§6§3§f§5").find());
    }

    @Test
    void essentialsHexPattern_doesNotMatchPlainText() {
        assertFalse(LegacyColorTranslator.ESSENTIALS_HEX_PATTERN.matcher("Regular text").find());
    }

    @Test
    void essentialsHexPattern_doesNotMatchIncompleteCode() {
        // Only five digit segments instead of six
        assertFalse(LegacyColorTranslator.ESSENTIALS_HEX_PATTERN.matcher("§x§f§b§6§3§f").find());
    }

    @Test
    void essentialsHexPattern_matchesUppercaseDigits() {
        assertTrue(LegacyColorTranslator.ESSENTIALS_HEX_PATTERN.matcher("§x§A§B§C§D§E§F").find());
    }
}
