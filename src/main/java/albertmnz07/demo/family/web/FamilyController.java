package albertmnz07.demo.family.web;

import albertmnz07.demo.family.Dni;
import albertmnz07.demo.family.Family;
import albertmnz07.demo.family.FamilyService;
import albertmnz07.demo.family.Invite;
import albertmnz07.demo.family.Membership;
import albertmnz07.demo.family.Persona;
import albertmnz07.demo.family.PersonaService;
import albertmnz07.demo.family.error.ForbiddenException;
import albertmnz07.demo.family.error.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@Validated
@Tag(name = "Families")
public class FamilyController {

    private final FamilyService families;
    private final PersonaService personas;

    public FamilyController(FamilyService families, PersonaService personas) {
        this.families = families;
        this.personas = personas;
    }

    @PostMapping("/api/v1/families")
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyResponse create(@ActingPersona Persona creator,
            @Valid @RequestBody CreateFamilyRequest request) {
        return FamilyResponse.of(families.createFamily(creator, request.name()));
    }

    @GetMapping("/api/v1/me/families")
    public List<FamilyResponse> mine(@ActingPersona Persona persona) {
        return families.listFamiliesFor(persona).stream()
                .map(FamilyResponse::of)
                .toList();
    }

    @GetMapping("/api/v1/families/{familyId}")
    public FamilyResponse get(@ActingPersona Persona viewer, @PathVariable UUID familyId) {
        Family family = families.getFamily(familyId);
        families.requireAcceptedMember(family, viewer);
        return FamilyResponse.of(family);
    }

    @GetMapping("/api/v1/families/{familyId}/members")
    public List<MemberResponse> members(@ActingPersona Persona viewer, @PathVariable UUID familyId) {
        Family family = families.getFamily(familyId);
        families.requireAcceptedMember(family, viewer);
        return families.listAcceptedMembers(family).stream()
                .map(membership -> MemberResponse.of(membership,
                        families.isConsentInForce(family, membership.getPersona())))
                .toList();
    }

    @PostMapping("/api/v1/families/{familyId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse issueInvite(@ActingPersona Persona actor, @PathVariable UUID familyId) {
        Family family = families.getFamily(familyId);
        families.requireAdmin(family, actor);
        Invite invite = families.issueInvite(family);
        return InviteResponse.of(invite);
    }

    @DeleteMapping("/api/v1/families/{familyId}/members/{personaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@ActingPersona Persona actor, @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        Family family = families.getFamily(familyId);
        families.requireAdmin(family, actor);
        Persona target = personas.find(personaId);
        if (!families.isAcceptedMember(family, target)) {
            throw new NotFoundException("Member not found in family: " + personaId);
        }
        families.removeMember(family, target);
    }

    @DeleteMapping("/api/v1/families/{familyId}/members/{personaId}/consent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeConsent(@ActingPersona Persona actor, @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        if (!actor.getId().equals(personaId)) {
            throw new ForbiddenException("A persona can only revoke its own consent");
        }
        Family family = families.getFamily(familyId);
        families.revokeConsent(family, actor);
    }

    @GetMapping("/api/v1/families/{familyId}/members/{personaId}/dni")
    public DniResponse dni(@ActingPersona Persona viewer, @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        Dni dni = requireVisibleDni(familyId, viewer, personaId);
        return DniResponse.of(familyId, personaId, dni);
    }

    @GetMapping("/api/v1/families/{familyId}/members/{personaId}/dni/front")
    public ResponseEntity<byte[]> frontPhoto(@ActingPersona Persona viewer, @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        Dni dni = requireVisibleDni(familyId, viewer, personaId);
        return PhotoResponses.of(dni.frontPhoto(), dni.frontPhotoContentType());
    }

    @GetMapping("/api/v1/families/{familyId}/members/{personaId}/dni/back")
    public ResponseEntity<byte[]> backPhoto(@ActingPersona Persona viewer, @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        Dni dni = requireVisibleDni(familyId, viewer, personaId);
        return PhotoResponses.of(dni.backPhoto(), dni.backPhotoContentType());
    }

    private Dni requireVisibleDni(UUID familyId, Persona viewer, UUID personaId) {
        Family family = families.getFamily(familyId);
        families.requireAcceptedMember(family, viewer);
        Persona target = personas.find(personaId);
        if (!families.isAcceptedMember(family, target)) {
            throw new NotFoundException("Member not found in family: " + personaId);
        }
        return families.visibleDni(family, viewer, target)
                .orElseThrow(() -> new ForbiddenException("Consent is not in force for this persona"));
    }
}
