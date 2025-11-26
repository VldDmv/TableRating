package org.criticizer.exceptions;

public class ServiceNotAvailableException extends ApplicationException {
    public ServiceNotAvailableException(String serviceName) {
        super("Service temporarily unavailable. Please try again later.",
                "Service not available: " + serviceName + " not found in context");
    }
}
