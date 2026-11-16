# Family App

Single-context glossary for the family app: how the domain names people, families, membership, consent, and visibility around DNI sharing.

## Language

**Persona**:
A person identified by a unique DNI, owning the DNI number and front/back photos as a single entity. A persona can belong to multiple families.
_Avoid_: User, Account

**Username**:
A persona's unique, lowercase handle (3–30 letters, digits, dots, underscores or hyphens), used to recognise the persona inside families independently of their DNI.
_Avoid_: Nick, Display name

**Family**:
A grouping of personas that can see one another's DNI while consent is in force, created by one persona who acts as admin.
_Avoid_: Group, Team

**Admin**:
The persona who created a family. It is the only persona allowed to issue invites and remove members, and it cannot be removed from the family.
_Avoid_: Owner, Manager

**Membership**:
The link between a persona and a family, with lifecycle `accepted → removed`; acceptance is the act of granting consent for that persona in that family, and removal revokes it.
_Avoid_: Relationship, Link

**Consent**:
A persona's grant to a family to share the persona's entity (DNI number plus front/back photos). Held per family, withheld by default, revocable immediately, re-grantable on re-acceptance.
_Avoid_: Sharing, Grant

**Visibility**:
A current member's ability to see a member's DNI entity, active only while consent is in force.
_Avoid_: Look

**Invite**:
A family-scoped, single-use, time-bound token (code) that a persona redeems to become a member and grant consent.
_Avoid_: Code, Ticket
