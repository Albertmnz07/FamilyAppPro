package albertmnz07.familyapp.infrastructure.config;

import albertmnz07.familyapp.application.command.CommandHandler;
import albertmnz07.familyapp.application.command.CreateFamilyCommand;
import albertmnz07.familyapp.application.command.CreateFamilyCommandHandler;
import albertmnz07.familyapp.application.command.IssueInviteCommand;
import albertmnz07.familyapp.application.command.IssueInviteCommandHandler;
import albertmnz07.familyapp.application.command.IssueInviteResult;
import albertmnz07.familyapp.application.command.LoginCommand;
import albertmnz07.familyapp.application.command.LoginCommandHandler;
import albertmnz07.familyapp.application.command.LoginResult;
import albertmnz07.familyapp.application.command.RedeemInviteCommand;
import albertmnz07.familyapp.application.command.RedeemInviteCommandHandler;
import albertmnz07.familyapp.application.command.RedeemInviteResult;
import albertmnz07.familyapp.application.command.RegisterPersonaCommand;
import albertmnz07.familyapp.application.command.RegisterPersonaCommandHandler;
import albertmnz07.familyapp.application.command.RemoveMemberCommand;
import albertmnz07.familyapp.application.command.RemoveMemberCommandHandler;
import albertmnz07.familyapp.application.command.RevokeConsentCommand;
import albertmnz07.familyapp.application.command.RevokeConsentCommandHandler;
import albertmnz07.familyapp.application.command.UpdateDniPhotosCommand;
import albertmnz07.familyapp.application.command.UpdateDniPhotosCommandHandler;
import albertmnz07.familyapp.application.port.Clock;
import albertmnz07.familyapp.application.port.InviteCodeGenerator;
import albertmnz07.familyapp.application.port.PasswordHasher;
import albertmnz07.familyapp.application.port.PhotoStore;
import albertmnz07.familyapp.application.query.AuthenticateTokenQuery;
import albertmnz07.familyapp.application.query.AuthenticateTokenQueryHandler;
import albertmnz07.familyapp.application.query.DniPhotoView;
import albertmnz07.familyapp.application.query.FamilyView;
import albertmnz07.familyapp.application.query.GetFamilyQuery;
import albertmnz07.familyapp.application.query.GetFamilyQueryHandler;
import albertmnz07.familyapp.application.query.GetMemberDniPhotoQuery;
import albertmnz07.familyapp.application.query.GetMemberDniPhotoQueryHandler;
import albertmnz07.familyapp.application.query.GetMemberDniQuery;
import albertmnz07.familyapp.application.query.GetMemberDniQueryHandler;
import albertmnz07.familyapp.application.query.GetMyDniPhotoQuery;
import albertmnz07.familyapp.application.query.GetMyDniPhotoQueryHandler;
import albertmnz07.familyapp.application.query.GetMyPersonaQuery;
import albertmnz07.familyapp.application.query.GetMyPersonaQueryHandler;
import albertmnz07.familyapp.application.query.ListFamilyMembersQuery;
import albertmnz07.familyapp.application.query.ListFamilyMembersQueryHandler;
import albertmnz07.familyapp.application.query.ListMyFamiliesQuery;
import albertmnz07.familyapp.application.query.ListMyFamiliesQueryHandler;
import albertmnz07.familyapp.application.query.MemberDniView;
import albertmnz07.familyapp.application.query.MemberView;
import albertmnz07.familyapp.application.query.PagedResult;
import albertmnz07.familyapp.application.query.PersonaView;
import albertmnz07.familyapp.application.query.QueryHandler;
import albertmnz07.familyapp.domain.repository.AuthTokenRepository;
import albertmnz07.familyapp.domain.repository.FamilyRepository;
import albertmnz07.familyapp.domain.repository.InviteRepository;
import albertmnz07.familyapp.domain.repository.MembershipRepository;
import albertmnz07.familyapp.domain.repository.PersonaRepository;
import albertmnz07.familyapp.domain.valueobject.PersonaId;
import albertmnz07.familyapp.infrastructure.security.Pbkdf2PasswordHasher;
import albertmnz07.familyapp.infrastructure.security.SecureRandomInviteCodeGenerator;
import albertmnz07.familyapp.infrastructure.security.SystemClock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Wires the pure application handlers to their adapters, each inside a transaction. */
@Configuration
public class UseCaseConfig {

    @Bean
    PasswordHasher passwordHasher() {
        return new Pbkdf2PasswordHasher();
    }

    @Bean
    InviteCodeGenerator secureRandomInviteCodeGenerator() {
        return new SecureRandomInviteCodeGenerator();
    }

    @Bean
    Clock clock() {
        return new SystemClock();
    }

    @Bean
    CommandHandler<RegisterPersonaCommand, PersonaView> registerPersona(
            PersonaRepository personas,
            PasswordHasher passwordHasher,
            PlatformTransactionManager transactions) {
        return transactional(
                new RegisterPersonaCommandHandler(personas, passwordHasher), transactions);
    }

    @Bean
    CommandHandler<LoginCommand, LoginResult> login(
            PersonaRepository personas,
            AuthTokenRepository tokens,
            PasswordHasher passwordHasher,
            Clock clock,
            PlatformTransactionManager transactions) {
        return transactional(
                new LoginCommandHandler(personas, tokens, passwordHasher, clock), transactions);
    }

