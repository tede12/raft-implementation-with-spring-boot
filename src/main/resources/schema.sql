CREATE TABLE IF NOT EXISTS node_state
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    node_id
    VARCHAR
(
    255
) NOT NULL UNIQUE,
    state VARCHAR
(
    255
) NOT NULL,
    current_term INT NOT NULL,
    voted_for VARCHAR
(
    255
)
    );

CREATE TABLE IF NOT EXISTS log_entry
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    term
    INT
    NOT
    NULL,
    command
    VARCHAR
(
    255
)
    );