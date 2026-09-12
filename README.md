# OrtakPay

OrtakPay, arkadaş grupları arasında ortak masrafları (kira, seyahat, yemek vb.)
adil şekilde paylaştırmayı sağlayan bir masraf paylaşım (expense-splitting)
platformudur. Splitwise benzeri bir ürünün backend'ini, portfolyo kalitesinde
ve mülakatta savunulabilir mühendislik kararlarıyla üretmeyi hedefleyen bir
öğrenme projesidir.

Mimari kararlar, veri modeli ve akış diyagramları için bkz.
[ARCHITECTURE.md](./ARCHITECTURE.md); tekil teknik kararların gerekçeleri için
bkz. [docs/adr/](./docs/adr/README.md). Geliştirme kuralları ve faz yol haritası
için bkz. [AGENTS.md](./AGENTS.md).

## Proje Yapısı

```
ortakpay/
├── core-service/           # Kullanıcı, grup, masraf, bakiye, settlement
├── notification-service/   # RabbitMQ event tüketimi, bildirim gönderimi
├── docker-compose.yml      # Postgres x2 + RabbitMQ
└── .env.example
```

İki servis de bağımsız Spring Boot uygulamalarıdır (ayrı `pom.xml`, ayrı
veritabanı, ayrı deploy). Aralarında senkron REST çağrısı yoktur; haberleşme
RabbitMQ üzerinden asenkron event'lerle yapılır.

## Teknoloji

Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security, PostgreSQL,
Flyway, Spring AMQP (RabbitMQ), MapStruct, springdoc-openapi, Maven.

## Nasıl Çalıştırılır

### 1. Ortam değişkenlerini ayarla

```bash
cp .env.example .env
# .env içindeki şifreleri gerektiği gibi değiştir
```

### 2. Altyapıyı ayağa kaldır (Postgres x2 + RabbitMQ)

```bash
docker-compose up -d
```

Bu komut şunları başlatır:
- `postgres-core` → `core_db` (varsayılan port `5434`; `5432` yerine bilinçli
  olarak farklı bir port kullanılıyor çünkü çoğu geliştirme makinesinde yerel
  bir Postgres kurulumu zaten `5432`'yi dinliyor olabilir — Docker'ın port
  binding'i bununla çakışırsa istekler yanlışlıkla yerel Postgres'e gider)
- `postgres-notification` → `notif_db` (varsayılan port `5433`)
- `rabbitmq` → AMQP `5672`, management UI `15672`
  (`http://localhost:15672`, kullanıcı adı/şifre `.env`'den)

### 3. Servisleri çalıştır

Her servis kendi bağımsız Maven projesidir. `application.yml` veritabanı
bağlantısını ortam değişkenlerinden okur, bu yüzden `.env` içindeki
değerlerin shell'e export edilmiş olması (ya da IDE run config'inde
tanımlanmış olması) gerekir.

```bash
# Core Service (port 8080)
cd core-service
mvn spring-boot:run

# Notification Service (port 8081)
cd notification-service
mvn spring-boot:run
```

### Build

```bash
cd core-service && mvn clean install
cd notification-service && mvn clean install
```

> Not: Faz 0 itibarıyla henüz entity/migration/endpoint yok — bu sadece boş
> iskeletin derlendiğini ve Spring context'inin (DB/MQ bağlantısı dahil)
> sorunsuz ayağa kalktığını doğrular. Bu yüzden build/test öncesi
> `docker-compose up` ile altyapının ayakta olması gerekir.

## Faz Durumu

Bkz. [AGENTS.md → Faz Yol Haritası](./AGENTS.md#faz-yol-haritası). Şu an
**Faz 0** (repo iskeleti + Docker Compose) tamamlanmıştır.
