create table if not exists public.transactions (
  id bigserial primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  category text not null,
  amount double precision not null,
  occurred_at timestamptz not null default now(),
  source text not null default 'manual',
  source_hash text
);

create index if not exists idx_transactions_owner_occurred_at
  on public.transactions (owner_id, occurred_at desc);
create index if not exists idx_transactions_owner_category
  on public.transactions (owner_id, category);
create unique index if not exists idx_transactions_owner_source_hash
  on public.transactions (owner_id, source_hash)
  where source_hash is not null;

create table if not exists public.tasks (
  id bigserial primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  description text,
  completed boolean not null default false,
  due_at timestamptz,
  priority text not null default 'medium'
);

alter table public.tasks
  add column if not exists description text;

create index if not exists idx_tasks_owner_completed
  on public.tasks (owner_id, completed);
create index if not exists idx_tasks_owner_due
  on public.tasks (owner_id, due_at);

create table if not exists public.events (
  id bigserial primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  start_at timestamptz not null,
  end_at timestamptz,
  note text,
  completed boolean not null default false,
  priority text not null default 'medium',
  event_type text not null default 'general'
);

alter table public.events
  add column if not exists completed boolean not null default false;

alter table public.events
  add column if not exists priority text not null default 'medium';

alter table public.events
  add column if not exists event_type text not null default 'general';

create index if not exists idx_events_owner_start
  on public.events (owner_id, start_at);

create table if not exists public.incomes (
  id bigserial primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  amount double precision not null,
  received_at timestamptz not null default now(),
  source text not null default 'manual'
);

create index if not exists idx_incomes_owner_received
  on public.incomes (owner_id, received_at desc);

create table if not exists public.budgets (
  id bigserial primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  category text not null,
  monthly_limit double precision not null
);

create unique index if not exists idx_budgets_owner_category
  on public.budgets (owner_id, lower(category));

create table if not exists public.recurring_templates (
  id bigserial primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  kind text not null,
  title text not null,
  description text,
  category text,
  amount double precision,
  priority text,
  cadence text not null,
  next_run_at timestamptz not null,
  enabled boolean not null default true
);

create index if not exists idx_recurring_owner_next
  on public.recurring_templates (owner_id, next_run_at);

create table if not exists public.user_profile (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null,
  email text not null,
  phone text not null,
  member_since_label text not null,
  verified boolean not null default false,
  avatar_url text
);

alter table public.user_profile
  add column if not exists avatar_url text;

create table if not exists public.assistant_messages (
  id bigserial primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  text text not null,
  is_user boolean not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_assistant_messages_owner_created
  on public.assistant_messages (owner_id, created_at, id);

create table if not exists public.app_updates (
  id bigserial primary key,
  active boolean not null default true,
  latest_version text not null,
  min_supported_version text not null,
  force_update boolean not null default false,
  title text not null default 'Update Available',
  message text not null default 'A newer version of the app is available. Please update now.',
  notes text[] not null default '{}',
  apk_url text,
  website_url text,
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index if not exists idx_app_updates_active_updated
  on public.app_updates (active, updated_at desc);

alter table public.transactions enable row level security;
alter table public.tasks enable row level security;
alter table public.events enable row level security;
alter table public.user_profile enable row level security;
alter table public.assistant_messages enable row level security;
alter table public.app_updates enable row level security;
alter table public.incomes enable row level security;
alter table public.budgets enable row level security;
alter table public.recurring_templates enable row level security;

drop policy if exists "transactions_owner_rw" on public.transactions;
create policy "transactions_owner_rw"
  on public.transactions
  for all
  using (auth.uid() = owner_id)
  with check (auth.uid() = owner_id);

drop policy if exists "tasks_owner_rw" on public.tasks;
create policy "tasks_owner_rw"
  on public.tasks
  for all
  using (auth.uid() = owner_id)
  with check (auth.uid() = owner_id);

drop policy if exists "events_owner_rw" on public.events;
create policy "events_owner_rw"
  on public.events
  for all
  using (auth.uid() = owner_id)
  with check (auth.uid() = owner_id);

drop policy if exists "assistant_messages_owner_rw" on public.assistant_messages;
create policy "assistant_messages_owner_rw"
  on public.assistant_messages
  for all
  using (auth.uid() = owner_id)
  with check (auth.uid() = owner_id);

drop policy if exists "user_profile_owner_rw" on public.user_profile;
create policy "user_profile_owner_rw"
  on public.user_profile
  for all
  using (auth.uid() = id)
  with check (auth.uid() = id);

drop policy if exists "incomes_owner_rw" on public.incomes;
create policy "incomes_owner_rw"
  on public.incomes
  for all
  using (auth.uid() = owner_id)
  with check (auth.uid() = owner_id);

drop policy if exists "budgets_owner_rw" on public.budgets;
create policy "budgets_owner_rw"
  on public.budgets
  for all
  using (auth.uid() = owner_id)
  with check (auth.uid() = owner_id);

drop policy if exists "recurring_templates_owner_rw" on public.recurring_templates;
create policy "recurring_templates_owner_rw"
  on public.recurring_templates
  for all
  using (auth.uid() = owner_id)
  with check (auth.uid() = owner_id);

drop policy if exists "app_updates_public_read" on public.app_updates;
create policy "app_updates_public_read"
  on public.app_updates
  for select
  to anon, authenticated
  using (active = true);

insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do update
  set public = excluded.public;

drop policy if exists "avatars_public_read" on storage.objects;
create policy "avatars_public_read"
  on storage.objects
  for select
  using (bucket_id = 'avatars');

drop policy if exists "avatars_owner_write" on storage.objects;
create policy "avatars_owner_write"
  on storage.objects
  for all
  to authenticated
  using (
    bucket_id = 'avatars'
    and auth.uid()::text = (storage.foldername(name))[1]
  )
  with check (
    bucket_id = 'avatars'
    and auth.uid()::text = (storage.foldername(name))[1]
  );
