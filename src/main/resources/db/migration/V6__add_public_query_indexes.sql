create index idx_posts_mood_status_published_at on posts (mood, status, published_at);
create index idx_posts_status_slug on posts (status, slug);
