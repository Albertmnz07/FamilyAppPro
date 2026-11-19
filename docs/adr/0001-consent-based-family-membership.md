# Consent-based family membership

The domain models a **Persona** — a person identified by a unique DNI number — as the identity that can belong to many **Families**. Becoming a member of a family happens by redeeming an invite, which simultaneously creates the **Membership** and grants the family **Consent** to share the persona's DNI number and any DNI photos; consent is held per family and is revocable. The DNI number stays mandatory and unique; the front/back photos are optional evidence that can be added after registration (see ADR-0007).

We chose this over per-family accounts: the DNI identifies one real person across families, so consent must attach to the person–family pair rather than to a family-local account.
