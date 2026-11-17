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

 
-- Owner control center extensions. Run after the original schema.
create table if not exists public.app_config (
  id text primary key check (id = 'main'),
  maintenance_mode boolean not null default false,
  force_update boolean not null default false,
  announcement text not null default '',
  updated_at timestamptz not null default now()
);
insert into public.app_config (id) values ('main') on conflict (id) do nothing;

create table if not exists public.website_projects (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  domain text,
  hosting_provider text,
  hosting_project_id text,
  status text not null default 'draft' check (status in ('draft', 'connected', 'deployed', 'paused')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.admin_audit_logs (
  id bigint generated always as identity primary key,
  actor_email text not null,
  action text not null,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

alter table public.app_config enable row level security;
alter table public.website_projects enable row level security;
alter table public.admin_audit_logs enable row level security;
-- No anon/authenticated policies: service-role access is server-only.

 
-- Separate Android App Studio project records (independent from website_projects).
create table if not exists public.app_projects (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  package_name text,
  platform text not null default 'android' check (platform = 'android'),
  status text not null default 'planning' check (status in ('planning','building','testing','ready','published','paused')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
alter table public.app_projects enable row level security;
-- No anon/authenticated policies: server service-role access only.


-- Website Studio builder fields (safe to run on an existing project).
alter table public.website_projects add column if not exists template text not null default 'portfolio' check (template in ('portfolio','business','landing','creator'));
alter table public.website_projects add column if not exists content jsonb not null default '{"headline":"Build something remarkable.","description":"Introduce your work, your ideas and what makes your project unique.","button_text":"Get started","accent":"#d8b15e"}'::jsonb;

-- OSEN TURBOPLAY anonymous device registry. The app sends only a generated UUID,
-- app/package version, Android version, model label and last-seen timestamp.
create table if not exists public.registered_devices (
  device_id uuid primary key,
  package_name text not null check (package_name = 'com.osenturboplay.app'),
  device_model text not null default 'Android device',
  android_version text not null default 'Unknown',
  app_version text not null default 'Unknown',
  first_seen timestamptz not null default now(),
  last_seen timestamptz not null default now()
);
alter table public.registered_devices enable row level security;
-- No anon/authenticated policies. Only server-side service-role access is allowed.
