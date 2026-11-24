package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.command.CommandHandler;
import albertmnz07.familyapp.application.command.CreateFamilyCommand;
import albertmnz07.familyapp.application.command.IssueInviteCommand;
import albertmnz07.familyapp.application.command.IssueInviteResult;
import albertmnz07.familyapp.application.command.RemoveMemberCommand;
import albertmnz07.familyapp.application.command.RevokeConsentCommand;
import albertmnz07.familyapp.application.query.DniPhotoView;
import albertmnz07.familyapp.application.query.FamilyView;
import albertmnz07.familyapp.application.query.GetFamilyQuery;
import albertmnz07.familyapp.application.query.GetMemberDniPhotoQuery;
import albertmnz07.familyapp.application.query.GetMemberDniQuery;
import albertmnz07.familyapp.application.query.ListFamilyMembersQuery;
import albertmnz07.familyapp.application.query.ListMyFamiliesQuery;
import albertmnz07.familyapp.application.query.MemberDniView;
import albertmnz07.familyapp.application.query.MemberView;
import albertmnz07.familyapp.application.query.PageQuery;
import albertmnz07.familyapp.application.query.PagedResult;
import albertmnz07.familyapp.application.query.QueryHandler;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.domain.valueobject.PhotoSide;
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

    private final CommandHandler<CreateFamilyCommand, FamilyView> createFamily;
    private final QueryHandler<ListMyFamiliesQuery, PagedResult<FamilyView>> listMyFamilies;
    private final QueryHandler<GetFamilyQuery, FamilyView> getFamily;
    private final CommandHandler<IssueInviteCommand, IssueInviteResult> issueInvite;
    private final QueryHandler<ListFamilyMembersQuery, PagedResult<MemberView>> listFamilyMembers;
    private final CommandHandler<RemoveMemberCommand, Void> removeMember;
    private final CommandHandler<RevokeConsentCommand, Void> revokeConsent;
    private final QueryHandler<GetMemberDniQuery, MemberDniView> getMemberDni;
    private final QueryHandler<GetMemberDniPhotoQuery, DniPhotoView> getMemberDniPhoto;

    public FamilyController(
            CommandHandler<CreateFamilyCommand, FamilyView> createFamily,
            QueryHandler<ListMyFamiliesQuery, PagedResult<FamilyView>> listMyFamilies,
            QueryHandler<GetFamilyQuery, FamilyView> getFamily,
            CommandHandler<IssueInviteCommand, IssueInviteResult> issueInvite,
            QueryHandler<ListFamilyMembersQuery, PagedResult<MemberView>> listFamilyMembers,
            CommandHandler<RemoveMemberCommand, Void> removeMember,
            CommandHandler<RevokeConsentCommand, Void> revokeConsent,
            QueryHandler<GetMemberDniQuery, MemberDniView> getMemberDni,
            QueryHandler<GetMemberDniPhotoQuery, DniPhotoView> getMemberDniPhoto) {
        this.createFamily = createFamily;
        this.listMyFamilies = listMyFamilies;
        this.getFamily = getFamily;
        this.issueInvite = issueInvite;
        this.listFamilyMembers = listFamilyMembers;
        this.removeMember = removeMember;
        this.revokeConsent = revokeConsent;
        this.getMemberDni = getMemberDni;
        this.getMemberDniPhoto = getMemberDniPhoto;
    }

    @PostMapping("/api/v1/families")
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyResponse create(
            @ActingPersona PersonaId creator, @Valid @RequestBody CreateFamilyRequest request) {
        return FamilyResponse.of(
                createFamily.handle(new CreateFamilyCommand(creator.value(), request.name())));
    }

    @GetMapping("/api/v1/me/families")
    public Page<FamilyResponse> mine(
            @ActingPersona PersonaId personaId,
            @PageableDefault(size = PageQuery.DEFAULT_SIZE) Pageable pageable) {
        PagedResult<FamilyView> result =
                listMyFamilies.handle(
                        new ListMyFamiliesQuery(
                                personaId.value(), PageResponses.queryOf(pageable)));
        return PageResponses.of(result, FamilyResponse::of);
    }

    @GetMapping("/api/v1/families/{familyId}")
    public FamilyResponse get(@ActingPersona PersonaId viewer, @PathVariable UUID familyId) {
        return FamilyResponse.of(getFamily.handle(new GetFamilyQuery(viewer.value(), familyId)));
    }

    @PostMapping("/api/v1/families/{familyId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse issueInvite(@ActingPersona PersonaId actor, @PathVariable UUID familyId) {
        return InviteResponse.of(
                issueInvite.handle(new IssueInviteCommand(actor.value(), familyId)));
    }

    @GetMapping("/api/v1/families/{familyId}/members")
    public Page<MemberResponse> members(
            @ActingPersona PersonaId viewer,
            @PathVariable UUID familyId,
            @PageableDefault(size = PageQuery.DEFAULT_SIZE) Pageable pageable) {
        PagedResult<MemberView> result =
                listFamilyMembers.handle(
                        new ListFamilyMembersQuery(
                                viewer.value(), familyId, PageResponses.queryOf(pageable)));
        return PageResponses.of(result, MemberResponse::of);
    }

    @DeleteMapping("/api/v1/families/{familyId}/members/{personaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @ActingPersona PersonaId actor,
            @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        removeMember.handle(new RemoveMemberCommand(actor.value(), familyId, personaId));
    }

    @DeleteMapping("/api/v1/families/{familyId}/members/{personaId}/consent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeConsent(
            @ActingPersona PersonaId actor,
            @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        revokeConsent.handle(new RevokeConsentCommand(actor.value(), familyId, personaId));
    }

    @GetMapping("/api/v1/families/{familyId}/members/{personaId}/dni")
    public DniResponse dni(
            @ActingPersona PersonaId viewer,
            @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        return DniResponse.of(
                getMemberDni.handle(new GetMemberDniQuery(viewer.value(), familyId, personaId)));
    }

    @GetMapping("/api/v1/families/{familyId}/members/{personaId}/dni/front")
    public ResponseEntity<byte[]> frontPhoto(
            @ActingPersona PersonaId viewer,
            @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        return memberPhoto(viewer, familyId, personaId, PhotoSide.FRONT);
    }

    @GetMapping("/api/v1/families/{familyId}/members/{personaId}/dni/back")
    public ResponseEntity<byte[]> backPhoto(
            @ActingPersona PersonaId viewer,
            @PathVariable UUID familyId,
            @PathVariable UUID personaId) {
        return memberPhoto(viewer, familyId, personaId, PhotoSide.BACK);
    }

    private ResponseEntity<byte[]> memberPhoto(
            PersonaId viewer, UUID familyId, UUID personaId, PhotoSide side) {
        DniPhotoView photo =
                getMemberDniPhoto.handle(
                        new GetMemberDniPhotoQuery(viewer.value(), familyId, personaId, side));
        return PhotoResponses.of(photo.content(), photo.contentType());
    }
}
