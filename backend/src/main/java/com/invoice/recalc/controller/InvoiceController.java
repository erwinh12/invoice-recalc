package com.invoice.recalc.controller;

import com.invoice.recalc.dto.InvoiceDto;
import com.invoice.recalc.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceDto.InvoiceResponse>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDto.InvoiceResponse> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @PostMapping("/{id}/recalculate/preview")
    public ResponseEntity<InvoiceDto.RecalculatePreviewResponse> previewRecalculation(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceDto.RecalculateRequest request) {
        return ResponseEntity.ok(invoiceService.previewRecalculation(id, request));
    }

    @PostMapping("/{id}/recalculate/confirm")
    public ResponseEntity<InvoiceDto.InvoiceResponse> confirmRecalculation(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceDto.RecalculateRequest request) {
        return ResponseEntity.ok(invoiceService.confirmRecalculation(id, request));
    }
}
