package com.invoice.recalc.service;

import com.invoice.recalc.dto.InvoiceDto;
import com.invoice.recalc.exception.InvoiceNotFoundException;
import com.invoice.recalc.exception.RecalculationLimitExceededException;
import com.invoice.recalc.model.Invoice;
import com.invoice.recalc.model.InvoiceDetail;
import com.invoice.recalc.model.User;
import com.invoice.recalc.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invoiceService, "ivaPercentage", new BigDecimal("19.0"));
    }

    private Invoice buildInvoice(String subtotal) {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("FAC-001");
        invoice.setCustomerName("Test Client");
        invoice.setSubtotal(new BigDecimal(subtotal));
        invoice.setIvaPercentage(new BigDecimal("19.00"));
        invoice.setIvaAmount(new BigDecimal(subtotal).multiply(new BigDecimal("0.19")).setScale(2, RoundingMode.HALF_UP));
        invoice.setTotal(new BigDecimal(subtotal).multiply(new BigDecimal("1.19")).setScale(2, RoundingMode.HALF_UP));

        InvoiceDetail d1 = new InvoiceDetail();
        d1.setId(1L); d1.setInvoice(invoice);
        d1.setProductName("Producto A"); d1.setQuantity(1);
        d1.setUnitPrice(new BigDecimal("50000.00"));
        d1.setLineTotal(new BigDecimal("50000.00"));

        InvoiceDetail d2 = new InvoiceDetail();
        d2.setId(2L); d2.setInvoice(invoice);
        d2.setProductName("Producto B"); d2.setQuantity(2);
        d2.setUnitPrice(new BigDecimal("15000.00"));
        d2.setLineTotal(new BigDecimal("30000.00"));

        List<InvoiceDetail> details = new ArrayList<>();
        details.add(d1);
        details.add(d2);
        invoice.setDetails(details);
        return invoice;
    }

    private InvoiceDto.RecalculateRequest buildRequest(String subtotal, User.UserType userType) {
        InvoiceDto.RecalculateRequest req = new InvoiceDto.RecalculateRequest();
        req.setNewSubtotal(new BigDecimal(subtotal));
        req.setUserType(userType);
        return req;
    }

    @Test
    @DisplayName("Preview: reducción proporcional del 25% sobre factura de $80,000 → $60,000")
    void preview_shouldApplyProportionalReduction() {
        Invoice invoice = buildInvoice("80000");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        InvoiceDto.RecalculatePreviewResponse preview =
                invoiceService.previewRecalculation(1L, buildRequest("60000", User.UserType.TIPO_A));

        assertThat(preview.getNewSubtotal()).isEqualByComparingTo("60000");
        assertThat(preview.getOriginalSubtotal()).isEqualByComparingTo("80000");
        assertThat(preview.getAppliedFactor()).isEqualByComparingTo("0.750000");
        assertThat(preview.getRecalculatedDetails().get(0).getLineTotal()).isEqualByComparingTo("37500.00");
        assertThat(preview.getRecalculatedDetails().get(1).getLineTotal()).isEqualByComparingTo("22500.00");
        assertThat(preview.getNewIvaAmount()).isEqualByComparingTo("11400.00");
        assertThat(preview.getNewTotal()).isEqualByComparingTo("71400.00");
    }

    @Test
    @DisplayName("Preview: incremento dentro del límite de Tipo A ($20,000)")
    void preview_tipoA_incrementWithinLimit_shouldSucceed() {
        Invoice invoice = buildInvoice("80000");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        assertThatNoException().isThrownBy(() ->
                invoiceService.previewRecalculation(1L, buildRequest("100000", User.UserType.TIPO_A)));
    }

    @Test
    @DisplayName("Preview: Tipo A excede límite de $20,000 → debe lanzar excepción")
    void preview_tipoA_exceedsLimit_shouldThrow() {
        Invoice invoice = buildInvoice("80000");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        assertThatThrownBy(() ->
                invoiceService.previewRecalculation(1L, buildRequest("100001", User.UserType.TIPO_A)))
                .isInstanceOf(RecalculationLimitExceededException.class)
                .hasMessageContaining("TIPO_A")
                .hasMessageContaining("20000");
    }

    @Test
    @DisplayName("Preview: Tipo B puede incrementar hasta $50,000")
    void preview_tipoB_incrementWithinLimit_shouldSucceed() {
        Invoice invoice = buildInvoice("80000");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        assertThatNoException().isThrownBy(() ->
                invoiceService.previewRecalculation(1L, buildRequest("130000", User.UserType.TIPO_B)));
    }

    @Test
    @DisplayName("Preview: Tipo B excede límite de $50,000 → debe lanzar excepción")
    void preview_tipoB_exceedsLimit_shouldThrow() {
        Invoice invoice = buildInvoice("80000");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        assertThatThrownBy(() ->
                invoiceService.previewRecalculation(1L, buildRequest("130001", User.UserType.TIPO_B)))
                .isInstanceOf(RecalculationLimitExceededException.class)
                .hasMessageContaining("TIPO_B")
                .hasMessageContaining("50000");
    }

    @Test
    @DisplayName("Preview: reducción siempre permitida sin importar el tipo")
    void preview_reduction_alwaysAllowed() {
        Invoice invoice = buildInvoice("80000");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        assertThatNoException().isThrownBy(() ->
                invoiceService.previewRecalculation(1L, buildRequest("1", User.UserType.TIPO_A)));
    }

    @Test
    @DisplayName("Preview: factura no encontrada → InvoiceNotFoundException")
    void preview_invoiceNotFound_shouldThrow() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
                invoiceService.previewRecalculation(99L, buildRequest("60000", User.UserType.TIPO_A)))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    @DisplayName("Confirm: los totales de la factura se actualizan correctamente")
    void confirm_shouldPersistUpdatedTotals() {
        Invoice invoice = buildInvoice("80000");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        InvoiceDto.InvoiceResponse response =
                invoiceService.confirmRecalculation(1L, buildRequest("60000", User.UserType.TIPO_A));

        assertThat(response.getSubtotal()).isEqualByComparingTo("60000.00");
        assertThat(response.getIvaAmount()).isEqualByComparingTo("11400.00");
        assertThat(response.getTotal()).isEqualByComparingTo("71400.00");

        BigDecimal detailsSum = response.getDetails().stream()
                .map(InvoiceDto.DetailResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(detailsSum).isEqualByComparingTo("60000.00");

        verify(invoiceRepository).save(any(Invoice.class));
    }
}
