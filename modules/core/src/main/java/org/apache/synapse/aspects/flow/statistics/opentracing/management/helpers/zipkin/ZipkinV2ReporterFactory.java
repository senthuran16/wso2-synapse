package org.apache.synapse.aspects.flow.statistics.opentracing.management.helpers.zipkin;

import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * ZipkinV2ReporterFactory receives the Zipkin backend url and generate a Zipkin reporter
 * This class is implemented for the purpose of prevent loading Zipkin related(dependencies) classes while initialising the
 * MI, if Zipkin is disabled in the synapse.properties file
 */
public class ZipkinV2ReporterFactory {

    private ZipkinV2Reporter reporter;

    public ZipkinV2ReporterFactory(String zipkinBackendURL) {
        reporter = new ZipkinV2Reporter(AsyncReporter.create(URLConnectionSender.create(zipkinBackendURL)));
    }

    public ZipkinV2Reporter getReporter() {
        return reporter;
    }
}
