package com.hascode.tutorial.ejb;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

public class MetricRegistryFactoryBean {
	@Produces
	@ApplicationScoped
	private MetricRegistry metricRegistry() {
		MetricRegistry registry = new MetricRegistry();
		JmxReporter reporter = JmxReporter.forRegistry(registry).build();
		reporter.start();
		return registry;
	}
}
