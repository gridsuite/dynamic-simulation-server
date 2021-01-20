CREATE TABLE IF NOT EXISTS ds.globalStatus (
    resultUuid uuid,
    status text,
    PRIMARY KEY (resultUuid)
);

CREATE TABLE IF NOT EXISTS ds.result (
    resultUuid uuid,
    result boolean,
    PRIMARY KEY (resultUuid)
);
