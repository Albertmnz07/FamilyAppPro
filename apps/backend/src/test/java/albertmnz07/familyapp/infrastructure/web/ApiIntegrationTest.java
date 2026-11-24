package albertmnz07.familyapp.infrastructure.web;

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

import albertmnz07.familyapp.application.port.Clock;
import com.jayway.jsonpath.JsonPath;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiIntegrationTest.ControllableClockConfig.class)
class ApiIntegrationTest {

    private static final Instant INITIAL_TIME = Instant.parse("2030-01-01T00:00:00Z");
    private static final String AUTH = "Authorization";
    private static final String PASSWORD = "password123";
    private static final String PHOTOS_PATH = "/api/v1/personas/me/dni/photos";
    private static final int TEN_MB = 10 * 1024 * 1024;

    private static final String ADMIN_DNI = "12345678Z";
    private static final String MEMBER_DNI = "11111111H";
    private static final String OUTSIDER_DNI = "22222222J";

    @Autowired MockMvc mvc;

    @Autowired JdbcTemplate jdbc;

    @Autowired ControllableClock clock;

    @TestConfiguration
    static class ControllableClockConfig {

        @Bean
        @Primary
        ControllableClock controllableClock() {
            return new ControllableClock();
        }
    }

    static final class ControllableClock implements Clock {

        private final AtomicReference<Instant> current = new AtomicReference<>(INITIAL_TIME);

        @Override
        public Instant now() {
            return current.get();
        }

        void advance(Duration duration) {
            current.updateAndGet(instant -> instant.plus(duration));
        }

        void reset() {
            current.set(INITIAL_TIME);
        }
    }

    @AfterEach
    void cleanUp() {
        clock.reset();
        jdbc.update("delete from auth_token");
        jdbc.update("delete from consent");
        jdbc.update("delete from membership");
        jdbc.update("delete from invite");
        jdbc.update("delete from family");
        jdbc.update("delete from persona");
    }

    @Test
    void registersAPersonaWithoutPhotos() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(get("/api/v1/personas/me").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(usernameFor(ADMIN_DNI)))
                .andExpect(jsonPath("$.dni").value(ADMIN_DNI))
                .andExpect(jsonPath("$.frontPhotoUrl").value(nullValue()))
                .andExpect(jsonPath("$.backPhotoUrl").value(nullValue()));

