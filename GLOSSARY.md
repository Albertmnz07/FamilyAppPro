# Family App

Single-context glossary for the family app: how the domain names people, families, membership, consent, and visibility around DNI sharing.

## Language

**Persona**:
A person identified by a unique DNI number. A persona can belong to multiple families.
_Avoid_: User, Account

**DNI**:
The unique number that identifies a Persona. Required at registration, normalized to uppercase and never edited.
_Avoid_: DNI number (except the `DniNumber` Value Object inside composite `Dni`, ADR-0010), Identity number

**DNI photos**:
Optional front and back images evidencing a Persona's DNI, uploaded after registration. They are shared only while Consent is in force.
_Avoid_: DNI document, ID images

**Username**:
A persona's unique, lowercase handle (3–30 letters, digits, dots, underscores or hyphens), used to recognise the persona inside families independently of their DNI.
_Avoid_: Nick, Display name

**Family**:
A named grouping of personas (non-blank name, up to 100 characters) that can see one another's DNI while consent is in force, created by one persona who acts as admin.
_Avoid_: Group, Team

**Admin**:
The persona who created a family. It is the only persona allowed to issue invites and remove members, and it cannot be removed from the family.
_Avoid_: Owner, Manager

**Membership**:
The link between a persona and a family, with lifecycle `accepted → removed`; acceptance is the act of granting consent for that persona in that family, and removal revokes it.
_Avoid_: Relationship, Link

**Consent**:
A persona's grant to a family to share the persona's DNI number and any DNI photos. Held per family, withheld by default, revocable immediately, re-grantable on re-acceptance.
_Avoid_: Sharing, Grant

**Visibility**:
A current member's ability to see a member's DNI number and any DNI photos, active only while Consent is in force.
_Avoid_: Look

**Invite**:
A family-scoped, single-use, time-bound token (code) that a persona redeems to become a member and grant consent.
_Avoid_: Code (except the 6-character `InviteCode` Value Object inside `Invite`, ADR-0010), Ticket
