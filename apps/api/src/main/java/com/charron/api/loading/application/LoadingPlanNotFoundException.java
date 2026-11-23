
package com.charron.api.loading.application;

public class LoadingPlanNotFoundException
        extends IllegalArgumentException {

    public LoadingPlanNotFoundException(String message) {
        super(message);
    }
}
