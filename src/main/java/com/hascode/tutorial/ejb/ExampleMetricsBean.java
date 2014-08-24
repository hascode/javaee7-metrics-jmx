package com.hascode.tutorial.ejb;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

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
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import com.yammer.metrics.reporting.JmxReporter;

@Singleton
@Startup
public class ExampleMetricsBean {
	private static final String REST_REPOSITORIES_URL = "https://bitbucket.org/api/2.0/repositories/hascode";

	private final Logger log = LoggerFactory.getLogger(ExampleMetricsBean.class);

	private MetricsRegistry registry;
	private Counter repositoriesParsed;
	private AtomicLong reqSent;
	private JmxReporter reporter;
	private Timer pageProcTimer;

	@PostConstruct
	protected void onBeanConstruction() {
		reqSent = new AtomicLong(0);
		registry = new MetricsRegistry();
		repositoriesParsed = registry.newCounter(ExampleMetricsBean.class, "Repositories-Parsed");
		pageProcTimer = registry.newTimer(ExampleMetricsBean.class, "Processing-Page-Time");
		registry.newGauge(new MetricName(ExampleMetricsBean.class, "Requests-Send-Total"), new Gauge<AtomicLong>() {
			@Override
			public AtomicLong value() {
				return reqSent;
			}
		});
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
		URL url = new URL(REST_REPOSITORIES_URL);
		repositoriesParsed.clear();
		queryBitbucket(url);
	}

	private void queryBitbucket(final URL url) {
		try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			TimerContext timerCtx = pageProcTimer.time();
			reqSent.incrementAndGet();
			JsonObject obj = rdr.readObject();
			JsonNumber currentElements = obj.getJsonNumber("pagelen");
			JsonString nextPage = obj.getJsonString("next");
			log.info("{} elements on current page, next page is: {}", currentElements, nextPage);
			JsonArray repositories = obj.getJsonArray("values");
			for (JsonObject repository : repositories.getValuesAs(JsonObject.class)) {
				repositoriesParsed.inc();
				log.info("repository '{}' has url: '{}'", repository.getString("name"), repository.getJsonObject("links").getJsonObject("self").getString("href"));
			}
			timerCtx.stop();
			if (nextPage != null) {
				queryBitbucket(new URL(nextPage.getString()));
			}
		} catch (IOException e) {
			log.warn("io exception thrown", e);
		}
	}
}
