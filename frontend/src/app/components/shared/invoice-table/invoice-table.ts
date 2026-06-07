import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TableRow {
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

@Component({
  selector: 'app-invoice-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './invoice-table.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceTableComponent {
  @Input() rows: TableRow[] = [];
  @Input() isPreview = false;
  @Input() footerLabel = '';
  @Input() footerValue = 0;

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency: 'COP', minimumFractionDigits: 0
    }).format(value);
  }
}
