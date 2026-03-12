import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Upload } from './components/upload/upload';
import { Operations } from './components/operations/operations';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Upload, Operations],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App 
{
  
}
