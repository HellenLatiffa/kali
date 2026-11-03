# Interface do Kali

Aplicação React + TypeScript + Vite integrada à API Spring Boot: autenticação, perfil, consentimento, calendário menstrual, sintomas, diário de bem-estar e dashboard.

Na raiz, execute `.\scripts\start-frontend.ps1`. A interface usa `http://localhost:5173` e encaminha `/api` ao backend local na porta 8080.

```powershell
npm ci
npm run lint
npm run build
```

Consulte o [README principal](../README.md) para configurar banco e backend. Sessões usam cookies do servidor; não há armazenamento de tokens em `localStorage`.
