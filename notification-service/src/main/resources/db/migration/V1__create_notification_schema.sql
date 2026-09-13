-- All three message shapes now carry the recipient's user id (GroupMemberAddedMessage
-- gained newMemberId), so recipient_user_id can be NOT NULL like recipient_email.
CREATE TABLE notification_logs (
    id                UUID         PRIMARY KEY,
    event_type        VARCHAR(50)  NOT NULL,
    recipient_user_id UUID         NOT NULL,
    recipient_email   VARCHAR(255) NOT NULL,
    payload           TEXT         NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notification_logs_recipient_email ON notification_logs (recipient_email);
