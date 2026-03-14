-- Run this once if you see:
-- ERROR: 42703: column "owner_id" does not exist
--
-- It upgrades older schemas that used user_id/account_id so the app's
-- owner-scoped queries and RLS policies work.
do $$
declare tbl text;
has_owner boolean;
has_user boolean;
has_account boolean;
has_fk boolean;
null_owners bigint;
begin foreach tbl in array array [
    'transactions',
    'tasks',
    'events',
    'incomes',
    'budgets',
    'recurring_templates',
    'assistant_messages'
  ] loop
select exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = tbl
      and column_name = 'owner_id'
  ) into has_owneraa;
if not has_owner then
select exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = tbl
      and column_name = 'user_id'
  ) into has_user;
select exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = tbl
      and column_name = 'account_id'
  ) into has_account;
if has_user then execute format(
  'alter table public.%I rename column user_id to owner_id',
  tbl
);
elsif has_account then execute format(
  'alter table public.%I rename column account_id to owner_id',
  tbl
);
else execute format(
  'alter table public.%I add column owner_id uuid',
  tbl
);
end if;
end if;
select exists (
    select 1
    from pg_constraint c
      join pg_class t on t.oid = c.conrelid
      join pg_namespace n on n.oid = t.relnamespace
    where n.nspname = 'public'
      and t.relname = tbl
      and c.contype = 'f'
      and c.conname = format('%s_owner_id_fkey', tbl)
  ) into has_fk;
if not has_fk then execute format(
  'alter table public.%I add constraint %I foreign key (owner_id) references auth.users(id) on delete cascade',
  tbl,
  format('%s_owner_id_fkey', tbl)
);
end if;
execute format(
  'select count(*) from public.%I where owner_id is null',
  tbl
) into null_owners;
if null_owners = 0 then execute format(
  'alter table public.%I alter column owner_id set not null',
  tbl
);
end if;
end loop;
end $$;