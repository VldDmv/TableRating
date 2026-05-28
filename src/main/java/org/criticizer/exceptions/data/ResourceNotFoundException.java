package org.criticizer.exceptions.data;

import org.criticizer.exceptions.ApplicationException;

public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(
                resourceType + " not found",
                resourceType + " with identifier '" + identifier + "' does not exist");
    }
}
