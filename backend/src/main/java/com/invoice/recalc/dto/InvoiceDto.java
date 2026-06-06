package com.invoice.recalc.dto;

import com.invoice.recalc.model.User;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class InvoiceDto {

    public static class RecalculateRequest {
        @NotNull(message = "El nuevo subtotal es obligatorio")
        @DecimalMin(value = "0.01", message = "El nuevo subtotal debe ser mayor a 0")
        @Digits(integer = 13, fraction = 2, message = "Formato de subtotal inválido")
        private BigDecimal newSubtotal;

        @NotNull(message = "El tipo de usuario es obligatorio")
        private User.UserType userType;

        public RecalculateRequest() {}
        public BigDecimal getNewSubtotal() { return newSubtotal; }
        public void setNewSubtotal(BigDecimal newSubtotal) { this.newSubtotal = newSubtotal; }
        public User.UserType getUserType() { return userType; }
        public void setUserType(User.UserType userType) { this.userType = userType; }
    }

    public static class InvoiceResponse {
        private Long id;
        private String invoiceNumber;
        private String customerName;
        private BigDecimal subtotal;
        private BigDecimal ivaPercentage;
        private BigDecimal ivaAmount;
        private BigDecimal total;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<DetailResponse> details;

        public InvoiceResponse() {}
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
        public BigDecimal getIvaPercentage() { return ivaPercentage; }
        public void setIvaPercentage(BigDecimal ivaPercentage) { this.ivaPercentage = ivaPercentage; }
        public BigDecimal getIvaAmount() { return ivaAmount; }
        public void setIvaAmount(BigDecimal ivaAmount) { this.ivaAmount = ivaAmount; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public List<DetailResponse> getDetails() { return details; }
        public void setDetails(List<DetailResponse> details) { this.details = details; }
    }

    public static class DetailResponse {
        private Long id;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public DetailResponse() {}
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getLineTotal() { return lineTotal; }
        public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    }

    public static class RecalculatePreviewResponse {
        private BigDecimal originalSubtotal;
        private BigDecimal newSubtotal;
        private BigDecimal appliedFactor;
        private BigDecimal ivaPercentage;
        private BigDecimal newIvaAmount;
        private BigDecimal newTotal;
        private List<DetailResponse> recalculatedDetails;
        private String message;

        public RecalculatePreviewResponse() {}
        public BigDecimal getOriginalSubtotal() { return originalSubtotal; }
        public void setOriginalSubtotal(BigDecimal originalSubtotal) { this.originalSubtotal = originalSubtotal; }
        public BigDecimal getNewSubtotal() { return newSubtotal; }
        public void setNewSubtotal(BigDecimal newSubtotal) { this.newSubtotal = newSubtotal; }
        public BigDecimal getAppliedFactor() { return appliedFactor; }
        public void setAppliedFactor(BigDecimal appliedFactor) { this.appliedFactor = appliedFactor; }
        public BigDecimal getIvaPercentage() { return ivaPercentage; }
        public void setIvaPercentage(BigDecimal ivaPercentage) { this.ivaPercentage = ivaPercentage; }
        public BigDecimal getNewIvaAmount() { return newIvaAmount; }
        public void setNewIvaAmount(BigDecimal newIvaAmount) { this.newIvaAmount = newIvaAmount; }
        public BigDecimal getNewTotal() { return newTotal; }
        public void setNewTotal(BigDecimal newTotal) { this.newTotal = newTotal; }
        public List<DetailResponse> getRecalculatedDetails() { return recalculatedDetails; }
        public void setRecalculatedDetails(List<DetailResponse> recalculatedDetails) { this.recalculatedDetails = recalculatedDetails; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;

        public ErrorResponse() {}
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
