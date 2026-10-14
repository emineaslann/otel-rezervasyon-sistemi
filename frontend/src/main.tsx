import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import App from './App'
import { OturumSaglayici } from './context/OturumContext'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <OturumSaglayici>
        <App />
      </OturumSaglayici>
    </BrowserRouter>
  </StrictMode>,
)