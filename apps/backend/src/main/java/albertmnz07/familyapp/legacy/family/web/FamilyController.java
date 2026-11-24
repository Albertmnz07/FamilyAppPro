package albertmnz07.familyapp.legacy.family.web;

import albertmnz07.familyapp.legacy.family.Dni;
import albertmnz07.familyapp.legacy.family.Family;
import albertmnz07.familyapp.legacy.family.FamilyService;
import albertmnz07.familyapp.legacy.family.Invite;
import albertmnz07.familyapp.legacy.family.Persona;
import albertmnz07.familyapp.legacy.family.PersonaService;
import albertmnz07.familyapp.legacy.family.error.ForbiddenException;
import albertmnz07.familyapp.legacy.family.error.NotFoundException;
import albertmnz07.familyapp.legacy.family.photo.PhotoStore;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@RestController
@Validated
@Tag(name = "Families")
public class FamilyController {

    private final FamilyService families;
    private final PersonaService personas;
    private final PhotoStore photos;

    public FamilyController(FamilyService families, PersonaService personas, PhotoStore photos) {
        this.families = families;
        this.personas = personas;
        this.photos = photos;
    }

    @PostMapping("/api/v1/families")
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyResponse create(
            @ActingPersona Persona creator, @Valid @RequestBody CreateFamilyRequest request) {
        return FamilyResponse.of(families.createFamily(creator, request.name()));
    }

    @GetMapping("/api/v1/me/families")
    public Page<FamilyResponse> mine(
            @ActingPersona Persona persona, @PageableDefault(size = 20) Pageable pageable) {
        return families.listFamiliesFor(persona, pageable).map(FamilyResponse::of);
    }

    @GetMapping("/api/v1/families/{familyId}")
    public FamilyResponse get(@ActingPersona Persona viewer, @PathVariable UUID familyId) {
        Family family = families.getFamily(familyId);
        families.requireAcceptedMember(family, viewer);
        return FamilyResponse.of(family);
    }

    @GetMapping("/api/v1/families/{familyId}/members")
    public Page<MemberResponse> members(
            @ActingPersona Persona viewer,
            @PathVariable UUID familyId,
            @PageableDefault(size = 20) Pageable pageable) {
        Family family = families.getFamily(familyId);
        families.requireAcceptedMember(family, viewer);
        return families.listAcceptedMembers(family, pageable)
                .map(membership ->
                        MemberResponse.of(
                                membership,
                                families.isConsentInForce(family, membership.getPersona())));
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
    public void removeMember(
            @ActingPersona Persona actor,
            @PathVariable UUID familyId,
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
    public void revokeConsent(
            @ActingPersona Persona actor,
            @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        if (!actor.getId().equals(personaId)) {
            throw new ForbiddenException("A persona can only revoke its own consent");
        }
        Family family = families.getFamily(familyId);
        families.revokeConsent(family, actor);
    }

    @GetMapping("/api/v1/families/{familyId}/members/{personaId}/dni")
    public DniResponse dni(
            @ActingPersona Persona viewer,
            @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        Dni dni = requireVisibleDni(familyId, viewer, personaId);
        return DniResponse.of(familyId, personaId, dni);
    }

    @GetMapping("/api/v1/families/{familyId}/members/{personaId}/dni/front")
    public ResponseEntity<byte[]> frontPhoto(
            @ActingPersona Persona viewer,
            @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        Dni dni = requireVisibleDni(familyId, viewer, personaId);
        return photos.get(dni.frontPhotoKey())
                .map(photo -> PhotoResponses.of(photo, dni.frontPhotoContentType()))
                .orElseThrow(() -> new NotFoundException("Front photo not found"));
    }

    @GetMapping("/api/v1/families/{familyId}/members/{personaId}/dni/back")
    public ResponseEntity<byte[]> backPhoto(
            @ActingPersona Persona viewer,
            @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        Dni dni = requireVisibleDni(familyId, viewer, personaId);
        return photos.get(dni.backPhotoKey())
                .map(photo -> PhotoResponses.of(photo, dni.backPhotoContentType()))
                .orElseThrow(() -> new NotFoundException("Back photo not found"));
    }

    private Dni requireVisibleDni(UUID familyId, Persona viewer, UUID personaId) {
        Family family = families.getFamily(familyId);
        families.requireAcceptedMember(family, viewer);
        Persona target = personas.find(personaId);
        if (!families.isAcceptedMember(family, target)) {
            throw new NotFoundException("Member not found in family: " + personaId);
        }
        return families.visibleDni(family, viewer, target)
                .orElseThrow(
                        () -> new ForbiddenException("Consent is not in force for this persona"));
    }
}
