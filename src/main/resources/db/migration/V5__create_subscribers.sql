create table subscribers (
    id bigint not null auto_increment,
    email varchar(320) not null,
    created_at datetime(6) not null,
    constraint pk_subscribers primary key (id),
    constraint uk_subscribers_email unique (email)
);
