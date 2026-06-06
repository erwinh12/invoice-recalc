package com.invoice.recalc.exception;

import com.invoice.recalc.model.User;

import java.math.BigDecimal;

public class RecalculationLimitExceededException extends RuntimeException {
    public RecalculationLimitExceededException(User.UserType userType, BigDecimal maxAllowed, BigDecimal attempted) {
        super(String.format(
            "El usuario tipo %s no puede incrementar el subtotal más de $%s. Incremento intentado: $%s",
            userType.name(), maxAllowed.toPlainString(), attempted.toPlainString()
        ));
    }
}
