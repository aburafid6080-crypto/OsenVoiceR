-- Run this SQL once in the Supabase SQL Editor.
create table if not exists public.app_settings (
  id text primary key check (id = 'main'),
  premium_enabled boolean not null default false,
  premium_password_salt text,
  premium_password_hash text,
  premium_password_iterations integer,
  premium_password_changed_at timestamptz
);
insert into public.app_settings (id) values ('main') on conflict (id) do nothing;
create table if not exists public.premium_users (
  user_id text primary key,
  premium boolean not null default false,
  updated_at timestamptz not null default now()
);
alter table public.app_settings enable row level security;
alter table public.premium_users enable row level security;
-- No anon/authenticated policies: only the server-side service role can access these tables.
