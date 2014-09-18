package com.hascode.tutorial.ejb;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@LocalBean
public class ExampleMetricsBean {
	private static final String REST_REPOSITORIES_URL = "https://bitbucket.org/api/2.0/repositories/hascode";

	private final Logger log = LoggerFactory.getLogger(ExampleMetricsBean.class);

	@Inject
	RepositoryProcessor repositoryProcessor;

	@Schedule(second = "*", minute = "*/1", hour = "*")
	public void parseBitbucketRepositories() throws MalformedURLException {
		log.info("parsing bitbucket repositories");
		URL url = new URL(REST_REPOSITORIES_URL);
		queryBitbucket(url);
	}

	private void queryBitbucket(final URL url) {
		try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			JsonString nextPage = repositoryProcessor.handleJson(rdr);
			if (nextPage != null) {
				queryBitbucket(new URL(nextPage.getString()));
			}
		} catch (IOException e) {
			log.warn("io exception thrown", e);
		}
	}
}
