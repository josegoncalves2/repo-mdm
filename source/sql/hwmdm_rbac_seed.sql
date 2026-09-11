-- HWMDM RBAC seed.
-- Idempotent: safe to run multiple times on an existing Headwind MDM database.

BEGIN;

SELECT setval('permissions_id_seq', COALESCE((SELECT MAX(id) FROM permissions), 1), TRUE);
SELECT setval('userroles_id_seq', COALESCE((SELECT MAX(id) FROM userroles), 1), TRUE);
SELECT setval('userrolesettings_id_seq', COALESCE((SELECT MAX(id) FROM userrolesettings), 1), TRUE);

WITH required_permissions(name, description) AS (
    VALUES
        ('device.profile.view', 'View the device profile and configuration metadata'),
        ('device.profile.edit', 'Edit the device profile and configuration metadata'),
        ('device.remote_access.view', 'View remote access module and device remote status'),
        ('device.remote_access.control', 'Control devices remotely and send remote support actions'),
        ('device.gps.view', 'View GPS map and device locations'),
        ('device.kiosk.edit', 'Edit kiosk mode settings'),
        ('device.lifecycle.lock', 'Lock device screen or kiosk session'),
        ('device.lifecycle.unlock', 'Unlock device screen or kiosk session'),
        ('device.lifecycle.reboot', 'Reboot managed devices'),
        ('device.lifecycle.wipe', 'Wipe managed devices'),
        ('device.lifecycle.reset', 'Reset managed devices'),
        ('device.logs.view', 'View device logs'),
        ('device.photos.upload', 'Upload or manage device photos'),
        ('device.network.filter', 'Manage device network filtering'),
        ('device.ldap.sync', 'Synchronize devices or users with LDAP'),
        ('device.export_import', 'Export and import devices'),
        ('device.contacts.manage', 'Manage device contacts, warranty and support metadata'),
        ('device.branding.edit', 'Edit portal, APK and launcher branding')
)
INSERT INTO permissions (name, description, superadmin)
SELECT name, description, FALSE
FROM required_permissions
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE permissions.name = required_permissions.name
);

WITH required_roles(name, description, superadmin) AS (
    VALUES
        ('Helpdesk 1', 'View devices and perform lock/unlock support actions', FALSE),
        ('Helpdesk 2', 'Helpdesk 1 plus device reboot support actions', FALSE),
        ('Helpdesk 3', 'Helpdesk 2 plus device wipe support actions', FALSE),
        ('Guest', 'Read-only access to own profile', FALSE)
)
INSERT INTO userroles (name, description, superadmin)
SELECT name, description, superadmin
FROM required_roles
WHERE NOT EXISTS (
    SELECT 1 FROM userroles WHERE userroles.name = required_roles.name
);

-- Admin must have every non-superadmin permission, including newly introduced granular permissions.
INSERT INTO userrolepermissions (roleid, permissionid)
SELECT r.id, p.id
FROM userroles r
CROSS JOIN permissions p
WHERE r.name = 'Admin'
  AND p.superadmin IS FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM userrolepermissions urp
      WHERE urp.roleid = r.id
        AND urp.permissionid = p.id
  );

