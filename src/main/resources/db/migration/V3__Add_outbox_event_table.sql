create table outbox_event
(
    id           bigint auto_increment primary key,
    event_type   varchar(64)  not null,
    payload      text         not null,
    status       varchar(16)  not null,
    attempts     int          not null default 0,
    last_error   varchar(1024),
    created_at   timestamp    not null,
    processed_at timestamp
);

create index idx_outbox_status on outbox_event (status);
