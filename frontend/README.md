# Frontend

OrtakPay'in Next.js (App Router, TypeScript, Tailwind CSS) tabanlı web arayüzü.

## Kurulum

```bash
npm install
cp .env.local.example .env.local
```

`.env.local` içindeki `NEXT_PUBLIC_API_BASE_URL`, core-service'in REST API'sinin
adresini gösterir (yerelde varsayılan: `http://localhost:8080`).

## Çalıştırma

```bash
npm run dev
```

Uygulama `http://localhost:3000` adresinde ayağa kalkar.

## Proje Yapısı

```
frontend/
├── app/            # App Router sayfaları/layout'ları
├── components/     # Paylaşılan React bileşenleri
├── lib/            # API client'ları, yardımcı fonksiyonlar
├── types/          # Paylaşılan TypeScript tipleri
├── hooks/          # Custom React hook'ları
└── public/
```
