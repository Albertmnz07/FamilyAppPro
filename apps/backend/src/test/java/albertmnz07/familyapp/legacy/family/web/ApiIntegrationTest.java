package albertmnz07.familyapp.legacy.family.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import albertmnz07.familyapp.infrastructure.persistence.AuthTokenJpaRepository;
import albertmnz07.familyapp.legacy.family.ConsentRepository;
import albertmnz07.familyapp.legacy.family.Family;
import albertmnz07.familyapp.legacy.family.FamilyRepository;
import albertmnz07.familyapp.legacy.family.Invite;
import albertmnz07.familyapp.legacy.family.InviteRepository;
import albertmnz07.familyapp.legacy.family.MembershipRepository;
import albertmnz07.familyapp.legacy.family.Persona;
import albertmnz07.familyapp.legacy.family.PersonaRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final String PASSWORD = "password123";
    private static final String PHOTOS_PATH = "/api/v1/personas/me/dni/photos";
    private static final int TEN_MB = 10 * 1024 * 1024;

    private static final String ADMIN_DNI = "12345678Z";
    private static final String MEMBER_DNI = "11111111H";
    private static final String OUTSIDER_DNI = "22222222J";

    @Autowired MockMvc mvc;

    @Autowired PersonaRepository personas;

    @Autowired FamilyRepository families;

    @Autowired InviteRepository invites;

    @Autowired MembershipRepository memberships;

    @Autowired ConsentRepository consents;

    @Autowired AuthTokenJpaRepository tokens;

    @AfterEach
    void cleanUp() {
        tokens.deleteAll();
        consents.deleteAll();
        memberships.deleteAll();
        invites.deleteAll();
        families.deleteAll();
        personas.deleteAll();
    }

    @Test
    void registersAPersonaWithoutPhotos() throws Exception {
        UUID personaId = registerPersona(ADMIN_DNI);
        Persona persona = personas.findById(personaId).orElseThrow();

        assertThat(persona.getFrontPhotoKey()).isNull();
        assertThat(persona.getBackPhotoKey()).isNull();
        assertThat(persona.getFrontPhotoContentType()).isNull();
        assertThat(persona.getBackPhotoContentType()).isNull();

        mvc.perform(get("/api/v1/personas/me").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(usernameFor(ADMIN_DNI)))
                .andExpect(jsonPath("$.dni").value(ADMIN_DNI))
                .andExpect(jsonPath("$.frontPhotoUrl").value(nullValue()))
                .andExpect(jsonPath("$.backPhotoUrl").value(nullValue()));

        mvc.perform(get("/api/v1/personas/me/photos/front").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/personas/me/photos/back").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsADuplicateDni() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(registerRequest("another-user", ADMIN_DNI))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("duplicate-dni"));
    }

    @Test
    void rejectsADuplicateUsername() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(registerRequest(usernameFor(ADMIN_DNI), OUTSIDER_DNI))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("duplicate-username"));
    }

    @Test
    void acceptsADniWithoutTheSpanishFormat() throws Exception {
        mvc.perform(registerRequest("plain-dni-user", "DNI-SIN-FORMATO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value("DNI-SIN-FORMATO"));
    }

    @Test
    void normalizesTheDniToUppercase() throws Exception {
        mvc.perform(registerRequest("mixed-case-user", "  abc-123  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value("ABC-123"));
    }

    @Test
    void uploadsOneDniPhotoSideAndLeavesTheOtherAbsent() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(
                        multipart(HttpMethod.PUT, PHOTOS_PATH)
                                .header(AUTH, bearer(ADMIN_DNI))
                                .file(photo("frontPhoto", "image/jpeg", (byte) 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frontPhotoUrl").value("/api/v1/personas/me/photos/front"))
                .andExpect(jsonPath("$.backPhotoUrl").value(nullValue()));

        mvc.perform(get("/api/v1/personas/me/photos/back").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadsBothDniPhotoSides() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(
                        multipart(HttpMethod.PUT, PHOTOS_PATH)
                                .header(AUTH, bearer(ADMIN_DNI))
                                .file(photo("frontPhoto", "image/jpeg", (byte) 1))
                                .file(photo("backPhoto", "image/png", (byte) 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frontPhotoUrl").value("/api/v1/personas/me/photos/front"))
                .andExpect(jsonPath("$.backPhotoUrl").value("/api/v1/personas/me/photos/back"));

        mvc.perform(get("/api/v1/personas/me/photos/front").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"));
        mvc.perform(get("/api/v1/personas/me/photos/back").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void uploadingOneSidePreservesTheOther() throws Exception {
        registerPersona(ADMIN_DNI);
        mvc.perform(
                        multipart(HttpMethod.PUT, PHOTOS_PATH)
                                .header(AUTH, bearer(ADMIN_DNI))
                                .file(photo("frontPhoto", "image/jpeg", (byte) 1)))
                .andExpect(status().isOk());

        mvc.perform(
                        multipart(HttpMethod.PUT, PHOTOS_PATH)
                                .header(AUTH, bearer(ADMIN_DNI))
                                .file(photo("backPhoto", "image/png", (byte) 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frontPhotoUrl").value("/api/v1/personas/me/photos/front"))
                .andExpect(jsonPath("$.backPhotoUrl").value("/api/v1/personas/me/photos/back"));
    }

    @Test
    void rejectsAnOversizedPhotoWithoutDiscardingTheOtherSide() throws Exception {
        registerPersona(ADMIN_DNI);
        byte[] originalFront = {1, 1, 1};
        mvc.perform(
                        multipart(HttpMethod.PUT, PHOTOS_PATH)
                                .header(AUTH, bearer(ADMIN_DNI))
                                .file(
                                        new MockMultipartFile(
                                                "frontPhoto",
                                                "front.jpg",
                                                "image/jpeg",
                                                originalFront)))
                .andExpect(status().isOk());

        mvc.perform(
                        multipart(HttpMethod.PUT, PHOTOS_PATH)
                                .header(AUTH, bearer(ADMIN_DNI))
                                .file(
                                        new MockMultipartFile(
                                                "frontPhoto",
                                                "front.jpg",
                                                "image/jpeg",
                                                new byte[] {2, 2, 2}))
                                .file(
                                        new MockMultipartFile(
                                                "backPhoto",
                                                "back.jpg",
                                                "image/jpeg",
                                                new byte[TEN_MB + 1])))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.code").value("photo-too-large"));

        MvcResult storedFront =
                mvc.perform(get("/api/v1/personas/me/photos/front").header(AUTH, bearer(ADMIN_DNI)))
                        .andExpect(status().isOk())
                        .andReturn();
        assertThat(storedFront.getResponse().getContentAsByteArray()).isEqualTo(originalFront);
        mvc.perform(get("/api/v1/personas/me/photos/back").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnUnsupportedDniPhotoType() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(
                        multipart(HttpMethod.PUT, PHOTOS_PATH)
                                .header(AUTH, bearer(ADMIN_DNI))
                                .file(
                                        new MockMultipartFile(
                                                "frontPhoto",
                                                "notes.pdf",
                                                "application/pdf",
                                                new byte[] {1})))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("unsupported-photo-type"));
    }

    @Test
    void uploadingDniPhotosRequiresAuthentication() throws Exception {
        mvc.perform(
                        multipart(HttpMethod.PUT, PHOTOS_PATH)
                                .file(photo("frontPhoto", "image/jpeg", (byte) 1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void rejectsAShortPassword() throws Exception {
        mvc.perform(
                        multipart("/api/v1/personas")
                                .param("username", "someone")
                                .param("dni", ADMIN_DNI)
                                .param("password", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"));
    }

    @Test
    void requiresAValidBearerToken() throws Exception {
        mvc.perform(get("/api/v1/me/families"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));

        mvc.perform(get("/api/v1/me/families").header(AUTH, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/me/families").header(AUTH, "not-bearer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIssuesATokenThatIdentifiesThePersona() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(get("/api/v1/personas/me").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(usernameFor(ADMIN_DNI)))
                .andExpect(jsonPath("$.dni").value(ADMIN_DNI));
    }

    @Test
    void loginRejectsAWrongPassword() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\""
                                                + usernameFor(ADMIN_DNI)
                                                + "\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void loginRejectsAnUnknownUsername() throws Exception {
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"nobody\",\"password\":\""
                                                + PASSWORD
                                                + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void loginRequiresUsernameAndPassword() throws Exception {
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"));
    }

    @Test
    void creatingAFamilyMakesTheCreatorItsAdminAndMember() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");

        mvc.perform(get("/api/v1/me/families").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(familyId.toString()))
                .andExpect(
                        jsonPath("$.content[0].adminPersonaId")
                                .value(adminOf(familyId).toString()));

        mvc.perform(get("/api/v1/families/" + familyId).header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Familia García"));

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].personaId").value(adminOf(familyId).toString()))
                .andExpect(jsonPath("$.content[0].username").value(usernameFor(ADMIN_DNI)))
                .andExpect(jsonPath("$.content[0].admin").value(true))
                .andExpect(jsonPath("$.content[0].consentInForce").value(true))
                .andExpect(jsonPath("$.content[0].dni").value(ADMIN_DNI));
    }

    @Test
    void rejectsABlankFamilyName() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(
                        post("/api/v1/families")
                                .header(AUTH, bearer(ADMIN_DNI))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"));
    }

    @Test
    void onlyTheAdminCanIssueInvites() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        joinFamily(familyId, MEMBER_DNI);
        registerPersona(OUTSIDER_DNI);

        mvc.perform(
                        post("/api/v1/families/" + familyId + "/invites")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/v1/families/" + familyId + "/invites")
                                .header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/v1/families/" + familyId + "/invites")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.familyId").value(familyId.toString()));
    }

    @Test
    void redeemingAnInviteMakesThePersonaVisible() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].dni").value(MEMBER_DNI))
                .andExpect(jsonPath("$.content[1].dni").value(ADMIN_DNI));

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(MEMBER_DNI))
                .andExpect(jsonPath("$.frontPhotoUrl").value(nullValue()))
                .andExpect(jsonPath("$.backPhotoUrl").value(nullValue()));

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni/front")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anOutsiderCannotSeeAFamilyOrItsMembers() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(get("/api/v1/families/" + familyId).header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + outsiderId + "/dni")
                                .header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPersonaOutsideTheFamilyCannotBeInspected() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + outsiderId + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokingConsentHidesTheDniFromOtherMembers() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + memberId + "/consent")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isNoContent());

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].personaId").value(memberId.toString()))
                .andExpect(jsonPath("$.content[0].consentInForce").value(false))
                .andExpect(jsonPath("$.content[0].dni").value(nullValue()));

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPersonaCannotRevokeSomebodyElsesConsent() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + memberId + "/consent")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isForbidden());
    }

    @Test
    void removingAMemberRevokesAccess() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + memberId)
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNoContent());

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/v1/families/" + familyId).header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isForbidden());
    }

    @Test
    void theAdminCannotBeRemoved() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + adminOf(familyId))
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("cannot-remove-admin"));
    }

    @Test
    void anInviteCanOnlyBeRedeemedOnce() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        String code = issueInvite(ADMIN_DNI, familyId);
        registerPersona(MEMBER_DNI);
        redeem(code, MEMBER_DNI);
        registerPersona(OUTSIDER_DNI);

        mvc.perform(
                        post("/api/v1/invites/" + code + "/redemption")
                                .header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invite-already-redeemed"));
    }

    @Test
    void anExpiredInviteIsRejected() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        Family family = families.findById(familyId).orElseThrow();
        invites.save(new Invite("EXPIRE", family, Instant.now().minusSeconds(60)));
        registerPersona(OUTSIDER_DNI);

        mvc.perform(post("/api/v1/invites/EXPIRE/redemption").header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invite-expired"));
    }

    @Test
    void anUnknownInviteIsNotFound() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(post("/api/v1/invites/ZZZZZZ/redemption").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound());
    }

    @Test
    void membersAreOrderedAlphabeticallyByDni() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        joinFamily(familyId, MEMBER_DNI);

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dni").value(MEMBER_DNI))
                .andExpect(jsonPath("$.content[1].dni").value(ADMIN_DNI));
    }

    @Test
    void membersListingIsPagedAndStaysOrderedByDniWithinThePage() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        joinFamily(familyId, MEMBER_DNI);

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(ADMIN_DNI))
                                .param("page", "1")
                                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].dni").value(ADMIN_DNI))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void familyListingIsPagedAndKeepsTheNameOrderWithinThePage() throws Exception {
        registerPersona(ADMIN_DNI);
        createFamily(ADMIN_DNI, "Zúñiga");
        createFamily(ADMIN_DNI, "Alfaro");

        mvc.perform(
                        get("/api/v1/me/families")
                                .header(AUTH, bearer(ADMIN_DNI))
                                .param("page", "1")
                                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Zúñiga"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void pagingUsesTheDefaultPageZeroAndSizeTwentyWhenNoPagingParamsAreSent() throws Exception {
        registerPersona(ADMIN_DNI);
        for (int i = 0; i < 21; i++) {
            createFamily(ADMIN_DNI, "Familia " + i);
        }

        mvc.perform(get("/api/v1/me/families").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void aPageSizeAboveTheCapIsClampedToTheMaximum() throws Exception {
        registerPersona(ADMIN_DNI);
        createFamily(ADMIN_DNI, "Familia García");

        mvc.perform(get("/api/v1/me/families").header(AUTH, bearer(ADMIN_DNI)).param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void aClientSortParameterIsIgnoredAndOrderingStaysFixedServerSide() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        joinFamily(familyId, MEMBER_DNI);

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(ADMIN_DNI))
                                .param("sort", "dni,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dni").value(MEMBER_DNI))
                .andExpect(jsonPath("$.content[1].dni").value(ADMIN_DNI))
                .andExpect(jsonPath("$.pageable.sort.sorted").value(false));
    }

    @Test
    void exposesTheOpenApiDescription() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/personas'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/families/{familyId}/members'].get").exists());
    }

    @Test
    void allowsCorsFromTheLocalFrontEnd() throws Exception {
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(
                                        "/api/v1/families")
                                .header("Origin", "http://localhost:5173")
                                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void allowsCorsPutForDniPhotoUploads() throws Exception {
        mvc.perform(
                        options(PHOTOS_PATH)
                                .header("Origin", "http://localhost:5173")
                                .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")));
    }

    private MockMultipartFile photo(String part, String contentType, byte fill) {
        return new MockMultipartFile(
                part, part + ".jpg", contentType, new byte[] {fill, fill, fill});
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            registerRequest(String username, String dni) {
        return multipart("/api/v1/personas")
                .param("username", username)
                .param("dni", dni)
                .param("password", PASSWORD);
    }

    private UUID registerPersona(String dni) throws Exception {
        MvcResult result =
                mvc.perform(registerRequest(usernameFor(dni), dni))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.username").value(usernameFor(dni)))
                        .andExpect(jsonPath("$.dni").value(dni))
                        .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private String usernameFor(String dni) {
        return "user-" + dni.toLowerCase(java.util.Locale.ROOT);
    }

    private String bearer(String dni) throws Exception {
        MvcResult result =
                mvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\""
                                                        + usernameFor(dni)
                                                        + "\",\"password\":\""
                                                        + PASSWORD
                                                        + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private UUID createFamily(String dni, String name) throws Exception {
        MvcResult result =
                mvc.perform(
                                post("/api/v1/families")
                                        .header(AUTH, bearer(dni))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"" + name + "\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private String issueInvite(String dni, UUID familyId) throws Exception {
        MvcResult result =
                mvc.perform(
                                post("/api/v1/families/" + familyId + "/invites")
                                        .header(AUTH, bearer(dni)))
                        .andExpect(status().isCreated())
                        .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.code");
    }

    private void redeem(String code, String dni) throws Exception {
        mvc.perform(post("/api/v1/invites/" + code + "/redemption").header(AUTH, bearer(dni)))
                .andExpect(status().isOk());
    }

    private UUID joinFamily(UUID familyId, String dni) throws Exception {
        UUID personaId = registerPersona(dni);
        String code = issueInvite(ADMIN_DNI, familyId);
        redeem(code, dni);
        return personaId;
    }

    private UUID adminOf(UUID familyId) {
        return families.findById(familyId).orElseThrow().getAdmin().getId();
    }
}
