WITH ranked_payrolls AS (
    SELECT
        payroll_id,
        user_id,
        role,
        reference_id,
        reference_type,
        ROW_NUMBER() OVER (
            PARTITION BY user_id, role, reference_id, reference_type
            ORDER BY
                CASE status
                    WHEN 'APPROVED' THEN 1
                    WHEN 'PENDING' THEN 2
                    WHEN 'REJECTED' THEN 3
                    ELSE 4
                END,
                processed_at DESC NULLS LAST,
                created_at DESC NULLS LAST,
                payroll_id DESC
        ) AS row_num
    FROM payrolls
),
duplicate_payrolls AS (
    SELECT
        payroll_id AS duplicate_payroll_id,
        user_id,
        role,
        reference_id,
        reference_type
    FROM ranked_payrolls
    WHERE row_num > 1
),
canonical_payrolls AS (
    SELECT
        payroll_id AS canonical_payroll_id,
        user_id,
        role,
        reference_id,
        reference_type
    FROM ranked_payrolls
    WHERE row_num = 1
)
UPDATE wallet_transactions wt
SET payroll_id = cp.canonical_payroll_id
FROM duplicate_payrolls dp
JOIN canonical_payrolls cp
    ON cp.user_id = dp.user_id
   AND cp.role = dp.role
   AND cp.reference_id = dp.reference_id
   AND cp.reference_type = dp.reference_type
WHERE wt.payroll_id = dp.duplicate_payroll_id;

WITH ranked_payrolls AS (
    SELECT
        payroll_id,
        ROW_NUMBER() OVER (
            PARTITION BY user_id, role, reference_id, reference_type
            ORDER BY
                CASE status
                    WHEN 'APPROVED' THEN 1
                    WHEN 'PENDING' THEN 2
                    WHEN 'REJECTED' THEN 3
                    ELSE 4
                END,
                processed_at DESC NULLS LAST,
                created_at DESC NULLS LAST,
                payroll_id DESC
        ) AS row_num
    FROM payrolls
)
DELETE FROM payrolls p
USING ranked_payrolls rp
WHERE p.payroll_id = rp.payroll_id
  AND rp.row_num > 1;

ALTER TABLE payrolls
    ADD CONSTRAINT uk_payrolls_user_role_reference
    UNIQUE (user_id, role, reference_id, reference_type);
