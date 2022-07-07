package example.service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Gauge.Builder;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.common.TextFormat;

import example.dao.JDBCDao;
import example.entity.Host;
import example.entity.Result;
import example.projection.ServerInstanceApplication;
import example.utils.ClusterConfigReader;

import example.service.BaseService;
import example.service.HostData;

/*
 *  @author: Serguei Kouzmine (kouzmine_serguei@yahoo.com)
 */

@Service
public class NodeExporter {
	private static final Logger logger = LogManager.getLogger(NodeExporter.class);

	private ClusterConfigReader clusterConfigReader = new ClusterConfigReader();

	@Autowired
	private JDBCDao dao;

	// custom metric setting the instance
	// https://prometheus.github.io/client_java/io/prometheus/client/Gauge.html
	private static final boolean debug = false;

	@Resource
	private BaseService service;

	String fileName = "cluster.yaml";
	Map<String, Host> hostInfo = new HashMap<>();

	private static Map<String, Gauge> gauges = new HashMap<>();

	private CollectorRegistry registry;
	private Gauge example = null;

	private HostData hostData = null;

	private Map<String, String> data = new HashMap<>();

	// https://stackoverflow.com/questions/26275736/how-to-pass-a-mapstring-string-with-application-properties
	@Value("#{${example.metricExtractors}}")
	private Map<String, String> metricExtractors;

	// https://stackoverflow.com/questions/6212898/spring-properties-file-get-element-as-an-array
	@Value("${example.labelNames}")
	private String[] labelNames;

	@Value("#{'${example.metricNames}'.split(',')}")
	private String[] metricNames;

	@Value("#{${example.extractedMetricNames}}")
	private Map<String, String> extractedMetricNames;

	private static Random random = new Random();
	private static float value = 42;
	private static final int length = 10;

	private void createGauge(String counterName) {
		// cache the gauge objects
		if (gauges.containsKey(counterName))
			return;
		// NOTE: check potential name collisions before register
		Enumeration<MetricFamilySamples> metricFamilySamplesEnumeration = registry
				.metricFamilySamples();
		List<String> definedMetricNames = new ArrayList<>();
		while (metricFamilySamplesEnumeration.hasMoreElements()) {
			MetricFamilySamples metricFamilySamples = metricFamilySamplesEnumeration
					.nextElement();
			// NOTE: getNames() was not available in 0.11.0 or earlier releases
			String[] names = metricFamilySamples.getNames();
			definedMetricNames.addAll(Arrays.asList(names));
		}

		if (debug)
			logger.info("Defined metric family sample names:" + definedMetricNames);

		try {
			if (!definedMetricNames.contains(counterName)) {
				Builder builder = Gauge
						.build(counterName, "Value of metric from instance")
						.labelNames(new String[] { "instance", "datacenter", "application",
								"linborg_instance" });
				example = builder.register(registry);
				gauges.put(counterName, example);
			}
		} catch (Exception e) {
			logger.error("skipping metric update - exception: " + e.getMessage());
			// e.printStackTrace();
		}
	}

	private void exampleGauge(String counterName, ServerInstanceApplication host,
			float value) {

		String hostname = host.getServerName();
		String datacenter = "dummy";
		String application = host.getApplicationName();
		String linborgInstance = host.getInstanceName();
		Gauge gauge = gauges.get(counterName);
		// invoke Prometheus variadic methods with a argument array set at
		// compile-time
		// can also pass the arguments explicitly as in
		// gauge.labels(hostname,datacenter,appid,environment).set(value);
		// String[] labelArgs = new String[] {};
		// java.lang.ArrayIndexOutOfBoundsException
		String[] labelArgs = new String[4];
		labelArgs[0] = hostname;
		labelArgs[1] = datacenter;
		labelArgs[2] = application;
		labelArgs[3] = linborgInstance;

		// https://stackoverflow.com/questions/12320429/java-how-to-check-the-type-of-an-arraylist-as-a-whole
		/*
		int index = 0;
		String element = "memory";
		if (metricNames instanceof ArrayList<?>)
			((ArrayList<String>) metricNames).set(index, element);
		*/

		// https://prometheus.github.io/client_java/io/prometheus/client/Gauge.Child.html
		// see also:
		// https://prometheus.github.io/client_java/io/prometheus/client/Info.Child.html
		// sets or gets the info
		// https://prometheus.io/docs/concepts/metric_types/
		// NOTE: creates untyped metrics with caller-specified timestamp:
		// example{appid="BAR",env="UAT",host="lenovo120S",operation="write"} 100
		// 1656541996000
		// unclear how to do it through prometheus.io classes
		gauge.labels(labelArgs).set(value);
		// NOTE: the method named "setToTime" name is misleading
		// Executes runnable code and records its run duration

		// NOTE: the "setToCurrentTime" method operates the gauge value,
		// - not the timestamp of the gauge
		// gauge.setToCurrentTime();
		// https://github.com/prometheus/client_java/blob/master/simpleclient/src/main/java/io/prometheus/client/Gauge.java#L175
		// Set to current unixtime
		// througn "set" method

		if (debug)
			logger.info(String.format("Adding custom metrics %s %s %s %s %s: ",
					counterName, labelArgs) + gauge.labels(labelArgs).get());

	}

	public String metrics() {
		if (debug)
			logger.info("Starting reporting metrics");
		Writer writer = new StringWriter();
		try {
			registry = CollectorRegistry.defaultRegistry;

			clusterConfigReader.read(fileName);
			hostInfo = clusterConfigReader.getInfo();

			List<?> payload = dao.findAllServerInstanceApplication();
			for (Object row : payload) {
				ServerInstanceApplication serverInstance = (ServerInstanceApplication) row;
				String hostname = serverInstance.getServerName();
				hostData = new HostData(hostname);
				hostData.setMetrics(Arrays.asList(metricNames));
				hostData.setExtractedMetricNames(extractedMetricNames);
				hostData.setMetricExtractors(metricExtractors);
				hostData.readData();
				data = hostData.getData();
				if (data != null && !data.isEmpty()) {
					if (debug)
						logger.info(String.format("Loading %d metrics for host : %s",
								data.keySet().size(), hostname));

					for (String metricName : data.keySet()) {
						createGauge(metricName);
						// generate random metrics
						value = Float
								.parseFloat(data.get(metricName) + random.nextInt((int) 42));

						exampleGauge(metricName, serverInstance, value);
					}
				} else {
					if (debug)
						logger.info(
								String.format("no metrics found for %s, skipping", hostname));
				}
			}
			// https://prometheus.github.io/client_java/io/prometheus/client/exporter/common/TextFormat.html
			TextFormat.write004(writer, registry.metricFamilySamples());
		} catch (

		IOException e) {
			logger.error("Exception (caught):" + e.toString());
			return null;
		}
		return writer.toString();
	}
}
