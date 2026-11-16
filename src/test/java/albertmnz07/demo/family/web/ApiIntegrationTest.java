package albertmnz07.demo.family.web;

import albertmnz07.demo.family.ConsentRepository;
import albertmnz07.demo.family.Family;
import albertmnz07.demo.family.FamilyRepository;
import albertmnz07.demo.family.Invite;
import albertmnz07.demo.family.InviteRepository;
import albertmnz07.demo.family.MembershipRepository;
import albertmnz07.demo.family.PersonaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String HEADER = "X-Persona-Id";
    private static final byte[] FRONT = {1, 2, 3};
    private static final byte[] BACK = {4, 5, 6};

    private static final String ADMIN_DNI = "12345678Z";
    private static final String MEMBER_DNI = "11111111H";
    private static final String OUTSIDER_DNI = "22222222J";

    @Autowired
    MockMvc mvc;

    @Autowired
    PersonaRepository personas;

    @Autowired
    FamilyRepository families;

    @Autowired
    InviteRepository invites;

    @Autowired
    MembershipRepository memberships;

    @Autowired
    ConsentRepository consents;

    @AfterEach
    void cleanUp() {
        consents.deleteAll();
        memberships.deleteAll();
        invites.deleteAll();
        families.deleteAll();
        personas.deleteAll();
    }

    @Test
    void registersAPersonaAndServesItsOwnProfile() throws Exception {
        UUID personaId = registerPersona(ADMIN_DNI);

        mvc.perform(get("/api/v1/personas/me").header(HEADER, personaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(personaId.toString()))
                .andExpect(jsonPath("$.username").value(usernameFor(ADMIN_DNI)))
                .andExpect(jsonPath("$.dni").value(ADMIN_DNI))
                .andExpect(jsonPath("$.frontPhotoUrl").value("/api/v1/personas/me/photos/front"));

        mvc.perform(get("/api/v1/personas/me/photos/front").header(HEADER, personaId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(FRONT));
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
    void rejectsAnInvalidDni() throws Exception {
        mvc.perform(registerRequest(usernameFor("12345678A"), "12345678A"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"));
    }

    @Test
    void rejectsAnUnsupportedPhotoType() throws Exception {
        mvc.perform(multipart("/api/v1/personas")
                .file(new MockMultipartFile("frontPhoto", "front.txt", "text/plain", FRONT))
                .file(new MockMultipartFile("backPhoto", "back.jpg", "image/jpeg", BACK))
                .param("username", "someone")
                .param("dni", ADMIN_DNI))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("unsupported-photo-type"));
    }

    @Test
    void requiresAValidActingPersonaHeader() throws Exception {
        mvc.perform(get("/api/v1/me/families"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));

        mvc.perform(get("/api/v1/me/families").header(HEADER, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/me/families").header(HEADER, "not-a-uuid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingAFamilyMakesTheCreatorItsAdminAndMember() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");

        mvc.perform(get("/api/v1/me/families").header(HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(familyId.toString()))
                .andExpect(jsonPath("$[0].adminPersonaId").value(adminId.toString()));

        mvc.perform(get("/api/v1/families/" + familyId).header(HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Familia García"));

        mvc.perform(get("/api/v1/families/" + familyId + "/members").header(HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].personaId").value(adminId.toString()))
                .andExpect(jsonPath("$[0].username").value(usernameFor(ADMIN_DNI)))
                .andExpect(jsonPath("$[0].admin").value(true))
                .andExpect(jsonPath("$[0].consentInForce").value(true))
                .andExpect(jsonPath("$[0].dni").value(ADMIN_DNI));
    }

    @Test
    void rejectsABlankFamilyName() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);

        mvc.perform(post("/api/v1/families")
                .header(HEADER, adminId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"));
    }

    @Test
    void onlyTheAdminCanIssueInvites() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(post("/api/v1/families/" + familyId + "/invites").header(HEADER, memberId))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/families/" + familyId + "/invites").header(HEADER, outsiderId))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/families/" + familyId + "/invites").header(HEADER, adminId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.familyId").value(familyId.toString()));
    }

    @Test
    void redeemingAnInviteMakesThePersonaVisible() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);

        mvc.perform(get("/api/v1/families/" + familyId + "/members").header(HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dni").value(MEMBER_DNI))
                .andExpect(jsonPath("$[1].dni").value(ADMIN_DNI));

        mvc.perform(get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni")
                .header(HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(MEMBER_DNI))
                .andExpect(jsonPath("$.frontPhotoUrl")
                        .value("/api/v1/families/" + familyId + "/members/" + memberId + "/dni/front"));

        mvc.perform(get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni/front")
                .header(HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(FRONT));
    }

    @Test
    void anOutsiderCannotSeeAFamilyOrItsMembers() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(get("/api/v1/families/" + familyId).header(HEADER, outsiderId))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/families/" + familyId + "/members").header(HEADER, outsiderId))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/families/" + familyId + "/members/" + adminId + "/dni")
                .header(HEADER, outsiderId))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPersonaOutsideTheFamilyCannotBeInspected() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(get("/api/v1/families/" + familyId + "/members/" + outsiderId + "/dni")
                .header(HEADER, adminId))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokingConsentHidesTheDniFromOtherMembers() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);

        mvc.perform(delete("/api/v1/families/" + familyId + "/members/" + memberId + "/consent")
                .header(HEADER, memberId))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/families/" + familyId + "/members").header(HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personaId").value(memberId.toString()))
                .andExpect(jsonPath("$[0].consentInForce").value(false))
                .andExpect(jsonPath("$[0].dni").value(nullValue()));

        mvc.perform(get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni")
                .header(HEADER, adminId))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPersonaCannotRevokeSomebodyElsesConsent() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);

        mvc.perform(delete("/api/v1/families/" + familyId + "/members/" + memberId + "/consent")
                .header(HEADER, adminId))
                .andExpect(status().isForbidden());
    }

    @Test
    void removingAMemberRevokesAccess() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);

        mvc.perform(delete("/api/v1/families/" + familyId + "/members/" + memberId)
                .header(HEADER, adminId))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/families/" + familyId + "/members").header(HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/families/" + familyId).header(HEADER, memberId))
                .andExpect(status().isForbidden());
    }

    @Test
    void theAdminCannotBeRemoved() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");

        mvc.perform(delete("/api/v1/families/" + familyId + "/members/" + adminId)
                .header(HEADER, adminId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("cannot-remove-admin"));
    }

    @Test
    void anInviteCanOnlyBeRedeemedOnce() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        String code = issueInvite(adminId, familyId);
        UUID memberId = registerPersona(MEMBER_DNI);
        redeem(code, memberId);
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(post("/api/v1/invites/" + code + "/redemption").header(HEADER, outsiderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invite-already-redeemed"));
    }

    @Test
    void anExpiredInviteIsRejected() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        Family family = families.findById(familyId).orElseThrow();
        invites.save(new Invite("EXPIRE", family, Instant.now().minusSeconds(60)));
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(post("/api/v1/invites/EXPIRE/redemption").header(HEADER, outsiderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invite-expired"));
    }

    @Test
    void anUnknownInviteIsNotFound() throws Exception {
        UUID personaId = registerPersona(ADMIN_DNI);

        mvc.perform(post("/api/v1/invites/ZZZZZZ/redemption").header(HEADER, personaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void membersAreOrderedAlphabeticallyByDni() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(adminId, "Familia García");
        joinFamily(familyId, MEMBER_DNI);

        mvc.perform(get("/api/v1/families/" + familyId + "/members").header(HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dni").value(MEMBER_DNI))
                .andExpect(jsonPath("$[1].dni").value(ADMIN_DNI));
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
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .options("/api/v1/families")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder registerRequest(
            String username, String dni) {
        return multipart("/api/v1/personas")
                .file(new MockMultipartFile("frontPhoto", "front.jpg", "image/jpeg", FRONT))
                .file(new MockMultipartFile("backPhoto", "back.jpg", "image/jpeg", BACK))
                .param("username", username)
                .param("dni", dni);
    }

    private UUID registerPersona(String dni) throws Exception {
        MvcResult result = mvc.perform(registerRequest(usernameFor(dni), dni))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(usernameFor(dni)))
                .andExpect(jsonPath("$.dni").value(dni))
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private String usernameFor(String dni) {
        return "user-" + dni.toLowerCase(java.util.Locale.ROOT);
    }

    private UUID createFamily(UUID adminId, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/families")
                .header(HEADER, adminId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private String issueInvite(UUID adminId, UUID familyId) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/families/" + familyId + "/invites")
                .header(HEADER, adminId))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.code");
    }

    private void redeem(String code, UUID personaId) throws Exception {
        mvc.perform(post("/api/v1/invites/" + code + "/redemption").header(HEADER, personaId))
                .andExpect(status().isOk());
    }

    private UUID joinFamily(UUID familyId, String dni) throws Exception {
        UUID personaId = registerPersona(dni);
        String code = issueInvite(adminOf(familyId), familyId);
        redeem(code, personaId);
        return personaId;
    }

    private UUID adminOf(UUID familyId) {
        return families.findById(familyId).orElseThrow().getAdmin().getId();
    }
}
