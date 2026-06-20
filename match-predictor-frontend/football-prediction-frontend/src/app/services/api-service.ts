import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // Teams
  getTeams(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/predictions/teams`);
  }

  // Matches
  getUpcomingMatches(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/predictions/upcoming-matches`);
  }

  getAllMatches(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/predictions/all`);
  }

  // Predictions
  generatePrediction(matchId: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/predictions/generate/${matchId}`, {});
  }

  getAllPredictions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/predictions/all`);
  }

  getLatestPrediction(matchId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/predictions/match/${matchId}/latest`);
  }

  getPredictionStats(): Observable<string> {
    return this.http.get(`${this.baseUrl}/predictions/stats`, { responseType: 'text' });
  }

  // Contextual AI
  generateContextualPrediction(request: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/contextual/predict`, request);
  }

  getConversationHistory(sessionId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/contextual/history/${sessionId}`);
  }

  getContextOptions(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/contextual/options`);
  }

  // ChromaDB
  getChromaDbStatus(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/chromadb/status`);
  }

  searchPredictions(query: string, limit: number = 5): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/chromadb/search`, {
      params: { query, limit: limit.toString() }
    });
  }

  getPredictionsByTeam(teamName: string, limit: number = 10): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/chromadb/team/${teamName}`, {
      params: { limit: limit.toString() }
    });
  }

  getAnalytics(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/chromadb/analytics`);
  }

  getHistoricalContext(homeTeam: string, awayTeam: string): Observable<string> {
    return this.http.get(`${this.baseUrl}/chromadb/history/${homeTeam}/${awayTeam}`, {
      responseType: 'text'
    });
  }

  regenerateAnalytics(): Observable<string> {
    return this.http.post(`${this.baseUrl}/chromadb/regenerate-analytics`, {}, {
      responseType: 'text'
    });
  }

  // Chat endpoint - Now uses real backend
  testAi(message: string): Observable<string> {
    return this.http.post<{response: string}>(`${this.baseUrl}/chat/message`, { message })
      .pipe(
        map(response => response.response)
      );
  }
}

