import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InvoiceRecalc } from './invoice-recalc';

describe('InvoiceRecalc', () => {
  let component: InvoiceRecalc;
  let fixture: ComponentFixture<InvoiceRecalc>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvoiceRecalc],
    }).compileComponents();

    fixture = TestBed.createComponent(InvoiceRecalc);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
