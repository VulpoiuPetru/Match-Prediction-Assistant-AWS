import { AfterViewChecked, ChangeDetectorRef, Component, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../services/api-service';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms'; 
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'app-dashboard-component',
  imports: [
    CommonModule,
    FormsModule,

    // Angular Material
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,

    // HTTP
    HttpClientModule],
  templateUrl: './dashboard-component.html',
  styleUrl: './dashboard-component.css'
})
export class DashboardComponent implements OnInit {

 private http = inject(HttpClient);
  private snackBar = inject(MatSnackBar);
  private API = 'http://localhost:8080/api';
  
  teams: any[] = [];
  predictions: any[] = [];
  
  loadingTeams = false;
  loadingPredictions = false;
  
  // Chat
  chatMessages: {role: 'user' | 'ai', content: string}[] = [];
  userMessage = '';
  isThinking = false;

  // Team Selector
  selectedTeam1: any = null;
  selectedTeam2: any = null;
  isPredictingManual = false;
  manualPrediction: any = null;

  ngOnInit() {
    this.loadTeams();
    this.loadPredictions();
    this.initChat();
  }

  initChat() {
    this.chatMessages = [{
      role: 'ai',
      content: 'Hi! Ask me about match predictions.\n\nTry:\n• "Real Madrid vs Barcelona"\n• "Who will win Liverpool vs Chelsea?"\n• "Show me teams"'
    }];
  }
  sendChat() {
    if (!this.userMessage.trim() || this.isThinking) return;

    const message = this.userMessage.trim();
    this.chatMessages.push({ role: 'user', content: message });
    this.userMessage = '';
    this.isThinking = true;

    this.http.post<{response: string}>(`${this.API}/chat/message`, { message }).subscribe({
      next: (data) => {
        this.chatMessages.push({ role: 'ai', content: data.response });
        this.isThinking = false;
      },
      error: (err) => {
        console.error('❌ HTTP Error:', err);
        this.chatMessages.push({ 
          role: 'ai', 
          content: 'Error connecting to server. Make sure backend is running!' 
        });
        this.isThinking = false;
      }
    });
}

  loadTeams() {
    this.loadingTeams = true;
    this.http.get<any[]>(`${this.API}/predictions/teams`).subscribe({
      next: (data) => {
        this.teams = data;
        console.log(this.loadingTeams);
        this.loadingTeams = false;
        console.log(this.loadingTeams);
      },
      error: () => {
        this.loadingTeams = false;
        this.snackBar.open('Failed to load teams', 'Close', { duration: 3000 });
      }
    });
  }

  loadPredictions() {
    this.loadingPredictions = true;
    this.http.get<any[]>(`${this.API}/predictions/all`).subscribe({
      next: (data) => {
        this.predictions = data;
        this.loadingPredictions = false;
      },
      error: () => {
        this.loadingPredictions = false;
      }
    });
  }

  predictManual() {
    if (!this.selectedTeam1 || !this.selectedTeam2 || this.selectedTeam1.id === this.selectedTeam2.id) {
      this.snackBar.open('Please select two different teams', 'Close', { duration: 3000 });
      return;
    }

    // Use setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      this.isPredictingManual = true;
      this.manualPrediction = null;
     // this.cdr.detectChanges();

      // Call the new RAG endpoint
      const requestBody = {
        homeTeamId: this.selectedTeam1.id,
        awayTeamId: this.selectedTeam2.id
      };

      this.http.post<any>(`${this.API}/predictions/predict-teams`, requestBody).subscribe({
        next: (prediction) => {
          this.manualPrediction = prediction;
          this.isPredictingManual = false;
          this.snackBar.open('RAG Prediction with real data generated!', 'Close', { duration: 3000 });
          this.loadPredictions();
        },
        error: (err) => {
          this.isPredictingManual = false;
          console.error('RAG Prediction error:', err);
          this.snackBar.open('Failed! Check if Ollama is running with llama3.2', 'Close', { duration: 5000 });
        }
      });
    }, 0);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString();
  }

}
