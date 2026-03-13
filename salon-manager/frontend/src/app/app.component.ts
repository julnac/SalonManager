import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  public title = 'Salon Manager';
  private translate = inject(TranslateService);

  public ngOnInit(): void {
    this.translate.setDefaultLang('pl');
    this.translate.use('pl');
  }
}
