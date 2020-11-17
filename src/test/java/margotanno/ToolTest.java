package margotanno;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.junit.Test;

public class ToolTest {
	private static String MARGOT_ANNO_TEST ="/margot_anno.html";
	
	@Test
	public void testParseInput() {
		String input = " * IIIF Viewer URL,Margot URL \n" + ",http://example.com/moo.html\n"
				+ "\"https://rosetest.library.jhu.edu/dlmm/#rose/Francais12595/033v/image\", \"http://example.com/anno.html\"";

		Map<String, String> data = Tool.parseInput(new StringReader(input));

		assertEquals(1, data.size());
		assertTrue(data.containsKey("http://example.com/anno.html"));
		assertEquals("Francais12595.033v.tif", data.get("http://example.com/anno.html"));
	}
	
	@Test
	public void testGetAnnotationBody() throws IOException {
		try (InputStream is = this.getClass().getResourceAsStream(MARGOT_ANNO_TEST)) {
			String body = Tool.getAnnotationBody(Jsoup.parse(is, "UTF-8", "http://example.com/"));
			
			assertNotNull(body);
			assertFalse(body.contains("class="));
			assertFalse(body.contains("content_node"));
			assertFalse(body.contains("field-item"));
			assertFalse(body.contains("Lire en"));
		}
	}
	
	@Test
	public void testCreateWebAnnotation() {
		String web_url = "http://example.com/blah.html";
		String html_body = "<p>Some html.</p>";
		String rosa_id = "Douce195.001r.tif";
		int seq = 32;
		
		JSONObject anno = Tool.createWebAnnotation(web_url, html_body, rosa_id, seq);
		
		assertNotNull(anno);
		assertEquals(anno.getString("@context"), "http://www.w3.org/ns/anno.jsonld");
		assertEquals(anno.getString("type"), "Annotation");
		assertTrue(anno.getString("id").contains("" + seq));
		assertEquals(anno.getJSONObject("body").getString("format"), "text/html");
		assertEquals(anno.getJSONObject("body").getString("value"), html_body);
		assertEquals(anno.getString("target"), "rosa:" + rosa_id);
		
	}
}
