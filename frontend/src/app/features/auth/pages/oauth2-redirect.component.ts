import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TokenService } from '../../../core/services/token.service';

@Component({
  selector: 'app-oauth2-redirect',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="display: flex; justify-content: center; align-items: center; height: 100vh; font-family: sans-serif; flex-direction: column;">
      <h2>Authenticating with Google...</h2>
      <p>Please wait while we complete your secure login.</p>
    </div>
  `,
})
export class OAuth2RedirectComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private tokenService: TokenService
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    const refreshToken = this.route.snapshot.queryParamMap.get('refreshToken');
    const error = this.route.snapshot.queryParamMap.get('error');

    if (token) {
      this.tokenService.setTokens(token, refreshToken || '');
      this.router.navigate(['/home']);
    } else {
      console.error('OAuth2 Error:', error);
      this.router.navigate(['/auth'], { queryParams: { error: error || 'OAuth authentication failed' } });
    }
  }
}
