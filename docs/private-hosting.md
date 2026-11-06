# Private deployment

The Android client still uses Matrix. A small private group can run a Synapse homeserver
on the owner's computer; phones connect to that server. The computer must remain awake
and connected for message delivery and sync. This client change does not deploy a server.

For this Windows machine, a Linux VM or WSL2 with the official Synapse container,
PostgreSQL, and an HTTPS reverse proxy is a practical deployment option. Choose the
server name before creating accounts: it forms their Matrix IDs and cannot be changed
in-place. Use invitation-only accounts; leave public self-registration disabled.

For access outside the home, choose either reachable HTTPS with a stable hostname,
or a private VPN on every participant's device. Router port forwarding depends on
the ISP; carrier-grade NAT can prevent direct inbound IPv4. A private-only service
does not require inviting remote Matrix servers or exposing federation endpoints.
Do not expose the database or the Synapse administration API to the public network.

Back up PostgreSQL, uploaded media, the server signing key and configuration. Clients'
encryption recovery keys are separate: retaining the server backup does not decrypt
end-to-end encrypted messages. Keep account verification and recovery in the app.

Voice/video calls and reliable background push require additional services and testing;
installing Synapse alone does not establish these capabilities. The private Android
build removes analytics/crash-upload providers and simplifies the primary navigation.

Official references:
- https://element-hq.github.io/synapse/latest/setup/installation.html
- https://element-hq.github.io/synapse/latest/reverse_proxy.html
- https://element-hq.github.io/synapse/latest/postgres.html
