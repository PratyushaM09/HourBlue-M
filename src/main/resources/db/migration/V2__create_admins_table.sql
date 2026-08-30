create table admins (
    id bigint not null auto_increment,
    email varchar(254) not null,
    password_hash varchar(100) not null,
    enabled boolean not null default true,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint pk_admins primary key (id),
    constraint uk_admins_email unique (email)
);
