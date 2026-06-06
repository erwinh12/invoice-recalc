package com.invoice.recalc.exception;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(Long id) {
        super("Factura con ID " + id + " no encontrada");
    }
}
