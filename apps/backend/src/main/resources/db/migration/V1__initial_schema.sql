CREATE TABLE persona (
    id uuid NOT NULL,
    username character varying(30) NOT NULL,
    dni character varying(255) NOT NULL,
    password_hash character varying(255),
    front_photo bytea NOT NULL,
    back_photo bytea NOT NULL,
    front_photo_key character varying(255),
    front_photo_content_type character varying(255) NOT NULL,
    back_photo_key character varying(255),
    back_photo_content_type character varying(255) NOT NULL,
    CONSTRAINT persona_pkey PRIMARY KEY (id),
    CONSTRAINT uk_persona_username UNIQUE (username),
    CONSTRAINT uk_persona_dni UNIQUE (dni)
);

CREATE TABLE family (
    id uuid NOT NULL,
    name character varying(255),
    admin_id uuid NOT NULL,
    CONSTRAINT family_pkey PRIMARY KEY (id),
    CONSTRAINT fk_family_admin FOREIGN KEY (admin_id) REFERENCES persona (id)
);

CREATE TABLE membership (
    id uuid NOT NULL,
    family_id uuid NOT NULL,
    persona_id uuid NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT membership_pkey PRIMARY KEY (id),
    CONSTRAINT uk_membership_family_persona UNIQUE (family_id, persona_id),
    CONSTRAINT fk_membership_family FOREIGN KEY (family_id) REFERENCES family (id),
    CONSTRAINT fk_membership_persona FOREIGN KEY (persona_id) REFERENCES persona (id),
    CONSTRAINT membership_status_check CHECK (status IN ('ACCEPTED', 'REMOVED'))
);

CREATE TABLE consent (
    id uuid NOT NULL,
    family_id uuid NOT NULL,
    persona_id uuid NOT NULL,
    granted_at timestamp(6) with time zone NOT NULL,
    revoked_at timestamp(6) with time zone,
    CONSTRAINT consent_pkey PRIMARY KEY (id),
    CONSTRAINT uk_consent_family_persona UNIQUE (family_id, persona_id),
    CONSTRAINT fk_consent_family FOREIGN KEY (family_id) REFERENCES family (id),
    CONSTRAINT fk_consent_persona FOREIGN KEY (persona_id) REFERENCES persona (id)
);

CREATE TABLE invite (
    id uuid NOT NULL,
    code character varying(6) NOT NULL,
    family_id uuid NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    redeemed_at timestamp(6) with time zone,
    CONSTRAINT invite_pkey PRIMARY KEY (id),
    CONSTRAINT uk_invite_code UNIQUE (code),
    CONSTRAINT fk_invite_family FOREIGN KEY (family_id) REFERENCES family (id)
);

CREATE TABLE auth_token (
    id uuid NOT NULL,
    token character varying(255) NOT NULL,
    persona_id uuid NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT auth_token_pkey PRIMARY KEY (id),
    CONSTRAINT uk_auth_token_token UNIQUE (token),
    CONSTRAINT fk_auth_token_persona FOREIGN KEY (persona_id) REFERENCES persona (id)
);
