
package com.charron.api.movement.application;

public class DuplicateMovementReferenceException
        extends RuntimeException {

    public DuplicateMovementReferenceException(String reference) {
        super("Movement reference already exists: " + reference);
    }
}
