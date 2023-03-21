CREATE DATABASE mysql_async_tests;
USE mysql_async_tests;
CREATE TABLE mysql_async_tests.transaction_test (id VARCHAR(255) NOT NULL, PRIMARY KEY (id));
CREATE USER 'mysql_async'@'127.0.0.1' IDENTIFIED BY 'root';
CREATE USER 'mysql_async_sha256'@'127.0.0.1' IDENTIFIED WITH sha256_password; ;
SET old_passwords = 2;
SET PASSWORD FOR 'mysql_async_sha256'@'127.0.0.1' = PASSWORD('sha256_password');
CREATE USER 'mysql_async_nopw'@'127.0.0.1';
GRANT ALL PRIVILEGES ON *.* TO 'mysql_async'@'127.0.0.1';
GRANT ALL PRIVILEGES ON *.* TO 'mysql_async_sha256'@'127.0.0.1';
FLUSH PRIVILEGES;
GRANT ALL PRIVILEGES ON *.* TO 'mysql_async_nopw'@'127.0.0.1';
SET GLOBAL sql_mode = 'ALLOW_INVALID_DATES';
