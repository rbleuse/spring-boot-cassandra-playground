CREATE TABLE uuid_v7_events
(
    id uuid,
    payload text,
    PRIMARY KEY ((id))
);

CREATE TABLE snowflake_events
(
    id bigint CHECK id > 0,
    payload text,
    PRIMARY KEY ((id))
);
