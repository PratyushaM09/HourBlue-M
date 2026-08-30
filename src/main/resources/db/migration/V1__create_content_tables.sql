create table categories (
    id bigint not null auto_increment,
    name varchar(120) not null,
    slug varchar(120) not null,
    description varchar(255),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint pk_categories primary key (id),
    constraint uk_categories_name unique (name),
    constraint uk_categories_slug unique (slug)
);

create table posts (
    id bigint not null auto_increment,
    category_id bigint not null,
    slug varchar(160) not null,
    title varchar(160) not null,
    description text not null,
    image_url varchar(2048) not null,
    cloudinary_public_id varchar(255),
    alt_text varchar(255) not null,
    source_url varchar(2048),
    status varchar(20) not null default 'DRAFT',
    published_at datetime(6),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint pk_posts primary key (id),
    constraint uk_posts_slug unique (slug),
    constraint fk_posts_category foreign key (category_id) references categories (id) on delete restrict,
    constraint chk_posts_status check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

create index idx_posts_status_published_at on posts (status, published_at);
create index idx_posts_category_status_published_at on posts (category_id, status, published_at);
