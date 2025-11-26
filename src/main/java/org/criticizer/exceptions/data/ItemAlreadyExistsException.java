package org.criticizer.exceptions.data;

import org.criticizer.exceptions.ApplicationException;

public class ItemAlreadyExistsException extends ApplicationException {
    public ItemAlreadyExistsException(String itemType, String itemName) {
        super("This " + itemType + " already exists in your list",
                itemType + " '" + itemName + "' already exists for this user");
    }
}
