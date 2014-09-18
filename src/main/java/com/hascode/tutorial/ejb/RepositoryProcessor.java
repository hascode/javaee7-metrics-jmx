package com.hascode.tutorial.ejb;

import java.util.concurrent.atomic.AtomicLong;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.inject.Inject;
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

@Stateful
@LocalBean
public class RepositoryProcessor {
	private final Logger log = LoggerFactory.getLogger(RepositoryProcessor.class);
	private final AtomicLong reqSent = new AtomicLong(0);

	@Inject
	@Metric(name = "Repositories-Parsed")
	private Counter repositoriesParsed;

	@Timed(name = "Processing-Page-Time")
	public JsonString handleJson(final JsonReader rdr) {
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
