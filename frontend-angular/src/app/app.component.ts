import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WireframeP1Component } from './components/wireframe-p1/wireframe-p1.component';
import { MotorP2Component } from './components/motor-p2/motor-p2.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, WireframeP1Component, MotorP2Component],
  template: `
    <div class="modos">
      <strong>Vista Angular 18:</strong>
      <button [class.activo]="modo === 'P1'" (click)="modo = 'P1'">P1: Wireframe Interacción</button>
      <button [class.activo]="modo === 'P2'" (click)="modo = 'P2'">P2: Motor Real (Spring Boot REST + Angular)</button>
    </div>
    <app-wireframe-p1 *ngIf="modo === 'P1'"></app-wireframe-p1>
    <app-motor-p2 *ngIf="modo === 'P2'"></app-motor-p2>
  `
})
export class AppComponent {
  modo: 'P1' | 'P2' = 'P1';
}
