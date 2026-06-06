import { Component, OnInit, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, EMPTY } from 'rxjs';
import {
  debounceTime,
  distinctUntilChanged,
  switchMap,
  filter,
  tap,
  catchError,
  finalize,
  takeUntil
} from 'rxjs/operators';
import { InvoiceService, Invoice, RecalculatePreviewResponse, UserType } from '../../services/invoice.service';

const LIMITS: Record<UserType, number> = {
  TIPO_A: 20000,
  TIPO_B: 50000
};

@Component({
  selector: 'app-invoice-recalc',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './invoice-recalc.html',
  styleUrls: ['./invoice-recalc.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceRecalcComponent implements OnInit, OnDestroy {
  invoices: Invoice[] = [];
  selectedInvoice: Invoice | null = null;
  preview: RecalculatePreviewResponse | null = null;
  confirmedInvoice: Invoice | null = null;

  recalcForm!: FormGroup;
  loading = false;
  loadingInvoices = true;
  invoiceLoadError = '';
  previewLoading = false;
  errorMessage = '';
  successMessage = '';

  userTypes: { label: string; value: UserType; limit: number }[] = [
    { label: 'Tipo A — Operador', value: 'TIPO_A', limit: 20000 },
    { label: 'Tipo B — Supervisor', value: 'TIPO_B', limit: 50000 }
  ];

  readonly skeletons = [1, 2, 3, 4];
  private readonly destroy$ = new Subject<void>();

  constructor(
    private invoiceService: InvoiceService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadInvoices();
    this.setupLivePreview();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  buildForm(): void {
    this.recalcForm = this.fb.group({
      newSubtotal: [null, [Validators.required, Validators.min(0.01)]],
      userType: ['TIPO_A', Validators.required]
    });
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
    this.preview = null;
    this.confirmedInvoice = null;
    this.errorMessage = '';
    this.successMessage = '';
    this.recalcForm.reset({ newSubtotal: null, userType: 'TIPO_A' });
    this.cdr.markForCheck();
  }

  setupLivePreview(): void {
    this.recalcForm.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      tap(() => {
        if (this.selectedInvoice) this.updateLimitValidation();
        this.cdr.markForCheck();
      }),
      filter(() => this.recalcForm.valid && !!this.selectedInvoice && !this.incrementExceedsLimit),
      tap(() => { this.previewLoading = true; this.errorMessage = ''; this.cdr.markForCheck(); }),
      switchMap(() =>
        this.invoiceService.preview(this.selectedInvoice!.id, this.recalcForm.value).pipe(
          tap(result => {
            this.preview = result;
            this.cdr.markForCheck();
          }),
          catchError(err => {
            this.errorMessage = err.error?.message || 'Error al calcular la vista previa.';
            this.preview = null;
            this.cdr.markForCheck();
            return EMPTY;
          }),
          finalize(() => { this.previewLoading = false; this.cdr.markForCheck(); })
        )
      ),
      takeUntil(this.destroy$)
    ).subscribe();
  }

  private updateLimitValidation(): void {
    const ctrl = this.recalcForm.get('newSubtotal')!;
    if (this.incrementExceedsLimit) {
      ctrl.setErrors({ ...ctrl.errors, limitExceeded: true });
    } else {
      if (ctrl.errors?.['limitExceeded']) {
        const { limitExceeded, ...rest } = ctrl.errors;
        ctrl.setErrors(Object.keys(rest).length ? rest : null);
      }
    }
  }

  confirmRecalculation(): void {
    if (!this.selectedInvoice || !this.recalcForm.valid || this.incrementExceedsLimit) return;
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.markForCheck();

    this.invoiceService.confirm(this.selectedInvoice.id, this.recalcForm.value).pipe(
      tap(updated => {
        this.confirmedInvoice = updated;
        this.selectedInvoice = updated;
        this.preview = null;
        this.successMessage = `Factura ${updated.invoiceNumber} actualizada correctamente.`;
        this.cdr.markForCheck();
      }),
      catchError(err => {
        this.errorMessage = err.error?.message || 'Error al confirmar el recálculo.';
        this.cdr.markForCheck();
        return EMPTY;
      }),
      finalize(() => { this.loading = false; this.cdr.markForCheck(); }),
      takeUntil(this.destroy$)
    ).subscribe(() => this.loadInvoices());
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }).format(value);
  }

  get increment(): number {
    if (!this.selectedInvoice || !this.recalcForm?.get('newSubtotal')?.value) return 0;
    return this.recalcForm.get('newSubtotal')!.value - this.selectedInvoice.subtotal;
  }

  get currentUserType(): UserType {
    return this.recalcForm?.get('userType')?.value ?? 'TIPO_A';
  }

  get maxIncrement(): number {
    return LIMITS[this.currentUserType];
  }

  get incrementExceedsLimit(): boolean {
    return this.increment > 0 && this.increment > this.maxIncrement;
  }

  get remainingIncrement(): number {
    return Math.max(0, this.maxIncrement - Math.max(0, this.increment));
  }

  get limitUsagePercent(): number {
    if (this.increment <= 0) return 0;
    return Math.min((this.increment / this.maxIncrement) * 100, 100);
  }

  get limitStatus(): 'ok' | 'warning' | 'exceeded' {
    if (this.incrementExceedsLimit) return 'exceeded';
    if (this.limitUsagePercent >= 80) return 'warning';
    return 'ok';
  }

  Math = Math;
}
