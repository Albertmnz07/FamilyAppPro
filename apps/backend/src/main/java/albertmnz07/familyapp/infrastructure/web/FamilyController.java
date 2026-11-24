package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.command.CommandHandler;
import albertmnz07.familyapp.application.command.CreateFamilyCommand;
import albertmnz07.familyapp.application.command.IssueInviteCommand;
import albertmnz07.familyapp.application.command.IssueInviteResult;
import albertmnz07.familyapp.application.query.FamilyView;
import albertmnz07.familyapp.application.query.GetFamilyQuery;
import albertmnz07.familyapp.application.query.ListMyFamiliesQuery;
import albertmnz07.familyapp.application.query.PageQuery;
import albertmnz07.familyapp.application.query.PagedResult;
import albertmnz07.familyapp.application.query.QueryHandler;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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

    public FamilyController(
            CommandHandler<CreateFamilyCommand, FamilyView> createFamily,
            QueryHandler<ListMyFamiliesQuery, PagedResult<FamilyView>> listMyFamilies,
            QueryHandler<GetFamilyQuery, FamilyView> getFamily,
            CommandHandler<IssueInviteCommand, IssueInviteResult> issueInvite) {
        this.createFamily = createFamily;
        this.listMyFamilies = listMyFamilies;
        this.getFamily = getFamily;
        this.issueInvite = issueInvite;
    }

    @PostMapping("/api/v1/families")
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyResponse create(
            @Parameter(hidden = true) @ActingPersona PersonaId creator,
            @Valid @RequestBody CreateFamilyRequest request) {
        return FamilyResponse.of(
                createFamily.handle(new CreateFamilyCommand(creator.value(), request.name())));
    }

    @GetMapping("/api/v1/me/families")
    public Page<FamilyResponse> mine(
            @Parameter(hidden = true) @ActingPersona PersonaId personaId,
            @PageableDefault(size = PageQuery.DEFAULT_SIZE) Pageable pageable) {
        PagedResult<FamilyView> result =
                listMyFamilies.handle(
                        new ListMyFamiliesQuery(
                                personaId.value(), PageResponses.queryOf(pageable)));
        return PageResponses.of(result, FamilyResponse::of);
    }

    @GetMapping("/api/v1/families/{familyId}")
    public FamilyResponse get(
            @Parameter(hidden = true) @ActingPersona PersonaId viewer,
            @PathVariable UUID familyId) {
        return FamilyResponse.of(getFamily.handle(new GetFamilyQuery(viewer.value(), familyId)));
    }

    @PostMapping("/api/v1/families/{familyId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse issueInvite(
            @Parameter(hidden = true) @ActingPersona PersonaId actor, @PathVariable UUID familyId) {
        return InviteResponse.of(
                issueInvite.handle(new IssueInviteCommand(actor.value(), familyId)));
    }
}
