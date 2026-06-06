package com.invoice.recalc.service;

import com.invoice.recalc.dto.InvoiceDto;
import com.invoice.recalc.exception.InvoiceNotFoundException;
import com.invoice.recalc.exception.RecalculationLimitExceededException;
import com.invoice.recalc.model.Invoice;
import com.invoice.recalc.model.InvoiceDetail;
import com.invoice.recalc.model.User;
import com.invoice.recalc.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceService {

    private static final BigDecimal MAX_INCREMENT_TIPO_A = new BigDecimal("20000");
    private static final BigDecimal MAX_INCREMENT_TIPO_B = new BigDecimal("50000");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final InvoiceRepository invoiceRepository;

    @Value("${app.iva.percentage:19.0}")
    private BigDecimal ivaPercentage;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto.InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDto.InvoiceResponse getInvoiceById(Long id) {
        return toResponse(findInvoiceOrThrow(id));
    }

    @Transactional(readOnly = true)
    public InvoiceDto.RecalculatePreviewResponse previewRecalculation(Long invoiceId, InvoiceDto.RecalculateRequest request) {
        Invoice invoice = findInvoiceOrThrow(invoiceId);
        validateRecalculationLimit(invoice.getSubtotal(), request.getNewSubtotal(), request.getUserType());

        BigDecimal factor = calculateFactor(invoice.getSubtotal(), request.getNewSubtotal());
        List<InvoiceDto.DetailResponse> recalculatedDetails = recalculateDetails(invoice.getDetails(), factor);
        BigDecimal newIva = calculateIva(request.getNewSubtotal());
        BigDecimal newTotal = request.getNewSubtotal().add(newIva).setScale(SCALE, ROUNDING);

        InvoiceDto.RecalculatePreviewResponse response = new InvoiceDto.RecalculatePreviewResponse();
        response.setOriginalSubtotal(invoice.getSubtotal());
        response.setNewSubtotal(request.getNewSubtotal());
        response.setAppliedFactor(factor.setScale(6, ROUNDING));
        response.setIvaPercentage(ivaPercentage);
        response.setNewIvaAmount(newIva);
        response.setNewTotal(newTotal);
        response.setRecalculatedDetails(recalculatedDetails);
        response.setMessage("Vista previa generada. Los valores NO han sido guardados.");
        return response;
    }

    @Transactional
    public InvoiceDto.InvoiceResponse confirmRecalculation(Long invoiceId, InvoiceDto.RecalculateRequest request) {
        Invoice invoice = findInvoiceOrThrow(invoiceId);
        validateRecalculationLimit(invoice.getSubtotal(), request.getNewSubtotal(), request.getUserType());

        BigDecimal factor = calculateFactor(invoice.getSubtotal(), request.getNewSubtotal());
        BigDecimal sumOfDetails = BigDecimal.ZERO;
        List<InvoiceDetail> details = invoice.getDetails();

        for (int i = 0; i < details.size(); i++) {
            InvoiceDetail detail = details.get(i);
            BigDecimal newLineTotal;
            if (i == details.size() - 1) {
                newLineTotal = request.getNewSubtotal().subtract(sumOfDetails).setScale(SCALE, ROUNDING);
            } else {
                newLineTotal = detail.getLineTotal().multiply(factor).setScale(SCALE, ROUNDING);
            }
            BigDecimal newUnitPrice = newLineTotal.divide(BigDecimal.valueOf(detail.getQuantity()), SCALE, ROUNDING);
            detail.setLineTotal(newLineTotal);
            detail.setUnitPrice(newUnitPrice);
            sumOfDetails = sumOfDetails.add(newLineTotal);
        }

        BigDecimal newIva = calculateIva(request.getNewSubtotal());
        invoice.setSubtotal(request.getNewSubtotal());
        invoice.setIvaAmount(newIva);
        invoice.setTotal(request.getNewSubtotal().add(newIva).setScale(SCALE, ROUNDING));
        invoice.setIvaPercentage(ivaPercentage);

        return toResponse(invoiceRepository.save(invoice));
    }

    private void validateRecalculationLimit(BigDecimal currentSubtotal, BigDecimal newSubtotal, User.UserType userType) {
        BigDecimal increment = newSubtotal.subtract(currentSubtotal);
        if (increment.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal maxAllowed = userType == User.UserType.TIPO_A ? MAX_INCREMENT_TIPO_A : MAX_INCREMENT_TIPO_B;
        if (increment.compareTo(maxAllowed) > 0) {
            throw new RecalculationLimitExceededException(userType, maxAllowed, increment);
        }
    }

    private BigDecimal calculateFactor(BigDecimal originalSubtotal, BigDecimal newSubtotal) {
        if (originalSubtotal.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("El subtotal original no puede ser cero");
        }
        return newSubtotal.divide(originalSubtotal, 10, ROUNDING);
    }

    private BigDecimal calculateIva(BigDecimal subtotal) {
        return subtotal.multiply(ivaPercentage).divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
    }

    private List<InvoiceDto.DetailResponse> recalculateDetails(List<InvoiceDetail> details, BigDecimal factor) {
        List<InvoiceDto.DetailResponse> result = new ArrayList<>();
        for (InvoiceDetail detail : details) {
            BigDecimal newLineTotal = detail.getLineTotal().multiply(factor).setScale(SCALE, ROUNDING);
            BigDecimal newUnitPrice = newLineTotal.divide(BigDecimal.valueOf(detail.getQuantity()), SCALE, ROUNDING);
            InvoiceDto.DetailResponse dr = new InvoiceDto.DetailResponse();
            dr.setId(detail.getId());
            dr.setProductName(detail.getProductName());
            dr.setQuantity(detail.getQuantity());
            dr.setUnitPrice(newUnitPrice);
            dr.setLineTotal(newLineTotal);
            result.add(dr);
        }
        return result;
    }

    private Invoice findInvoiceOrThrow(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
    }

    private InvoiceDto.InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceDto.DetailResponse> details = invoice.getDetails().stream().map(d -> {
            InvoiceDto.DetailResponse dr = new InvoiceDto.DetailResponse();
            dr.setId(d.getId());
            dr.setProductName(d.getProductName());
            dr.setQuantity(d.getQuantity());
            dr.setUnitPrice(d.getUnitPrice());
            dr.setLineTotal(d.getLineTotal());
            return dr;
        }).toList();

        InvoiceDto.InvoiceResponse r = new InvoiceDto.InvoiceResponse();
        r.setId(invoice.getId());
        r.setInvoiceNumber(invoice.getInvoiceNumber());
        r.setCustomerName(invoice.getCustomerName());
        r.setSubtotal(invoice.getSubtotal());
        r.setIvaPercentage(invoice.getIvaPercentage());
        r.setIvaAmount(invoice.getIvaAmount());
        r.setTotal(invoice.getTotal());
        r.setCreatedAt(invoice.getCreatedAt());
        r.setUpdatedAt(invoice.getUpdatedAt());
        r.setDetails(details);
        return r;
    }
}
