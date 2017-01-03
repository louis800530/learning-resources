# MySQL Note

## Connection on Console

`mysql -h192.168.1.53 -ue7life -p`

### Exit

`exit;`

## Database

### Show all Databases

`show databases;`

### Create a Database

`create database test character set utf8;`

* use **UTF-8** as default character set.

### Select a Database to Use

`use test;`

* select a database to use before accessing a table.

### Show script creating a Database

`show create database test`;

### Delete a Database

`drop database test;`

## Table

### Create a Table

```sql
create table scores (
	id int primary key auto_increment,
	exam1 float not null,
	exam2 float not null,
	exam3 float not null,
	avg float not null,
	created timestamp not null default current_timestamp,
	updated timestamp not null default current_timestamp on update current_timestamp
);
```

### Show script creating a table

`show create table test1;`

### Create an Index

`create index idx_test1_name on test1(name);`


## CRUD

### Insert

`insert into test1 (name, created, updated) values('test', now(), now());`

`insert into test1 (name) values('test2');`

### Select

`select id, name, created, updated from test1;`

`select id, name, created, updated from test1 where id = 1;`

### Update

`update test1 set name = 'test11' where id = 1;`

### Delete

`delete from test1 where id = 1;`



