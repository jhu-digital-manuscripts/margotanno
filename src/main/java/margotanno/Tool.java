package margotanno;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.json.JSONWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

/**
 * Read spreadsheet with annotations to screen scrape. Write out JSON-LD Web Annotations.
 * Web pages are expected to be in particular format Margot used to display annotations.
 * 
 * Input format is CSV spreadsheet of form:
 * <pre>
 * IIIF Viewer URL,Margot URL 
 * "https://rosetest.library.jhu.edu/dlmm/#rose/Francais12595/033v/image", "http://example.com/margotanno.html"
 * </pre>
 */

public class Tool {
	/**
	 * @param input
	 * @return Margot web page URL -> rosa id of target
	 */
	protected static Map<String,String> parseInput(Reader input) {
		HashMap<String, String> result = new HashMap<String, String>();
	
		try (Stream<String> stream = new BufferedReader(input).lines()) {
		    stream.skip(1).forEach(line -> {
		    	String[] parts = parseCSVLine(line);
		    	
		    	if (parts.length != 2) {
		    		throw new RuntimeException("Malformed line: " + line);
		    	}
		    	
		    	String margoturl = parts[1].trim();

		    	if (!parts[0].isEmpty()) {
		    		// Bad hack to figure out archive id
			    	
			    	String start_marker = "/#rose/";
			    	String end_marker = "/image";
			    	int start = parts[0].indexOf(start_marker);
			    	start += start_marker.length();
			    	int end = parts[0].indexOf(end_marker);
			    	
			    	String rosa_id = parts[0].substring(start, end).replace('/', '.');
			    	rosa_id += ".tif";
			    	
			    	result.put(margoturl, rosa_id);
		    	}
		    });
		}
		
		return result;
	}
	
	// Bad hack
	private static String[] parseCSVLine(String line) {
		return line.replace("\"", "").trim().split(",");
	}
	
	
	/**
	 * The annotation body is in a div with class content_node.
	 * Grab it and then do some cleanup.
	 * 
	 * @return HTML fragment containing annotation
	 */
	protected static String getAnnotationBody(Document doc) {
		Element content = doc.selectFirst(".content_node");
		
		// Remove paragraph linking to French margot site
		System.err.println("MOOO!");
		content.select("em").forEach(el -> {
			System.err.println(el);
			
			if (el.ownText().contains("Lire en")) {
				el.remove();
			}
		});
		
		// Remove image tags that point into margot site
		content.select("img").forEach(img -> {
			if (img.attr("src").startsWith("https://uwaterloo.ca/margot/")) {
				img.remove();
			}
		});
		
		// Try to collapse the odd <span><span>blah</span></span>
		content.select("span span").forEach(el -> el.unwrap());

		// Remove empty divs and spans
		content.select("div:empty").forEach(el -> el.remove());
		content.select("span:empty").forEach(el -> el.remove());

		// Clean the annotation by removing unwanted elements and attributes
		return Jsoup.clean(content.toString(), Whitelist.relaxed());
	}
	
	protected static JSONObject createWebAnnotation(String web_url, String html_body, String rosa_id, int seq) {
		JSONObject result = new JSONObject();
		
		result.put("@context", "http://www.w3.org/ns/anno.jsonld");
		result.put("id", "rosa:margot_annos.jsonld#" + seq);
		result.put("type", "Annotation");
		result.put("via", web_url);
		
		JSONObject body = new JSONObject();
		
		body.put("type", "TextualBody");
		body.put("format", "text/html");
		body.put("value", html_body);
		
		result.put("body", body);
		result.put("target", "rosa:" + rosa_id);
		
		return result;
	}
	
	/**
	 * Output array of JSON-LD Web Annotations given spreadsheet of margot websites to scrape.
	 * 
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		JSONWriter output = new JSONWriter(System.out);
		output.array();

		try (Reader input = new InputStreamReader(System.in, StandardCharsets.UTF_8)) {
			int seq = 0;	
			Map<String, String> data = parseInput(input);
			
			System.err.println(data.size());
			System.exit(1);
			
			for (String url: data.keySet()) {
				String rosa_id = data.get(url);
				
				try {
					System.err.println("GRRR! " + seq);
					System.err.println(url);
					String body = getAnnotationBody(Jsoup.connect(url).get());
					JSONObject anno = createWebAnnotation(url, body, rosa_id, seq++); 
					System.err.println(anno);
					
					output.value(anno);
				} catch (IOException e) {
					throw new RuntimeException("Failed on: " + url, e);
				}
				
				Thread.sleep(1000);
			}
		}
		
		output.endArray();
		System.out.flush();
	}
}
