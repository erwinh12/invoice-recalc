import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Invoice } from '../../services/invoice.service';

@Component({
  selector: 'app-invoice-select',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './invoice-select.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceSelectComponent {
  @Input() invoices: Invoice[] = [];
  @Input() loading = false;
  @Input() error = '';
  @Input() selected: Invoice | null = null;
  @Input() skeletons: number[] = [];
  @Output() invoiceSelected = new EventEmitter<Invoice>();
  @Output() retry = new EventEmitter<void>();

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency: 'COP', minimumFractionDigits: 0
    }).format(value);
  }
}
