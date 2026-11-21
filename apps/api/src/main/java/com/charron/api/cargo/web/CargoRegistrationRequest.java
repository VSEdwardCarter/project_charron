
package com.charron.api.cargo.web;

public record CargoRegistrationRequest(
        String name,
        String reference,
        String description,
        Long weightGrams,
        Long lengthMm,
        Long widthMm,
        Long heightMm
) {}
