import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard-component/dashboard-component';
import { HistoryComponent } from './components/history-component/history-component';
import { LoginComponent } from './components/login-component/login-component';



export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'history', component: HistoryComponent },
  { path: '**', redirectTo: '/dashboard' }
//   { path: 'teams', component: TeamsComponent },
//   { path: 'matches', component: MatchesComponent },
//   { path: 'predictions', component: PredictionsComponent },
//   { path: 'ai-test', component: AiTestComponent }
];