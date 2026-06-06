import { Component } from '@angular/core';
import { InvoiceRecalcComponent } from './components/invoice-recalc/invoice-recalc';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [InvoiceRecalcComponent],
  template: `<app-invoice-recalc></app-invoice-recalc>`
})
export class App {}