        mvc.perform(get("/api/v1/personas/me/photos/front").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
        mvc.perform(get("/api/v1/personas/me/photos/back").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
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
    void normalizesUsernameToLowercaseAndDniToUppercase() throws Exception {
        mvc.perform(registerRequest("Mixed.User_1", "  abc-123  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("mixed.user_1"))
                .andExpect(jsonPath("$.dni").value("ABC-123"));

        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"MIXED.USER_1\",\"password\":\""
                                                + PASSWORD
                                                + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mixed.user_1"));
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));

        mvc.perform(get("/api/v1/me/families").header(AUTH, "Bearer    "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));

        mvc.perform(get("/api/v1/me/families").header(AUTH, "not-bearer"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void loginIssuesATokenThatIdentifiesThePersonaUntilItExpires() throws Exception {
        registerPersona(ADMIN_DNI);
        String token = bearer(ADMIN_DNI);

        mvc.perform(get("/api/v1/personas/me").header(AUTH, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(usernameFor(ADMIN_DNI)))
                .andExpect(jsonPath("$.dni").value(ADMIN_DNI));

        clock.advance(Duration.ofDays(7).plusSeconds(1));

        mvc.perform(get("/api/v1/personas/me").header(AUTH, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
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
    void loginRejectsAnUnknownUsernameMalformedUsernameOrShortPasswordWithUnauthorized()
            throws Exception {
        registerPersona(ADMIN_DNI);

        for (String payload :
                List.of(
                        "{\"username\":\"nobody\",\"password\":\"" + PASSWORD + "\"}",
                        "{\"username\":\"!\",\"password\":\"" + PASSWORD + "\"}",
                        "{\"username\":\"" + usernameFor(ADMIN_DNI) + "\",\"password\":\"x\"}")) {
            mvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("unauthorized"));
        }
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
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");

        mvc.perform(get("/api/v1/me/families").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(familyId.toString()))
                .andExpect(jsonPath("$.content[0].adminPersonaId").value(adminId.toString()));

        mvc.perform(get("/api/v1/families/" + familyId).header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Familia García"))
                .andExpect(jsonPath("$.adminPersonaId").value(adminId.toString()));

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].personaId").value(adminId.toString()))
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
    void rejectsAnOversizedFamilyName() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(
                        post("/api/v1/families")
                                .header(AUTH, bearer(ADMIN_DNI))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"));
    }

    @Test
    void operationsTargetingAMissingFamilyReturnNotFound() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID missingFamilyId = UUID.randomUUID();
        String token = bearer(ADMIN_DNI);

        mvc.perform(get("/api/v1/families/" + missingFamilyId).header(AUTH, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
        mvc.perform(get("/api/v1/families/" + missingFamilyId + "/members").header(AUTH, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
        mvc.perform(post("/api/v1/families/" + missingFamilyId + "/invites").header(AUTH, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
        mvc.perform(
                        delete("/api/v1/families/" + missingFamilyId + "/members/" + adminId)
                                .header(AUTH, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
        mvc.perform(
                        delete(
                                        "/api/v1/families/"
                                                + missingFamilyId
                                                + "/members/"
                                                + adminId
                                                + "/consent")
                                .header(AUTH, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
        mvc.perform(
                        get("/api/v1/families/" + missingFamilyId + "/members/" + adminId + "/dni")
                                .header(AUTH, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
        mvc.perform(
                        get("/api/v1/families/"
                                        + missingFamilyId
                                        + "/members/"
                                        + adminId
                                        + "/dni/front")
                                .header(AUTH, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void myFamiliesListingIgnoresClientSortAndOrdersCaseInsensitively() throws Exception {
        registerPersona(ADMIN_DNI);
        createFamily(ADMIN_DNI, "zúñiga");
        createFamily(ADMIN_DNI, "Alfaro");

        mvc.perform(
                        get("/api/v1/me/families")
                                .header(AUTH, bearer(ADMIN_DNI))
                                .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Alfaro"))
                .andExpect(jsonPath("$.content[1].name").value("zúñiga"))
                .andExpect(jsonPath("$.pageable.sort.sorted").value(false));
    }

    @Test
    void malformedInviteTokensReturnNotFoundWhileLowercaseCodesAreNormalized() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        String code = issueInvite(ADMIN_DNI, familyId);
        UUID memberId = registerPersona(MEMBER_DNI);

        mvc.perform(post("/api/v1/invites/bad!/redemption").header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));

        mvc.perform(
                        post("/api/v1/invites/TOO-LONG-CODE/redemption")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));

        mvc.perform(
                        post("/api/v1/invites/" + code.toLowerCase(Locale.ROOT) + "/redemption")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familyId").value(familyId.toString()))
                .andExpect(jsonPath("$.personaId").value(memberId.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(
                        post("/api/v1/families/" + familyId + "/invites")
                                .header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(
                        post("/api/v1/families/" + familyId + "/invites")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.familyId").value(familyId.toString()))
                .andExpect(
                        jsonPath("$.expiresAt")
                                .value(INITIAL_TIME.plus(Duration.ofDays(7)).toString()));
    }

    @Test
    void redeemingAnInviteMakesThePersonaAndCreatorMutuallyVisible() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
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
                        get("/api/v1/families/" + familyId + "/members/" + adminId + "/dni")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(ADMIN_DNI));

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni/front")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void anOutsiderCannotSeeAFamilyOrItsMembers() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(get("/api/v1/families/" + familyId).header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + outsiderId + "/dni")
                                .header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void aPersonaOutsideTheFamilyOrMissingCannotBeInspected() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + outsiderId + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));

        mvc.perform(
                        get("/api/v1/families/"
                                        + familyId
                                        + "/members/"
                                        + UUID.randomUUID()
                                        + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void aPersonaCannotRevokeSomebodyElsesConsent() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + memberId + "/consent")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void removingAMemberRevokesAccessAndExcludesTheFamilyFromTheirList() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
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
        mvc.perform(get("/api/v1/me/families").header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/families/" + familyId).header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + adminId + "/dni")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + memberId)
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void reAcceptingAFreshInviteAfterConsentRevocationOrRemovalRestoresVisibility()
            throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID memberId = registerPersona(MEMBER_DNI);
        String initialCode = issueInvite(ADMIN_DNI, familyId);
        redeem(initialCode, MEMBER_DNI);

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + memberId + "/consent")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isNoContent());
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isForbidden());

        redeem(issueInvite(ADMIN_DNI, familyId), MEMBER_DNI);
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(MEMBER_DNI));

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + memberId)
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNoContent());
        mvc.perform(
                        post("/api/v1/invites/" + initialCode + "/redemption")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invite-already-redeemed"));
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound());

        redeem(issueInvite(ADMIN_DNI, familyId), MEMBER_DNI);
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + memberId + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(MEMBER_DNI));
        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members/" + adminId + "/dni")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(ADMIN_DNI));
    }

    @Test
    void theAdminCannotBeRemoved() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + adminId)
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
        String code = issueInvite(ADMIN_DNI, familyId);
        registerPersona(OUTSIDER_DNI);

        clock.advance(Duration.ofDays(7));

        mvc.perform(
                        post("/api/v1/invites/" + code + "/redemption")
                                .header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invite-expired"));
    }

    @Test
    void anUnknownInviteIsNotFound() throws Exception {
        registerPersona(ADMIN_DNI);

        mvc.perform(post("/api/v1/invites/ZZZZZZ/redemption").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
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
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");

        mvc.perform(get("/api/v1/me/families").header(AUTH, bearer(ADMIN_DNI)).param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content.length()").value(1));

        mvc.perform(
                        get("/api/v1/families/" + familyId + "/members")
                                .header(AUTH, bearer(ADMIN_DNI))
                                .param("size", "500"))
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
    void exposesTheOpenApiDescriptionWithoutLeakingActingPersonaParameters() throws Exception {
        Parameter parameterMeta = ActingPersona.class.getAnnotation(Parameter.class);
        assertThat(parameterMeta).isNotNull();
        assertThat(parameterMeta.hidden()).isTrue();

        MvcResult result =
                mvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.paths['/api/v1/personas'].post").exists())
                        .andExpect(
                                jsonPath("$.paths['/api/v1/families/{familyId}/members'].get")
                                        .exists())
                        .andExpect(jsonPath("$.components.schemas.Persona").doesNotExist())
                        .andExpect(jsonPath("$.components.schemas.PersonaId").doesNotExist())
                        .andReturn();

        String spec = result.getResponse().getContentAsString();
        List<Map<String, Object>> queryParameters =
                JsonPath.read(spec, "$.paths.*.*.parameters[?(@.in == 'query')]");
        assertThat(queryParameters)
                .extracting(param -> param.get("name"))
                .containsOnly("username", "dni", "password", "pageable");
    }

    @Test
    void allowsCorsFromTheLocalFrontEnd() throws Exception {
        mvc.perform(
                        options("/api/v1/families")
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

    @Test
    void streamsMemberDniPhotosWhileConsentIsInForceAndBlocksThemAfterRevocation()
            throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        UUID memberId = joinFamily(familyId, MEMBER_DNI);
        byte[] frontBytes = {7, 8, 9};
        byte[] backBytes = {4, 5, 6};

        mvc.perform(
                        multipart(HttpMethod.PUT, PHOTOS_PATH)
                                .header(AUTH, bearer(MEMBER_DNI))
                                .file(
                                        new MockMultipartFile(
                                                "frontPhoto",
                                                "front.jpg",
                                                "image/jpeg",
                                                frontBytes))
                                .file(
                                        new MockMultipartFile(
                                                "backPhoto", "back.png", "image/png", backBytes)))
                .andExpect(status().isOk());

        String base = "/api/v1/families/" + familyId + "/members/" + memberId + "/dni";
        mvc.perform(get(base).header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personaId").value(memberId.toString()))
                .andExpect(jsonPath("$.number").value(MEMBER_DNI))
                .andExpect(jsonPath("$.frontPhotoUrl").value(base + "/front"))
                .andExpect(jsonPath("$.backPhotoUrl").value(base + "/back"));

        MvcResult front =
                mvc.perform(get(base + "/front").header(AUTH, bearer(ADMIN_DNI)))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Content-Type", "image/jpeg"))
                        .andExpect(header().string("Cache-Control", containsString("no-store")))
                        .andReturn();
        assertThat(front.getResponse().getContentAsByteArray()).isEqualTo(frontBytes);

        MvcResult back =
                mvc.perform(get(base + "/back").header(AUTH, bearer(ADMIN_DNI)))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Content-Type", "image/png"))
                        .andReturn();
        assertThat(back.getResponse().getContentAsByteArray()).isEqualTo(backBytes);

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + memberId + "/consent")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isNoContent());

        mvc.perform(get(base + "/front").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(get(base + "/back").header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void revokingConsentInOneFamilyLeavesConsentInOtherFamiliesInForce() throws Exception {
        registerPersona(ADMIN_DNI);
        UUID familyA = createFamily(ADMIN_DNI, "Familia A");
        UUID memberId = joinFamily(familyA, MEMBER_DNI);

        registerPersona(OUTSIDER_DNI);
        UUID familyB = createFamily(OUTSIDER_DNI, "Familia B");
        String codeB = issueInvite(OUTSIDER_DNI, familyB);
        redeem(codeB, MEMBER_DNI);

        mvc.perform(
                        delete("/api/v1/families/" + familyA + "/members/" + memberId + "/consent")
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isNoContent());

        mvc.perform(
                        get("/api/v1/families/" + familyA + "/members/" + memberId + "/dni")
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(
                        get("/api/v1/families/" + familyB + "/members/" + memberId + "/dni")
                                .header(AUTH, bearer(OUTSIDER_DNI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(MEMBER_DNI));
    }

    @Test
    void nonAdminsCannotRemoveMembersAndMissingMembersReturnNotFound() throws Exception {
        UUID adminId = registerPersona(ADMIN_DNI);
        UUID familyId = createFamily(ADMIN_DNI, "Familia García");
        joinFamily(familyId, MEMBER_DNI);
        UUID outsiderId = registerPersona(OUTSIDER_DNI);

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + adminId)
                                .header(AUTH, bearer(MEMBER_DNI)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mvc.perform(
                        delete("/api/v1/families/" + familyId + "/members/" + outsiderId)
                                .header(AUTH, bearer(ADMIN_DNI)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
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
        return "user-" + dni.toLowerCase(Locale.ROOT);
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
}
