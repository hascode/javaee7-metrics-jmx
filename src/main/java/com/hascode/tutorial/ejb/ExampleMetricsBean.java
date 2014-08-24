package com.hascode.tutorial.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.reporting.JmxReporter;

@Singleton
@Startup
public class ExampleMetricsBean {
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

	@Schedule(second = "*/10", minute = "*", hour = "*")
	public void doSth() {
		System.err.println("xxxxxx");
		numReqSend.inc();
	}
}
