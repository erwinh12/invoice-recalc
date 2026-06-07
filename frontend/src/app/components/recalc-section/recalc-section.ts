import {
  Component, Input, Output, EventEmitter, OnChanges, SimpleChanges,
  OnInit, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, EMPTY } from 'rxjs';
import {
  debounceTime, distinctUntilChanged, switchMap,
  filter, tap, catchError, finalize, takeUntil
} from 'rxjs/operators';
import {
  InvoiceService, Invoice, RecalculatePreviewResponse, UserType
} from '../../services/invoice.service';
import { InvoiceTableComponent } from '../shared/invoice-table/invoice-table';

const LIMITS: Record<UserType, number> = { TIPO_A: 20000, TIPO_B: 50000 };

@Component({
  selector: 'app-recalc-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InvoiceTableComponent],
  templateUrl: './recalc-section.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecalcSectionComponent implements OnInit, OnChanges, OnDestroy {
  @Input() invoice!: Invoice;
  @Output() confirmed = new EventEmitter<Invoice>();

  recalcForm!: FormGroup;
  preview: RecalculatePreviewResponse | null = null;
  loading = false;
  previewLoading = false;
  errorMessage = '';
  successMessage = '';
  showModal = false;

  readonly userTypes: { label: string; value: UserType; limit: number }[] = [
    { label: 'Tipo A — Operador', value: 'TIPO_A', limit: 20000 },
    { label: 'Tipo B — Supervisor', value: 'TIPO_B', limit: 50000 }
  ];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private invoiceService: InvoiceService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.recalcForm = this.fb.group({
      newSubtotal: [null, [Validators.required, Validators.min(0.01)]],
      userType: ['TIPO_A', Validators.required]
    });
    this.setupLivePreview();
    this.loadInitialPreview();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['invoice'] && !changes['invoice'].firstChange && this.recalcForm) {
      this.recalcForm.patchValue({ newSubtotal: null }, { emitEvent: false });
      this.loadInitialPreview();
    }
  }

  private loadInitialPreview(): void {
    this.previewLoading = true;
    this.preview = null;
    this.cdr.markForCheck();
    this.invoiceService.preview(this.invoice.id, {
      newSubtotal: this.invoice.subtotal,
      userType: this.recalcForm.get('userType')?.value ?? 'TIPO_A'
    }).pipe(
      tap(result => { this.preview = result; }),
      catchError(() => EMPTY),
      finalize(() => { this.previewLoading = false; this.cdr.markForCheck(); }),
      takeUntil(this.destroy$)
    ).subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupLivePreview(): void {
    this.recalcForm.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(400),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      tap(() => { this.updateLimitValidation(); this.cdr.markForCheck(); }),
      filter(() => this.recalcForm.valid && !this.incrementExceedsLimit),
      tap(() => { this.previewLoading = true; this.errorMessage = ''; this.cdr.markForCheck(); }),
      switchMap(() =>
        this.invoiceService.preview(this.invoice.id, this.recalcForm.value).pipe(
          tap(result => { this.preview = result; this.cdr.markForCheck(); }),
          catchError(err => {
            this.errorMessage = err.error?.message || 'Error al calcular la vista previa.';
            this.preview = null;
            this.cdr.markForCheck();
            return EMPTY;
          }),
          finalize(() => { this.previewLoading = false; this.cdr.markForCheck(); })
        )
      )
    ).subscribe();
  }

  private updateLimitValidation(): void {
    const ctrl = this.recalcForm.get('newSubtotal')!;
    if (this.incrementExceedsLimit) {
      ctrl.setErrors({ ...ctrl.errors, limitExceeded: true });
    } else if (ctrl.errors?.['limitExceeded']) {
      const { limitExceeded, ...rest } = ctrl.errors;
      ctrl.setErrors(Object.keys(rest).length ? rest : null);
    }
  }

  confirmRecalculation(): void {
    if (!this.recalcForm.valid || this.incrementExceedsLimit) return;
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.markForCheck();

    this.invoiceService.confirm(this.invoice.id, this.recalcForm.value).pipe(
      tap(updated => {
        this.successMessage = `Factura ${updated.invoiceNumber} actualizada correctamente.`;
        this.errorMessage = '';
        this.showModal = true;
        this.recalcForm.patchValue({ newSubtotal: null }, { emitEvent: false });
        this.confirmed.emit(updated);
        this.cdr.markForCheck();
      }),
      catchError(err => {
        this.errorMessage = err.error?.message || 'Error al confirmar el recálculo.';
        this.successMessage = '';
        this.showModal = true;
        this.cdr.markForCheck();
        return EMPTY;
      }),
      finalize(() => { this.loading = false; this.cdr.markForCheck(); }),
      takeUntil(this.destroy$)
    ).subscribe();
  }

  closeModal(): void {
    this.showModal = false;
    this.cdr.markForCheck();
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency: 'COP', minimumFractionDigits: 0
    }).format(value);
  }

  get increment(): number {
    if (!this.recalcForm?.get('newSubtotal')?.value) return 0;
    return this.recalcForm.get('newSubtotal')!.value - this.invoice.subtotal;
  }

  get currentUserType(): UserType {
    return this.recalcForm?.get('userType')?.value ?? 'TIPO_A';
  }

  get maxIncrement(): number { return LIMITS[this.currentUserType]; }

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

  get incrementPercent(): number {
    if (!this.invoice?.subtotal) return 0;
    return Math.abs((this.increment / this.invoice.subtotal) * 100);
  }

  get newSubtotalDisplay(): number {
    return this.recalcForm?.get('newSubtotal')?.value ?? 0;
  }

  get reductionBarPercent(): number {
    if (!this.invoice?.subtotal || this.increment >= 0) return 100;
    const val = this.recalcForm.get('newSubtotal')?.value ?? 0;
    return Math.min(Math.max((val / this.invoice.subtotal) * 100, 0), 100);
  }

  get reductionStatus(): 'ok' | 'warning' | 'critical' {
    const pct = this.reductionBarPercent;
    if (pct <= 20) return 'critical';
    if (pct <= 50) return 'warning';
    return 'ok';
  }
}
