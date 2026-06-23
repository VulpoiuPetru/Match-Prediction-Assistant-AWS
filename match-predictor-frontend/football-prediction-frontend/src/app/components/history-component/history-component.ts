import { Component, OnInit, inject, ChangeDetectorRef, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-history-component',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  templateUrl: './history-component.html',
  styleUrl: './history-component.css'
})
export class HistoryComponent implements OnInit, AfterViewInit {
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private API = 'http://18.199.165.105:8080/api';

  @ViewChild('donutCanvas') donutCanvas!: ElementRef;
  @ViewChild('barCanvas') barCanvas!: ElementRef;

  predictions: any[] = [];
  loading = false;
  totalPredictions = 0;
  accuracy = '0%';
  homeWins = 0;
  draws = 0;
  awayWins = 0;
  dataReady = false;

  ngOnInit() {
    this.loadPredictions();
    this.loadStats();
  }

  ngAfterViewInit() {}

  loadPredictions() {
    this.loading = true;
    this.http.get<any[]>(`${this.API}/predictions/all`).subscribe({
      next: (data) => {
        this.predictions = data;
        this.totalPredictions = data.length;
        this.loading = false;
        this.calculateStats();
        this.cdr.detectChanges();
        setTimeout(() => this.renderCharts(), 100);
      },
      error: () => { this.loading = false; }
    });
  }

  loadStats() {
    this.http.get(`${this.API}/predictions/stats`, { responseType: 'text' }).subscribe({
      next: (data) => {
        this.accuracy = data;
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  calculateStats() {
    this.homeWins = 0; this.draws = 0; this.awayWins = 0;
    for (const p of this.predictions) {
      const h = p.homeWinProbability;
      const d = p.drawProbability;
      const a = p.awayWinProbability;
      if (h > d && h > a) this.homeWins++;
      else if (a > d && a > h) this.awayWins++;
      else this.draws++;
    }
  }

  getTopTeams(): {name: string, count: number}[] {
    const counts: {[key: string]: number} = {};
    for (const p of this.predictions) {
      const h = p.match.homeTeam.name;
      const a = p.match.awayTeam.name;
      counts[h] = (counts[h] || 0) + 1;
      counts[a] = (counts[a] || 0) + 1;
    }
    return Object.entries(counts)
      .map(([name, count]) => ({name, count}))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6);
  }

  renderCharts() {
    if (!this.donutCanvas || !this.barCanvas) return;

    new Chart(this.donutCanvas.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Home Win', 'Draw', 'Away Win'],
        datasets: [{
          data: [this.homeWins, this.draws, this.awayWins],
          backgroundColor: ['#00ff87', '#6b7280', '#f87171'],
          borderColor: '#0a0e1a',
          borderWidth: 3,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        cutout: '70%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#9ca3af', padding: 20, font: { size: 13 } }
          }
        }
      }
    });

    const topTeams = this.getTopTeams();
    new Chart(this.barCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: topTeams.map(t => t.name),
        datasets: [{
          label: 'Predictions',
          data: topTeams.map(t => t.count),
          backgroundColor: 'rgba(0,255,135,0.2)',
          borderColor: '#00ff87',
          borderWidth: 2,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false }
        },
        scales: {
          x: { ticks: { color: '#9ca3af', font: { size: 11 } }, grid: { color: 'rgba(255,255,255,0.04)' } },
          y: { ticks: { color: '#9ca3af' }, grid: { color: 'rgba(255,255,255,0.04)' } }
        }
      }
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString();
  }
}