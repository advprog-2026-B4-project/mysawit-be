#!/usr/bin/env python3
"""
Generate JWT tokens for all load test users and write to CSV.

Usage:
    python3 k6/generate-tokens.py > k6/data/test_data.csv

Requirements:
    pip install psycopg2-binary PyJWT python-dotenv

The script reads .env from the mysawit-be root, connects directly to the DB,
and signs tokens using the same algorithm/secret as JwtService.java
(HS256, Keys.hmacShaKeyFor(secret.getBytes(UTF_8))).

JWT TTL = 24 hours (86400s). Safe to pre-generate before starting the suite.
"""

import csv
import sys
import os
import time
import re

try:
    import jwt
    import psycopg2
    from dotenv import dotenv_values
except ImportError as e:
    print(f"[ERROR] Missing dependency: {e}", file=sys.stderr)
    print("Run: pip install psycopg2-binary PyJWT python-dotenv", file=sys.stderr)
    sys.exit(1)

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ENV_PATH = os.path.join(SCRIPT_DIR, '..', '.env')

env = dotenv_values(ENV_PATH)
if not env:
    print(f"[WARN] .env not found at {ENV_PATH}, using defaults", file=sys.stderr)

DB_URL       = env.get('DB_URL',            'jdbc:postgresql://localhost:5432/mysawit')
DB_USERNAME  = env.get('DB_USERNAME',       'postgres')
DB_PASSWORD  = env.get('DB_PASSWORD',       'postgres')
JWT_SECRET   = env.get('JWT_SECRET',        'change-me-in-production-min-256-bits')
JWT_EXP_MS   = int(env.get('JWT_EXPIRATION_MS', '86400000'))

# ---------------------------------------------------------------------------
# Parse JDBC URL → psycopg2 connection params
# jdbc:postgresql://host:port/dbname
# ---------------------------------------------------------------------------
def parse_jdbc(url: str):
    url = url.replace('jdbc:postgresql://', '')
    match = re.match(r'([^:/]+)(?::(\d+))?/(\S+)', url)
    if not match:
        raise ValueError(f"Cannot parse JDBC URL: {url}")
    host   = match.group(1)
    port   = int(match.group(2)) if match.group(2) else 5432
    dbname = match.group(3).split('?')[0]  # strip query params
    return host, port, dbname

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
host, port, dbname = parse_jdbc(DB_URL)

try:
    conn = psycopg2.connect(
        host=host, port=port, dbname=dbname,
        user=DB_USERNAME, password=DB_PASSWORD,
        connect_timeout=10
    )
except psycopg2.OperationalError as e:
    print(f"[ERROR] Cannot connect to DB: {e}", file=sys.stderr)
    sys.exit(1)

cur = conn.cursor()

# Query all load test users with their kebun_id and mandor_id
cur.execute("""
    SELECT
        u.user_id::TEXT,
        u.role,
        COALESCE(
            CASE u.role
                WHEN 'MANDOR' THEN (SELECT k.kebun_id::TEXT FROM kebun k WHERE k.mandor_id = u.user_id LIMIT 1)
                WHEN 'BURUH'  THEN (SELECT k.kebun_id::TEXT FROM kebun k WHERE k.mandor_id = u.mandor_id LIMIT 1)
                WHEN 'SUPIR'  THEN (SELECT k.kebun_id::TEXT FROM kebun k
                                    JOIN kebun_supir ks ON ks.kebun_id = k.kebun_id
                                    WHERE ks.supir_id = u.user_id LIMIT 1)
            END,
        '') AS kebun_id,
        COALESCE(u.mandor_id::TEXT, '') AS mandor_id
    FROM users u
    WHERE u.role IN ('BURUH', 'SUPIR', 'MANDOR')
      AND (u.username LIKE 'buruh_%' OR u.username LIKE 'supir_%' OR u.username LIKE 'mandor_%')
    ORDER BY u.role, u.username
""")

rows = cur.fetchall()
cur.close()
conn.close()

now_sec = int(time.time())
exp_sec = now_sec + (JWT_EXP_MS // 1000)
secret_bytes = JWT_SECRET.encode('utf-8')

writer = csv.writer(sys.stdout, lineterminator='\n')
writer.writerow(['userId', 'role', 'token', 'kebunId', 'mandorId'])

count = 0
for user_id, role, kebun_id, mandor_id in rows:
    payload = {
        'sub':  user_id,
        'role': role,
        'iat':  now_sec,
        'exp':  exp_sec,
    }
    token = jwt.encode(payload, secret_bytes, algorithm='HS256')
    if isinstance(token, bytes):
        token = token.decode('utf-8')
    writer.writerow([user_id, role, token, kebun_id, mandor_id])
    count += 1

print(f"[INFO] Generated {count} tokens. Expires in {JWT_EXP_MS // 3600000}h.", file=sys.stderr)
