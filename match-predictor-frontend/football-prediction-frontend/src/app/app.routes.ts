import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard-component/dashboard-component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: '**', redirectTo: '/dashboard' }
//   { path: 'teams', component: TeamsComponent },
//   { path: 'matches', component: MatchesComponent },
//   { path: 'predictions', component: PredictionsComponent },
//   { path: 'ai-test', component: AiTestComponent }
];