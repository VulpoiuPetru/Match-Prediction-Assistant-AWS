import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login-component.html',
  styleUrl: './login-component.css'
})
export class LoginComponent {
  private router = inject(Router);

  username = '';
  password = '';
  error = '';
  loading = false;

  private users = [
    { username: 'admin', password: 'parola123', role: 'ADMIN' },
    { username: 'testuser', password: 'parola123', role: 'USER' }
  ];

  login() {
    this.loading = true;
    this.error = '';

    setTimeout(() => {
      const user = this.users.find(
        u => u.username === this.username && u.password === this.password
      );

      if (user) {
        localStorage.setItem('currentUser', JSON.stringify(user));
        this.router.navigate(['/dashboard']);
      } else {
        this.error = 'Invalid username or password';
      }
      this.loading = false;
    }, 800);
  }
}