create table today_moments (
    id bigint not null auto_increment,
    feature_date date not null,
    post_id bigint not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint pk_today_moments primary key (id),
    constraint uk_today_moments_feature_date unique (feature_date),
    constraint fk_today_moments_post foreign key (post_id) references posts (id) on delete restrict
);
