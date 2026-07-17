#!/bin/sh
set -eu

if [ ! -d /var/lib/mysql/mysql ]; then
  mysqld --initialize-insecure --user=mysql --datadir=/var/lib/mysql
fi

mysqld_safe --datadir=/var/lib/mysql &
until mysqladmin ping --silent; do sleep 1; done
mysql -uroot <<'SQL'
CREATE DATABASE IF NOT EXISTS jimuqu_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'jimuqu'@'%' IDENTIFIED BY 'P@ssw0rd';
GRANT ALL PRIVILEGES ON jimuqu_db.* TO 'jimuqu'@'%';
FLUSH PRIVILEGES;
SQL

exec /usr/bin/supervisord -n -c /etc/supervisor/supervisord.conf
