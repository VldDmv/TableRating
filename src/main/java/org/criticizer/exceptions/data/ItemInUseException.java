package org.criticizer.exceptions.data;

import org.criticizer.exceptions.security.OperationNotPermittedException;

public class ItemInUseException extends OperationNotPermittedException {
    public ItemInUseException(String itemType) {
        super("delete" + itemType, "Cannot delete " + itemType + " because it is currently in use");
    }

    public ItemInUseException(String itemType, String details) {
        super("delete" + itemType, "Cannot delete " + itemType + ": " + details);
    }
}
