-- A preflight availability check alone cannot prevent simultaneous duplicate registrations.
-- If legacy data conflicts, fail the migration so it can be reviewed; never discard a customer.
CREATE UNIQUE INDEX ux_users_email_ignore_case ON users (lower(email));

-- One namespace for password, Google, and staff usernames, including concurrent writes.
-- Matching remains case sensitive, consistently with the existing authentication queries.
CREATE TABLE login_names (
    username varchar(255) PRIMARY KEY,
    owner_table varchar(32) NOT NULL,
    owner_id bigint NOT NULL,
    UNIQUE (owner_table, owner_id)
);
INSERT INTO login_names SELECT username, 'users', id FROM users WHERE username IS NOT NULL;
INSERT INTO login_names SELECT username, 'staff_accounts', id FROM staff_accounts;

CREATE FUNCTION maintain_login_name() RETURNS trigger LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.username IS NOT DISTINCT FROM NEW.username THEN
        RETURN NEW;
    END IF;
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        DELETE FROM login_names WHERE owner_table = TG_TABLE_NAME AND owner_id = OLD.id;
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') AND NEW.username IS NOT NULL THEN
        INSERT INTO login_names (username, owner_table, owner_id)
        VALUES (NEW.username, TG_TABLE_NAME, NEW.id);
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER users_login_name AFTER INSERT OR UPDATE OF username OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION maintain_login_name();
CREATE TRIGGER staff_login_name AFTER INSERT OR UPDATE OF username OR DELETE ON staff_accounts
    FOR EACH ROW EXECUTE FUNCTION maintain_login_name();
