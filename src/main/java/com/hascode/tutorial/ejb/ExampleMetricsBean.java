package com.hascode.tutorial.ejb;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.reporting.JmxReporter;

@Singleton
@Startup
public class ExampleMetricsBean {
	private final Logger log = LoggerFactory.getLogger(ExampleMetricsBean.class);

	private Counter numReqSend;
	private MetricsRegistry registry;
	private JmxReporter reporter;

	@PostConstruct
	protected void onBeanConstruction() {
		registry = new MetricsRegistry();
		numReqSend = registry.newCounter(ExampleMetricsBean.class, "Number-of-Request");
		reporter = new JmxReporter(registry);
		reporter.start();
	}

	@PreDestroy
	protected void onBeanDestruction() {
		reporter.shutdown();
		registry.shutdown();
	}

	@Schedule(second = "*/30", minute = "*", hour = "*")
	public void parseBitbucketRepositories() throws MalformedURLException {
		log.info("parsing bitbucket repositories");
		URL url = new URL("https://bitbucket.org/api/2.0/repositories/hascode");
		queryBitbucket(url);
	}

	private void queryBitbucket(final URL url) {
		try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			numReqSend.inc();
			JsonObject obj = rdr.readObject();
			JsonNumber currentElements = obj.getJsonNumber("pagelen");
			JsonString nextPage = obj.getJsonString("next");
			log.info("{} elements on current page, next page is: {}", currentElements, nextPage);
			JsonArray repositories = obj.getJsonArray("values");
			for (JsonObject repository : repositories.getValuesAs(JsonObject.class)) {
				String info = String.format("Repository '{}', URL: {}", repository.getJsonString("name").getString(), "");
				log.info(info);
			}
			if (nextPage != null) {
				queryBitbucket(new URL(nextPage.getString()));
			}
		} catch (IOException e) {
			log.warn("io exception thrown", e);
		}
	}
}
