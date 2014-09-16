package com.hascode.tutorial.ejb;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.annotation.Metric;
import com.codahale.metrics.annotation.Timed;

@Singleton
@LocalBean
public class ExampleMetricsBean {
	private static final String REST_REPOSITORIES_URL = "https://bitbucket.org/api/2.0/repositories/hascode";

	private final Logger log = LoggerFactory.getLogger(ExampleMetricsBean.class);
	private final AtomicLong reqSent = new AtomicLong(0);

	@Inject
	@Metric(name = "Repositories-Parsed")
	private Counter repositoriesParsed;

	@Schedule(second = "*/60", minute = "*", hour = "*")
	public void parseBitbucketRepositories() throws MalformedURLException {
		log.info("parsing bitbucket repositories");
		URL url = new URL(REST_REPOSITORIES_URL);
		queryBitbucket(url);
	}

	private void queryBitbucket(final URL url) {
		try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			JsonString nextPage = handleJson(rdr);
			if (nextPage != null) {
				queryBitbucket(new URL(nextPage.getString()));
			}
		} catch (IOException e) {
			log.warn("io exception thrown", e);
		}
	}

	@Timed(name = "Processing-Page-Time")
	private JsonString handleJson(final JsonReader rdr) {
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
		return nextPage;
	}
}
