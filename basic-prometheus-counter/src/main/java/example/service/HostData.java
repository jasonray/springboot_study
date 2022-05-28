package example.service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 *  @author: Serguei Kouzmine (kouzmine_serguei@yahoo.com)
 */
// read legacy metrics inventory file
public class HostData {

	private boolean debug = false;
	private String hostname = null;
	private List<String> metrics = null;
	private Map<String, String> metricExtractors = new HashMap<>();
	private Map<String, String> extractedMetricNames = new HashMap<>();

	// in the legacy application one has to process the metrics to extract the
	// therefor many columns will be hand crafted
	// numeric values and publish to Prometheus
	private Path filePath;
	private Map<String, String> data = new HashMap<>();

	private static final Logger logger = LogManager.getLogger(HostData.class);

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean value) {
		debug = value;
	}

	public List<String> getMetrics() {
		return metrics;
	}

	public void setMetrics(List<String> value) {
		metrics = value;
	}

	public void setMetricExtractors(Map<String, String> value) {
		metricExtractors = value;
	}

	public void setExtractedMetricNames(Map<String, String> value) {
		extractedMetricNames = value;
	}

	// relies on UNIX soft link making the fixed file path always point to
	// latest results
	public HostData(String hostname) {
		this.hostname = hostname;

		filePath = Paths.get(String.join(System.getProperty("file.separator"),
				Arrays.asList(dataDir, this.hostname, "data.txt")));
		// TODO : examine and bail if not a soft link
	}

	private String dataDir = String.join(System.getProperty("file.separator"),
			Arrays.asList(System.getProperty("user.dir"), "src", "test", "resources",
					"data"));

	public void setDataDir(String value) {
		dataDir = value;
	}

	public HostData() {

	}

	public Map<String, String> getData() {
		return data;
	}

	// read file fully
	public void loadData() {
		try {
			InputStream in = Files.newInputStream(filePath);
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(in));
			String key = null;
			String value = null;

			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				// collect metrics with non-blank values
				Pattern pattern = Pattern.compile("^(.*):  *(.*)$");
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					key = matcher.group(1);
					value = matcher.group(2);
					data.put(key, value);
				}
			}
			bufferedReader.close();

		} catch (FileNotFoundException e) {
			System.err.println("Exception (ignored) " + e.toString());
		} catch (IOException e) {
			System.err.println("Exception (ignored) " + e.toString());
		}
	}

	public void readData() {
		String key = null;
		String value = null;
		String line = null;
		try {
			InputStream in = Files.newInputStream(filePath);
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(in));

			while ((line = bufferedReader.readLine()) != null) {

				key = null;
				value = null;
				// collect metrics with non-blank values
				Pattern pattern = Pattern.compile( /* "(?:" to suppress capturing */
						"(" + StringUtils.join(metrics, "|") + ")" + ": " + "([^ ]*)$");
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					key = matcher.group(1);
					value = matcher.group(2);
				}
				// NOTE: "mKey" to prevent duplicate variable compiler error
				for (String mKey : metricExtractors.keySet()) {
					if (debug)
						logger.info(String.format("processing metric extractor: %s %s",
								mKey, metricExtractors.get(mKey)));
					pattern = Pattern.compile(
							"^\\s*(?:" + mKey + ")" + ": " + metricExtractors.get(mKey));
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						key = mKey;
						value = matcher.group(1);
						if (debug)
							logger.info(
									String.format("Found data for metric %s: %s", key, value));
					}
					// NOTE: hack
					if (value != null) {
						String realKey = extractedMetricNames != null
								&& extractedMetricNames.containsKey(key)
										? extractedMetricNames.get(key) : key;
						if (debug)
							logger.info(String.format("Adding data for metric %s(%s): %s",
									key, realKey, value));
						data.put(realKey, value);
					}
				}
			}
			bufferedReader.close();

		} catch (FileNotFoundException e) {
			System.err.println("Exception (ignored) " + e.toString());
		} catch (IOException e) {
			System.err.println("Exception (ignored) " + e.toString());
		}
	}
}
