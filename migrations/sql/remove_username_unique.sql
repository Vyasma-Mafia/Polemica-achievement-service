alter table public.users
    drop constraint users_username_key;

create index users_username_key on users (username);

