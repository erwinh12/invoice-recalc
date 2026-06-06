package com.invoice.recalc.config;

import com.invoice.recalc.model.Invoice;
import com.invoice.recalc.model.InvoiceDetail;
import com.invoice.recalc.repository.InvoiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final InvoiceRepository invoiceRepository;

    public DataInitializer(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public void run(String... args) {
        seedInvoices();
        System.out.println("✅ Datos de prueba cargados correctamente.");
    }

    private void seedInvoices() {
        Invoice inv1 = new Invoice();
        inv1.setInvoiceNumber("FAC-001");
        inv1.setCustomerName("Empresa ABC S.A.S");
        inv1.setSubtotal(new BigDecimal("80000.00"));
        inv1.setIvaPercentage(new BigDecimal("19.00"));
        inv1.setIvaAmount(new BigDecimal("15200.00"));
        inv1.setTotal(new BigDecimal("95200.00"));

        InvoiceDetail d1 = new InvoiceDetail();
        d1.setInvoice(inv1); d1.setProductName("Laptop Dell Inspiron");
        d1.setQuantity(1); d1.setUnitPrice(new BigDecimal("50000.00")); d1.setLineTotal(new BigDecimal("50000.00"));

        InvoiceDetail d2 = new InvoiceDetail();
        d2.setInvoice(inv1); d2.setProductName("Mouse Inalámbrico");
        d2.setQuantity(2); d2.setUnitPrice(new BigDecimal("7500.00")); d2.setLineTotal(new BigDecimal("15000.00"));

        InvoiceDetail d3 = new InvoiceDetail();
        d3.setInvoice(inv1); d3.setProductName("Teclado Mecánico");
        d3.setQuantity(1); d3.setUnitPrice(new BigDecimal("15000.00")); d3.setLineTotal(new BigDecimal("15000.00"));

        List<InvoiceDetail> details1 = new ArrayList<>();
        details1.add(d1); details1.add(d2); details1.add(d3);
        inv1.setDetails(details1);
        invoiceRepository.save(inv1);

        Invoice inv2 = new Invoice();
        inv2.setInvoiceNumber("FAC-002");
        inv2.setCustomerName("Distribuciones XYZ Ltda");
        inv2.setSubtotal(new BigDecimal("120000.00"));
        inv2.setIvaPercentage(new BigDecimal("19.00"));
        inv2.setIvaAmount(new BigDecimal("22800.00"));
        inv2.setTotal(new BigDecimal("142800.00"));

        InvoiceDetail d4 = new InvoiceDetail();
        d4.setInvoice(inv2); d4.setProductName("Monitor 27 pulgadas");
        d4.setQuantity(2); d4.setUnitPrice(new BigDecimal("35000.00")); d4.setLineTotal(new BigDecimal("70000.00"));

        InvoiceDetail d5 = new InvoiceDetail();
        d5.setInvoice(inv2); d5.setProductName("Silla Ergonómica");
        d5.setQuantity(1); d5.setUnitPrice(new BigDecimal("30000.00")); d5.setLineTotal(new BigDecimal("30000.00"));

        InvoiceDetail d6 = new InvoiceDetail();
        d6.setInvoice(inv2); d6.setProductName("Escritorio de Pie");
        d6.setQuantity(1); d6.setUnitPrice(new BigDecimal("20000.00")); d6.setLineTotal(new BigDecimal("20000.00"));

        List<InvoiceDetail> details2 = new ArrayList<>();
        details2.add(d4); details2.add(d5); details2.add(d6);
        inv2.setDetails(details2);
        invoiceRepository.save(inv2);
    }
}
