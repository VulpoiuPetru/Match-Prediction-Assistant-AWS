import { Component, signal, OnInit  } from '@angular/core';
import { RouterLinkActive, RouterOutlet, RouterLink, NavigationEnd, Router  } from '@angular/router';
import { DashboardComponent } from './components/dashboard-component/dashboard-component';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet,DashboardComponent,RouterLinkActive,RouterLink,CommonModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class App implements OnInit {
  
  // protected readonly title = signal('football-prediction-frontend');
   title = 'Football Predictions';
   showNavbar = false;

  constructor(private router: Router) {}

  ngOnInit() {
  const user = localStorage.getItem('currentUser');
  if (!user) {
    this.router.navigate(['/login']);
  }

  this.router.events.pipe(
    filter(event => event instanceof NavigationEnd)
  ).subscribe((event: any) => {
    this.showNavbar = !event.url.includes('/login');
  });
}

  logout() {
    localStorage.removeItem('currentUser');
    this.router.navigate(['/login']);
  }
}
