# AGENTS.md — OrtakPay (Masraf Paylaşım Platformu)

## Proje Amacı
Bu, portfolyo kalitesinde bir Java/Spring Boot projesidir. Amaç sadece çalışan bir
repo üretmek değil, geliştiricinin (junior/new-grad, güçlü temelleri olan) Spring
Security, JPA/Hibernate, transaction yönetimi, validation, exception handling ve
testing konularında **derinlemesine** anlayış kazanmasıdır.

## Claude Code için Davranış Kuralları
- Bir seferde tüm projeyi üretme. Her zaman tek bir adım/dosya grubu üzerinde çalış.
- Önemli bir mimari veya teknoloji kararı verirken (ör. locking stratejisi, bir
  design pattern kullanımı, bir Spring annotation'ın davranışı) **neden bu şekilde
  yaptığını kısaca açıkla**, alternatifleri belirt.
- Gereksiz soyutlama/karmaşıklık ekleme. Basit çözüm yeterliyse onu kullan.
- Kod üretirken modern Java 21 / Spring Boot 3.3+ pratiklerini kullan (record'lar,
  virtual threads gerektiğinde, `ProblemDetail`, vs.)
- Her önemli iş biriminden sonra (entity, servis metodu, endpoint) ilgili testi de
  aynı adımda yaz — "önce tüm kodu yaz, sonra testleri ekle" yapma.
- Bir adımı bitirdiğinde bana ne yaptığını ve neden öyle yaptığını 3-5 cümleyle özetle.
- Emin olmadığın bir tasarım kararında (ör. entity ilişkisi, endpoint sözleşmesi)
  varsayım yapıp ilerlemek yerine bana sor.

## Mimari
**Monorepo** — tek repo içinde iki bağımsız Spring Boot projesi (her biri kendi
`pom.xml`'ine sahip, birbirinden bağımsız build/deploy edilebilir):

```
ortakpay/
├── core-service/
│   ├── src/main/java/com/ortakpay/core/...
│   └── pom.xml
├── notification-service/
│   ├── src/main/java/com/ortakpay/notification/...
│   └── pom.xml
├── docker-compose.yml
├── AGENTS.md
├── ARCHITECTURE.md
└── README.md
```

- **Core Service**: Kullanıcı, grup, masraf, bakiye, ödeme kapatma (settlement) iş
  mantığının tamamı burada. PostgreSQL (core_db).
- **Notification Service**: Core Service'ten RabbitMQ üzerinden gelen event'leri
  dinler (grup daveti, borç hatırlatma vs.). PostgreSQL (notif_db) — sadece
  gönderim geçmişi/log için.
- Servisler arası **paylaşılan kod/JAR YOK**. Event DTO'ları her serviste ayrı
  tanımlanır (bilinçli tercih — coupling'i azaltmak için).
- Monorepo olması sadece geliştirme/versiyonlama kolaylığı sağlar; servisler
  runtime'da hâlâ tamamen bağımsız süreçlerdir (ayrı container, ayrı DB, ayrı
  deploy).

## Teknoloji Stack
| Katman | Teknoloji |
|---|---|
| Dil/Runtime | Java 21, Spring Boot 3.3+ |
| Data | Spring Data JPA + Hibernate, PostgreSQL, Flyway |
| Security | Spring Security, JJWT (JWT stateless auth) |
| Messaging | Spring AMQP (RabbitMQ) |
| Mapping | MapStruct |
| API Docs | springdoc-openapi (Swagger UI) |
| Test | JUnit 5, Mockito, Testcontainers, AssertJ |
| Infra | Docker, Docker Compose |
| Build | Maven |

## Domain Modeli (Core Service)
- `User`, `Group`, `GroupMember`, `Expense`, `ExpenseShare`, `Balance`, `Settlement`
- Split stratejileri (`EQUAL`, `EXACT`, `PERCENTAGE`) → Strategy Pattern
  (`SplitStrategy` interface)
- `Balance` tablosu **materialized** tutulur (her expense/settlement işleminde
  transactional güncellenir), `@Version` ile optimistic locking uygulanır.

## Kod Konvansiyonları
- Entity'lerde Lombok `@Getter/@Setter` kullanılabilir; DTO'larda **Java record**
  tercih edilir.
- Katmanlar: `controller` → `service` → `repository`. Controller'da iş mantığı
  OLMAYACAK.
- Global exception handling: `@RestControllerAdvice` + RFC 7807 `ProblemDetail`.
- Yetkilendirme: basit rol bazlı değil, resource-based (ör. "bu kullanıcı bu
  gruba üye mi?") kontrolü servis katmanında explicit yapılır.

## Test Kuralları
- Unit test: servis katmanı, split stratejileri (Mockito ile izole).
- Integration test: Testcontainers + gerçek PostgreSQL (H2 KULLANMA).
- Her yeni endpoint/servis metodu için en az 1 happy path + 1 edge case testi.

## Git / Commit Kuralları
- Conventional Commits formatı: `feat:`, `fix:`, `test:`, `refactor:`, `chore:`,
  `docs:`
- Her commit tek bir mantıksal birim olmalı (kod + o kodun testi aynı commit'te).
- Commit mesajı İngilizce, kısa ve açıklayıcı (ör. `feat: add optimistic locking
  to Balance entity`).
- Faz bazında feature branch aç (`feature/expense-splitting` gibi), faz bitince
  `main`'e merge et.

## Faz Yol Haritası
0. Repo yapısı + Docker Compose iskeleti (Postgres + RabbitMQ)
1. Core domain modeli + Flyway migration'ları + repository katmanı
2. Auth (JWT, register/login, Spring Security filter chain)
3. Expense oluşturma + split stratejileri + balance güncelleme (transaction +
   optimistic locking)
4. Grup yönetimi, settlement endpoint'leri, resource-based authorization
5. Testler (unit + Testcontainers integration)
6. Notification Service + RabbitMQ event akışı
7. Uçtan uca Docker Compose ile çalıştırma
8. (Stretch) AI destekli doğal dil masraf girişi
9. (Stretch) Outbox pattern, refresh token, Actuator/observability

## Önemli Not
Bu projeyi geliştiren kişi bu teknolojileri öğreniyor. Kod üretirken sadece
"çalışan" değil, mülakatta savunulabilir ve mühendislik gerekçesi olan kararlar
üret.