    @Bean
    CommandHandler<UpdateDniPhotosCommand, PersonaView> updateDniPhotos(
            PersonaRepository personas,
            PhotoStore photos,
            PlatformTransactionManager transactions) {
        return transactional(new UpdateDniPhotosCommandHandler(personas, photos), transactions);
    }

    @Bean
    CommandHandler<CreateFamilyCommand, FamilyView> createFamily(
            FamilyRepository families,
            MembershipRepository memberships,
            Clock clock,
            PlatformTransactionManager transactions) {
        return transactional(
                new CreateFamilyCommandHandler(families, memberships, clock), transactions);
    }

    @Bean
    CommandHandler<IssueInviteCommand, IssueInviteResult> issueInvite(
            FamilyRepository families,
            InviteRepository invites,
            InviteCodeGenerator codeGenerator,
            Clock clock,
            PlatformTransactionManager transactions) {
        return transactional(
                new IssueInviteCommandHandler(families, invites, codeGenerator, clock),
                transactions);
    }

    @Bean
    CommandHandler<RedeemInviteCommand, RedeemInviteResult> redeemInvite(
            InviteRepository invites,
            MembershipRepository memberships,
            Clock clock,
            PlatformTransactionManager transactions) {
        return transactional(
                new RedeemInviteCommandHandler(invites, memberships, clock), transactions);
    }

    @Bean
    CommandHandler<RemoveMemberCommand, Void> removeMember(
            FamilyRepository families,
            MembershipRepository memberships,
            Clock clock,
            PlatformTransactionManager transactions) {
        return transactional(
                new RemoveMemberCommandHandler(families, memberships, clock), transactions);
    }

    @Bean
    CommandHandler<RevokeConsentCommand, Void> revokeConsent(
            FamilyRepository families,
            MembershipRepository memberships,
            Clock clock,
            PlatformTransactionManager transactions) {
        return transactional(
                new RevokeConsentCommandHandler(families, memberships, clock), transactions);
    }

    @Bean
    QueryHandler<AuthenticateTokenQuery, PersonaId> authenticateToken(
            AuthTokenRepository tokens, Clock clock, PlatformTransactionManager transactions) {
        return readOnly(new AuthenticateTokenQueryHandler(tokens, clock), transactions);
    }

    @Bean
    QueryHandler<GetMyPersonaQuery, PersonaView> getMyPersona(
            PersonaRepository personas, PlatformTransactionManager transactions) {
        return readOnly(new GetMyPersonaQueryHandler(personas), transactions);
    }

    @Bean
    QueryHandler<GetMyDniPhotoQuery, DniPhotoView> getMyDniPhoto(
            PersonaRepository personas,
            PhotoStore photos,
            PlatformTransactionManager transactions) {
        return readOnly(new GetMyDniPhotoQueryHandler(personas, photos), transactions);
    }

    @Bean
    QueryHandler<GetFamilyQuery, FamilyView> getFamily(
            FamilyRepository families,
            MembershipRepository memberships,
            PlatformTransactionManager transactions) {
        return readOnly(new GetFamilyQueryHandler(families, memberships), transactions);
    }

    @Bean
    QueryHandler<ListMyFamiliesQuery, PagedResult<FamilyView>> listMyFamilies(
            MembershipRepository memberships,
            FamilyRepository families,
            PlatformTransactionManager transactions) {
        return readOnly(new ListMyFamiliesQueryHandler(memberships, families), transactions);
    }

    @Bean
    QueryHandler<ListFamilyMembersQuery, PagedResult<MemberView>> listFamilyMembers(
            FamilyRepository families,
            MembershipRepository memberships,
            PersonaRepository personas,
            PlatformTransactionManager transactions) {
        return readOnly(
                new ListFamilyMembersQueryHandler(families, memberships, personas), transactions);
    }

    @Bean
    QueryHandler<GetMemberDniQuery, MemberDniView> getMemberDni(
            FamilyRepository families,
            MembershipRepository memberships,
            PersonaRepository personas,
            PlatformTransactionManager transactions) {
        return readOnly(
                new GetMemberDniQueryHandler(families, memberships, personas), transactions);
    }

    @Bean
    QueryHandler<GetMemberDniPhotoQuery, DniPhotoView> getMemberDniPhoto(
            FamilyRepository families,
            MembershipRepository memberships,
            PersonaRepository personas,
            PhotoStore photos,
            PlatformTransactionManager transactions) {
        return readOnly(
                new GetMemberDniPhotoQueryHandler(families, memberships, personas, photos),
                transactions);
    }

    private static <C, R> CommandHandler<C, R> transactional(
            CommandHandler<C, R> handler, PlatformTransactionManager transactions) {
        TransactionTemplate template = new TransactionTemplate(transactions);
        return command -> template.execute(status -> handler.handle(command));
    }

    private static <Q, R> QueryHandler<Q, R> readOnly(
            QueryHandler<Q, R> handler, PlatformTransactionManager transactions) {
        TransactionTemplate template = new TransactionTemplate(transactions);
        template.setReadOnly(true);
        return query -> template.execute(status -> handler.handle(query));
    }
}
