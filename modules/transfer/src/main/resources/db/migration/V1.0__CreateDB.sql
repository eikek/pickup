-- the device is the pickup instance; it currently contains only one
-- row. Multiple pickups using one db is not supported atm.
CREATE TABLE device (
  device_id varchar(190) not null primary key,
  hostkeypair text not null,
  sshkeypair text not null,
  password text not null,
  enabled boolean not null,
  `insertion` timestamp NOT NULL
);

-- all peers that upload to us
CREATE TABLE incoming_peer (
  peer_id varchar(190) not null primary key,
  description text not null,
  public_key text not null,
  enabled boolean not null,
  connection_count bigint not null,
  last_connection timestamp,
  `insertion` timestamp NOT NULL
);

-- all peers we send our data to
CREATE TABLE outgoing_peer (
  peer_id varchar(190) not null primary key,
  remote_uri varchar(190) not null,
  schedule varchar(190),
  description text not null,
  enabled boolean not null,
  connection_count bigint not null,
  last_connection timestamp,
  `insertion` timestamp NOT NULL
);
