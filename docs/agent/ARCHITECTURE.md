# OrtakPay — Mimari Dokümantasyonu

## 1. Genel Sistem Mimarisi

İki bağımsız servis, RabbitMQ üzerinden asenkron haberleşir. Servisler arasında
senkron (REST) çağrı yoktur — bu bilinçli bir tercihtir: Core Service'in yanıt
süresi Notification Service'in ayakta olup olmamasına bağlı olmamalıdır.

```mermaid
flowchart LR
    subgraph Client
        FE[Web / Mobile Client]
    end

    subgraph CoreService["Core Service"]
        API[REST API]
        SEC["Spring Security (JWT)"]
        BIZ[Business Logic]
        API --> SEC --> BIZ
    end

    subgraph NotifService["Notification Service"]
        LISTENER[Event Listener]
        SENDER[Notification Sender]
        LISTENER --> SENDER
    end

    CoreDB[(PostgreSQL<br/>core_db)]
    NotifDB[(PostgreSQL<br/>notif_db)]
    MQ[[RabbitMQ]]
    EXT[/Email / Push Provider/]

    FE -->|HTTPS / REST + JWT| API
    BIZ -->|JPA| CoreDB
    BIZ -->|publish event| MQ
    MQ -->|consume event| LISTENER
    LISTENER --> NotifDB
    SENDER --> EXT
```

## 2. Veri Modeli (ER Diyagramı)

```mermaid
erDiagram
    USER ||--o{ GROUP_MEMBER : "joins"
    GROUP ||--o{ GROUP_MEMBER : "has"
    GROUP ||--o{ EXPENSE : "contains"
    USER ||--o{ EXPENSE : "pays"
    EXPENSE ||--o{ EXPENSE_SHARE : "splits into"
    USER ||--o{ EXPENSE_SHARE : "owes"
    GROUP ||--o{ BALANCE : "tracks"
    USER ||--o{ BALANCE : "has"
    GROUP ||--o{ SETTLEMENT : "records"
    USER ||--o{ SETTLEMENT : "pays or receives"

    USER {
        uuid id PK
        string email
        string passwordHash
        string displayName
    }
    GROUP {
        uuid id PK
        string name
        uuid createdBy FK
    }
    GROUP_MEMBER {
        uuid groupId FK
        uuid userId FK
        datetime joinedAt
    }
    EXPENSE {
        uuid id PK
        uuid groupId FK
        uuid paidBy FK
        decimal amount
        string description
        string splitType
    }
    EXPENSE_SHARE {
        uuid id PK
        uuid expenseId FK
        uuid userId FK
        decimal owedAmount
    }
    BALANCE {
        uuid id PK
        uuid groupId FK
        uuid userId FK
        decimal netAmount
        int version
    }
    SETTLEMENT {
        uuid id PK
        uuid groupId FK
        uuid fromUser FK
        uuid toUser FK
        decimal amount
        datetime settledAt
    }
```

## 3. Akış: Masraf Oluşturma (Expense Creation)

Bu akış, projenin en önemli teknik konusunu gösterir: **transaction sınırı +
optimistic locking ile concurrency kontrolü.**

```mermaid
sequenceDiagram
    actor U as Kullanıcı
    participant API as Core API
    participant SVC as ExpenseService
    participant DB as PostgreSQL
    participant MQ as RabbitMQ
    participant NOTIF as Notification Service

    U->>API: POST /groups/{id}/expenses
    API->>SVC: createExpense(request)
    SVC->>SVC: Payları hesapla (SplitStrategy)
    SVC->>DB: BEGIN TRANSACTION
    SVC->>DB: INSERT Expense + ExpenseShare(s)
    SVC->>DB: UPDATE Balance (@Version kontrolü)

    alt Version çakışması (eşzamanlı güncelleme)
        DB-->>SVC: OptimisticLockException
        SVC-->>API: 409 Conflict
        API-->>U: "Bakiye güncellendi, tekrar deneyin"
    else Başarılı
        SVC->>DB: COMMIT
        SVC->>MQ: publish ExpenseCreatedEvent
        SVC-->>API: 201 Created
        API-->>U: ExpenseResponse
        MQ->>NOTIF: consume ExpenseCreatedEvent
        NOTIF->>NOTIF: Grup üyelerine bildirim gönder
    end
```

## 4. Neden Bu Kararlar? (Mülakat Notları)

| Karar | Alternatif | Neden bu seçildi |
|---|---|---|
| RabbitMQ | Kafka | Yüksek throughput / event replay ihtiyacı yok; sadece güvenilir task delivery gerekiyor |
| Materialized Balance tablosu | Her istekte on-the-fly hesaplama | Gerçek concurrency/transaction problemi yaratıp optimistic locking'i doğal şekilde göstermek için |
| Optimistic locking | Pessimistic locking | Çakışma olasılığı düşük (aynı gruba aynı anda yazma nadir); pessimistic lock gereksiz throughput kaybı yaratır |
| Servisler arası paylaşılan JAR yok | Ortak "common" kütüphane | Coupling'i azaltmak; her servis bağımsız evrimleşebilsin |
| Monorepo | Çoklu repo | Portfolyo projesi için geliştirme/versiyonlama kolaylığı; servisler runtime'da yine bağımsız |
| Testcontainers | H2 in-memory DB | H2, production DB (Postgres) ile davranışsal farklar gösterebilir; Testcontainers gerçek Postgres kullanır |
