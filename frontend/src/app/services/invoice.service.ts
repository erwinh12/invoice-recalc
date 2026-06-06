// src/app/services/invoice.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type UserType = 'TIPO_A' | 'TIPO_B';

export interface InvoiceDetail {
  id: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  customerName: string;
  subtotal: number;
  ivaPercentage: number;
  ivaAmount: number;
  total: number;
  createdAt: string;
  updatedAt: string;
  details: InvoiceDetail[];
}

export interface RecalculateRequest {
  newSubtotal: number;
  userType: UserType;
}

export interface RecalculatePreviewResponse {
  originalSubtotal: number;
  newSubtotal: number;
  appliedFactor: number;
  ivaPercentage: number;
  newIvaAmount: number;
  newTotal: number;
  recalculatedDetails: InvoiceDetail[];
  message: string;
}

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly API_URL = 'http://localhost:8080/api/v1/invoices';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(this.API_URL);
  }

  getById(id: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.API_URL}/${id}`);
  }

  preview(id: number, request: RecalculateRequest): Observable<RecalculatePreviewResponse> {
    return this.http.post<RecalculatePreviewResponse>(
      `${this.API_URL}/${id}/recalculate/preview`, request
    );
  }

  confirm(id: number, request: RecalculateRequest): Observable<Invoice> {
    return this.http.post<Invoice>(
      `${this.API_URL}/${id}/recalculate/confirm`, request
    );
  }
}
