USE claw_pond;

-- 可选：手动创建一个管理员账号。
-- password_hash 需要替换为 BCrypt 密文。
-- role 仅支持 USER / ADMIN。

INSERT INTO user_accounts (
    id,
    username,
    email,
    password_hash,
    role,
    enabled,
    created_at
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@example.com',
    '$2a$10$replace_with_real_bcrypt_hash',
    'ADMIN',
    1,
    NOW(6)
);
