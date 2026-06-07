import { Component, OnInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InvoiceService, Invoice } from '../../services/invoice.service';
import { InvoiceSelectComponent } from '../invoice-select/invoice-select';
import { RecalcSectionComponent } from '../recalc-section/recalc-section';

@Component({
  selector: 'app-invoice-recalc',
  standalone: true,
  imports: [CommonModule, InvoiceSelectComponent, RecalcSectionComponent],
  templateUrl: './invoice-recalc.html',
  styleUrls: ['./invoice-recalc.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceRecalcComponent implements OnInit {
  invoices: Invoice[] = [];
  selectedInvoice: Invoice | null = null;
  loadingInvoices = true;
  invoiceLoadError = '';
  readonly skeletons = [1, 2, 3, 4];

  constructor(
    private invoiceService: InvoiceService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.loadingInvoices = true;
    this.invoiceLoadError = '';
    this.cdr.markForCheck();

    this.invoiceService.getAll().subscribe({
      next: (data) => {
        this.invoices = data;
        this.loadingInvoices = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.invoiceLoadError = 'backend';
        this.loadingInvoices = false;
        this.cdr.markForCheck();
      }
    });
  }

  selectInvoice(invoice: Invoice): void {
    this.selectedInvoice = invoice;
    this.cdr.markForCheck();
  }

  onConfirmed(updated: Invoice): void {
    this.selectedInvoice = updated;
    this.loadInvoices();
  }
}
