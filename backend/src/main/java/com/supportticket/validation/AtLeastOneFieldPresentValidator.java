package com.supportticket.validation;

import com.supportticket.dto.request.UpdateTicketRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AtLeastOneFieldPresentValidator implements ConstraintValidator<AtLeastOneFieldPresent, UpdateTicketRequest> {

    @Override
    public boolean isValid(UpdateTicketRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }
        return request.title() != null
                || request.description() != null
                || request.priority() != null
                || request.assigneeIncluded()
                || request.status() != null;
    }
}
