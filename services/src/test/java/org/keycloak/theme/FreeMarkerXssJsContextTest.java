/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.theme;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * PoC: Demonstrates that {@code ?no_esc} inside a {@code <script>} block in an
 * FTL template configured with {@code HTMLOutputFormat} does NOT protect the
 * JavaScript string context.
 *
 * <p>Background — FreeMarker's {@code HTMLOutputFormat} auto-escapes interpolations
 * for an HTML *text/attribute* context ({@code "} → {@code &quot;},
 * {@code <} → {@code &lt;}, etc.).  However, browsers do <b>not</b> decode HTML
 * entities inside {@code <script>} blocks; they treat the content as raw
 * JavaScript source.  This means:
 *
 * <ul>
 *   <li>With auto-escaping ON (no {@code ?no_esc}): a {@code "} in the value
 *       becomes {@code &quot;} in the output, which is <em>literal text</em>
 *       inside the script — it produces broken JS, not safe JS.</li>
 *   <li>With {@code ?no_esc}: the raw value passes through unchanged — a
 *       {@code "} in the value breaks out of the JS string literal.</li>
 * </ul>
 *
 * <p>The correct approach (already used in {@code keycloak.v2/login/template.ftl})
 * is {@code <#outputformat "JavaScript">} combined with the {@code ?c} built-in,
 * which emits a properly-quoted JSON string literal.
 *
 * <p>This PoC mirrors the pattern from
 * {@code themes/src/main/resources/theme/base/login/template.ftl} lines 45-51
 * and 76-82.
 *
 * @see DefaultFreeMarkerProvider#getTemplate – sets HTMLOutputFormat for .ftl
 */
public class FreeMarkerXssJsContextTest {

    // ---------------------------------------------------------------
    //  Vulnerable pattern (base/login/template.ftl line 49)
    //
    //    startSessionPolling(
    //        "${url.ssoLoginInOtherTabsUrl?no_esc}"
    //    );
    // ---------------------------------------------------------------

    private static final String VULNERABLE_TEMPLATE =
            "<script type=\"module\">\n"
          + "startSessionPolling(\n"
          + "    \"${url?no_esc}\"\n"
          + ");\n"
          + "</script>";

    // ---------------------------------------------------------------
    //  Safe pattern (keycloak.v2/login/template.ftl lines 90-96)
    //
    //    <#outputformat "JavaScript">
    //    startSessionPolling(
    //        ${url.ssoLoginInOtherTabsUrl?c}
    //    );
    //    </#outputformat>
    // ---------------------------------------------------------------

    private static final String SAFE_TEMPLATE =
            "<script type=\"module\">\n"
          + "<#outputformat \"JavaScript\">\n"
          + "startSessionPolling(\n"
          + "    ${url?c}\n"
          + ");\n"
          + "</#outputformat>\n"
          + "</script>";

    // ---------------------------------------------------------------
    //  Default auto-escape (no ?no_esc) — also not correct for JS
    //  context, but included to show it produces broken JS rather
    //  than executable XSS.
    // ---------------------------------------------------------------

    private static final String AUTOESC_TEMPLATE =
            "<script type=\"module\">\n"
          + "startSessionPolling(\n"
          + "    \"${url}\"\n"
          + ");\n"
          + "</script>";

    // A benign URL (typical Keycloak output)
    private static final String BENIGN_URL =
            "http://localhost:8080/realms/master/login-actions/restartSession?skip_logout=true";

    // Payload: double-quote breaks out of the JS string, injecting arbitrary JS
    private static final String PAYLOAD_DQUOTE =
            "http://localhost:8080/\");alert(document.domain);//";

    // Payload: </script> breaks out of the script block entirely
    private static final String PAYLOAD_SCRIPT_BREAK =
            "</script><script>alert(document.domain)</script><script>";

    /**
     * VULNERABLE: {@code ?no_esc} with a benign URL works fine, hiding the bug.
     */
    @Test
    public void vulnerablePattern_benignUrl_looksCorrect() throws Exception {
        String output = render(VULNERABLE_TEMPLATE, BENIGN_URL);

        assertTrue("Benign URL should appear verbatim",
                output.contains("\"" + BENIGN_URL + "\""));
    }

    /**
     * VULNERABLE: {@code ?no_esc} with a double-quote payload breaks the JS string.
     *
     * <p>Rendered output (inside {@code <script>}):
     * <pre>
     * startSessionPolling(
     *     "http://localhost:8080/");alert(document.domain);//"
     * );
     * </pre>
     *
     * The browser parses this as three JS statements:
     * <ol>
     *   <li>{@code startSessionPolling("http://localhost:8080/")}</li>
     *   <li>{@code alert(document.domain)}</li>
     *   <li>{@code //"  );  } — commented out</li>
     * </ol>
     */
    @Test
    public void vulnerablePattern_dquotePayload_allowsJsInjection() throws Exception {
        String output = render(VULNERABLE_TEMPLATE, PAYLOAD_DQUOTE);

        assertTrue("Payload should inject alert() as executable JS",
                output.contains("\");alert(document.domain);//"));
    }

    /**
     * VULNERABLE: {@code ?no_esc} with a {@code </script>} payload breaks out
     * of the entire script block.
     *
     * <p>Rendered output:
     * <pre>
     * &lt;script type="module"&gt;
     * startSessionPolling(
     *     "&lt;/script&gt;&lt;script&gt;alert(document.domain)&lt;/script&gt;&lt;script&gt;"
     * );
     * &lt;/script&gt;
     * </pre>
     *
     * The browser's HTML parser splits this into multiple script blocks,
     * executing the injected {@code alert(document.domain)}.
     */
    @Test
    public void vulnerablePattern_scriptBreakPayload_escapesScriptBlock() throws Exception {
        String output = render(VULNERABLE_TEMPLATE, PAYLOAD_SCRIPT_BREAK);

        assertTrue("Payload should contain raw </script> tag",
                output.contains("</script><script>alert(document.domain)</script>"));
    }

    /**
     * DEFAULT AUTO-ESCAPE: produces broken JS (not exploitable XSS, but
     * not correct either).  The {@code "} becomes {@code &quot;}, which is
     * literal text inside a script block — the browser sees a broken string.
     */
    @Test
    public void autoEscapePattern_dquotePayload_producesBrokenJs() throws Exception {
        String output = render(AUTOESC_TEMPLATE, PAYLOAD_DQUOTE);

        assertFalse("Auto-escape should encode the double-quote as &quot;",
                output.contains("\");alert("));
        assertTrue("Should contain HTML-encoded quote (broken JS, but not exploitable)",
                output.contains("&quot;"));
    }

    /**
     * SAFE: {@code <#outputformat "JavaScript">} + {@code ?c} emits the value
     * as a properly-quoted JSON string literal.  Double-quotes inside the
     * value are backslash-escaped, so the payload stays trapped inside the
     * string and never becomes executable code.
     *
     * <p>The rendered output contains the payload text, but with the critical
     * {@code "} backslash-escaped.  Compare:
     * <ul>
     *   <li>Vulnerable: {@code "http://localhost:8080/");alert(document.domain);//"}
     *       — the unescaped {@code "} terminates the string</li>
     *   <li>Safe: {@code "http://localhost:8080/\");alert(document.domain);//"}
     *       — the {@code \"} is an escaped character <em>inside</em> the string</li>
     * </ul>
     */
    @Test
    public void safePattern_dquotePayload_properlyEscaped() throws Exception {
        String output = render(SAFE_TEMPLATE, PAYLOAD_DQUOTE);

        // The ?c built-in wraps the value in quotes and backslash-escapes
        // internal quotes.  The raw sequence /" must appear as /\" in output
        // (the quote is escaped, keeping the payload inside the JS string).
        assertTrue("Safe pattern should backslash-escape the double-quote",
                output.contains("/\\\")"));

        // Verify the full payload is contained as inert string data, not
        // as separate JS statements.  The entire value from opening quote
        // to closing quote must be on one unbroken JS string.
        assertFalse("The payload must not appear as bare JS outside the string "
                + "(i.e., there must not be an unescaped \" before );alert)",
                output.contains("/\");alert"));
    }

    /**
     * SAFE: {@code </script>} inside the value is escaped by the JavaScript
     * output format — the {@code <} becomes {@code \\u003C} or is otherwise
     * neutralised so it cannot break out of the script block.
     */
    @Test
    public void safePattern_scriptBreakPayload_neutralised() throws Exception {
        String output = render(SAFE_TEMPLATE, PAYLOAD_SCRIPT_BREAK);

        assertFalse("Safe pattern must not contain raw </script> tag",
                output.contains("</script><script>alert"));
    }

    /**
     * SAFE: benign URL renders correctly with the safe pattern.
     */
    @Test
    public void safePattern_benignUrl_rendersCorrectly() throws Exception {
        String output = render(SAFE_TEMPLATE, BENIGN_URL);

        assertTrue("Benign URL should appear as a quoted JS string",
                output.contains("\"" + BENIGN_URL + "\""));
    }

    // ---------------------------------------------------------------
    //  Helper: renders a FreeMarker template with HTMLOutputFormat
    //  (same config as DefaultFreeMarkerProvider) and a single "url"
    //  variable.
    // ---------------------------------------------------------------

    private String render(String templateSource, String urlValue)
            throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);

        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("test.ftl", templateSource);
        cfg.setTemplateLoader(loader);

        Template template = cfg.getTemplate("test.ftl");

        Map<String, Object> model = new HashMap<>();
        model.put("url", urlValue);

        StringWriter out = new StringWriter();
        template.process(model, out);
        return out.toString();
    }
}
