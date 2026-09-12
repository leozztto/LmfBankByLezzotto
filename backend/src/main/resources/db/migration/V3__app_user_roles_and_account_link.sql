-- Papéis e escopo por conta (ADR 0010): admin enxerga tudo, usuário comum só
-- a própria conta. O vínculo usuário -> conta é manual (admin), nunca automático
-- na abertura de conta (POST /accounts continua sem relação com quem está logado).

alter table app_user
    add column role varchar(255) not null default 'USER' check (role in ('ADMIN', 'USER'));

alter table app_user
    add column account_id bigint unique references account;

-- 'demo' (seed de V2) vira o exemplo de usuário comum, sem conta vinculada
-- até um admin usar POST /admin/users/{username}/account.

insert into app_user (username, password_hash, role, created_at)
values ('admin', '$2a$10$DYdxcgtNsmFuptRvEVv3G.Qk5BTLtv4U4uF2mGchfcMVdUH2QrSKa', 'ADMIN', now());
