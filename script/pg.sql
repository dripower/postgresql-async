create database netty_driver_test;
create database netty_driver_time_test;
alter database netty_driver_time_test set timezone to 'GMT';
create table transaction_test ( id varchar(255) not null, constraint id_unique primary key (id));
CREATE USER postgres_md5 WITH PASSWORD 'postgres_md5'; GRANT ALL PRIVILEGES ON DATABASE netty_driver_test to postgres_md5;
CREATE USER postgres_cleartext WITH PASSWORD 'postgres_cleartext'; GRANT ALL PRIVILEGES ON DATABASE netty_driver_test to postgres_cleartext;
CREATE USER postgres_kerberos WITH PASSWORD 'postgres_kerberos'; GRANT ALL PRIVILEGES ON DATABASE netty_driver_test to postgres_kerberos;
\c netty_driver_test
CREATE TYPE example_mood AS ENUM ('sad', 'ok', 'happy');