WITH role_permission(role_name, permission_name) AS (
    VALUES
        ('Helpdesk 1', 'configurations'),
        ('Helpdesk 1', 'edit_devices'),
        ('Helpdesk 1', 'device.remote_access.view'),
        ('Helpdesk 1', 'device.lifecycle.lock'),
        ('Helpdesk 1', 'device.lifecycle.unlock'),
        ('Helpdesk 1', 'plugin_messaging_send'),

        ('Helpdesk 2', 'configurations'),
        ('Helpdesk 2', 'edit_devices'),
        ('Helpdesk 2', 'device.remote_access.view'),
        ('Helpdesk 2', 'device.lifecycle.lock'),
        ('Helpdesk 2', 'device.lifecycle.unlock'),
        ('Helpdesk 2', 'device.lifecycle.reboot'),
        ('Helpdesk 2', 'plugin_messaging_send'),

        ('Helpdesk 3', 'configurations'),
        ('Helpdesk 3', 'edit_devices'),
        ('Helpdesk 3', 'device.remote_access.view'),
        ('Helpdesk 3', 'device.remote_access.control'),
        ('Helpdesk 3', 'device.lifecycle.lock'),
        ('Helpdesk 3', 'device.lifecycle.unlock'),
        ('Helpdesk 3', 'device.lifecycle.reboot'),
        ('Helpdesk 3', 'device.lifecycle.wipe'),
        ('Helpdesk 3', 'plugin_messaging_send')
)
INSERT INTO userrolepermissions (roleid, permissionid)
SELECT r.id, p.id
FROM role_permission rp
JOIN userroles r ON r.name = rp.role_name
JOIN permissions p ON p.name = rp.permission_name
WHERE NOT EXISTS (
    SELECT 1
    FROM userrolepermissions urp
    WHERE urp.roleid = r.id
      AND urp.permissionid = p.id
);

-- New roles inherit a visible device table from Observer when available,
-- otherwise from Admin. Guest intentionally gets Observer-like visibility only.
WITH template AS (
    SELECT *
    FROM userrolesettings
    WHERE roleid = COALESCE(
        (SELECT id FROM userroles WHERE name = 'Observer'),
        (SELECT id FROM userroles WHERE name = 'Admin')
    )
),
new_roles AS (
    SELECT id AS roleid
    FROM userroles
    WHERE name IN ('Helpdesk 1', 'Helpdesk 2', 'Helpdesk 3', 'Guest')
)
INSERT INTO userrolesettings (
    roleid,
    customerid,
    columndisplayeddevicestatus,
    columndisplayeddevicedate,
    columndisplayeddevicenumber,
    columndisplayeddevicemodel,
    columndisplayeddevicepermissionsstatus,
    columndisplayeddeviceappinstallstatus,
    columndisplayeddeviceconfiguration,
    columndisplayeddeviceimei,
    columndisplayeddevicephone,
    columndisplayeddevicedesc,
    columndisplayeddevicegroup,
    columndisplayedlauncherversion,
    columndisplayedbatterylevel,
    columndisplayeddevicefilesstatus,
    columndisplayeddefaultlauncher,
    columndisplayedcustom1,
    columndisplayedcustom2,
    columndisplayedcustom3,
    columndisplayedmdmmode,
    columndisplayedkioskmode,
    columndisplayedandroidversion,
    columndisplayedenrollmentdate,
    columndisplayedserial,
    columndisplayedpublicip
)
SELECT
    nr.roleid,
    template.customerid,
    template.columndisplayeddevicestatus,
    template.columndisplayeddevicedate,
    template.columndisplayeddevicenumber,
    template.columndisplayeddevicemodel,
    template.columndisplayeddevicepermissionsstatus,
    template.columndisplayeddeviceappinstallstatus,
    template.columndisplayeddeviceconfiguration,
    template.columndisplayeddeviceimei,
    template.columndisplayeddevicephone,
    template.columndisplayeddevicedesc,
    template.columndisplayeddevicegroup,
    template.columndisplayedlauncherversion,
    template.columndisplayedbatterylevel,
    template.columndisplayeddevicefilesstatus,
    template.columndisplayeddefaultlauncher,
    template.columndisplayedcustom1,
    template.columndisplayedcustom2,
    template.columndisplayedcustom3,
    template.columndisplayedmdmmode,
    template.columndisplayedkioskmode,
    template.columndisplayedandroidversion,
    template.columndisplayedenrollmentdate,
    template.columndisplayedserial,
    template.columndisplayedpublicip
FROM new_roles nr
CROSS JOIN template
WHERE NOT EXISTS (
    SELECT 1
    FROM userrolesettings urs
    WHERE urs.roleid = nr.roleid
      AND urs.customerid = template.customerid
);

COMMIT;
