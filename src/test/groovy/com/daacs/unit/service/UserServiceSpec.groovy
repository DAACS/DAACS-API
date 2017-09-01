package com.daacs.service

import com.daacs.framework.exception.NotFoundException
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.framework.hystrix.FailureType
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.framework.serializer.DaacsOrikaMapper
import com.daacs.model.SessionedUser
import com.daacs.model.User
import com.daacs.model.UserFieldConfig
import com.daacs.model.UserSearchResult
import com.daacs.model.dto.CreateUserRequest
import com.daacs.model.dto.UpdateUserRequest
import com.daacs.model.event.UserEvent
import com.daacs.repository.EventContainerRepository
import com.daacs.repository.UserRepository
import com.lambdista.util.Try
import org.opensaml.saml2.core.Attribute
import org.opensaml.saml2.core.NameID
import org.opensaml.saml2.core.impl.AssertionBuilder
import org.opensaml.saml2.core.impl.AttributeBuilder
import org.opensaml.saml2.core.impl.NameIDBuilder
import org.opensaml.ws.wssecurity.impl.AttributedStringImpl
import org.opensaml.xml.schema.XSString
import org.springframework.security.saml.SAMLCredential
import spock.lang.Specification
import spock.lang.Unroll
/**
 * Created by chostetter on 6/22/16.
 */
class UserServiceSpec extends Specification {

    UserService userService;
    UserRepository userRepository;
    EventContainerRepository eventContainerRepository;
    DaacsOrikaMapper orikaMapper = new DaacsOrikaMapper()

    User dummyUser = new User("username", "", "Mr", "Dummy", true, ["ROLE_STUDENT"], "secondaryId", "canvasSisId");
    CreateUserRequest createUserRequest = new CreateUserRequest(username: "test", firstName: "first", lastName: "last", password: "pass", passwordConfirm: "pass", role: "ROLE_STUDENT")
    UserFieldConfig userFieldConfig

    FailureTypeException failureTypeException = new FailureTypeException("failure", "failure", FailureType.NOT_RETRYABLE, new NotFoundException(""))

    def setup(){
        userFieldConfig = new UserFieldConfig()
        dummyUser.setId(UUID.randomUUID().toString())
        userRepository = Mock(UserRepository);
        eventContainerRepository = Mock(EventContainerRepository)
        userService = new UserServiceImpl(userRepository: userRepository, userFieldConfig: userFieldConfig, eventContainerRepository: eventContainerRepository, orikaMapper: orikaMapper);
    }

    def "getUser: returns user"(){
        setup:
        String userId = UUID.randomUUID().toString()

        when:
        Try<User> maybeUser = userService.getUser(userId)

        then:
        1 * userRepository.getUser(userId) >> new Try.Success<User>(dummyUser)
        maybeUser.isSuccess()
        maybeUser.get().id == dummyUser.id
    }

    def "getUser: has failure exception when user not found"(){
        setup:
        String userId = UUID.randomUUID().toString()

        when:
        Try<User> maybeUser = userService.getUser(userId)

        then:
        1 * userRepository.getUser(_) >> new Try.Failure<User>(failureTypeException)

        then:
        maybeUser.isFailure()
        maybeUser.failed().get().getCause() instanceof NotFoundException
    }

    def "loadUserByUsername: returns SessionedUser"(){
        setup:
        String username = "user"

        when:
        SessionedUser sessionedUser = (SessionedUser) userService.loadUserByUsername(username)

        then:
        1 * userRepository.getUserByUsername(username) >> new Try.Success<User>(dummyUser)
        sessionedUser.id == dummyUser.id
    }

    def "loadUserByUsername: throws exception when user not found"(){
        setup:
        String userId = UUID.randomUUID().toString()

        when:
        userService.loadUserByUsername(userId)

        then:
        1 * userRepository.getUserByUsername(userId) >> new Try.Failure<User>(failureTypeException)
        thrown(FailureTypeException)
    }

    def "loadUserByUsername: throws exception when user is null"(){
        setup:
        String userId = UUID.randomUUID().toString()

        when:
        userService.loadUserByUsername(userId)

        then:
        1 * userRepository.getUserByUsername(userId) >> new Try.Success<User>(null)
        thrown(FailureTypeException)
    }

    def "saveUser: success"(){
        when:
        Try<User> maybeUser = userService.saveUser(dummyUser)

        then:
        1 * userRepository.saveUser(dummyUser) >> new Try.Success<User>(dummyUser)

        maybeUser.isSuccess()
        maybeUser.get() == dummyUser
    }

    def "saveUser: fails"(){
        when:
        Try<User> maybeUser = userService.saveUser(dummyUser)

        then:
        1 * userRepository.saveUser(_) >> new Try.Failure<User>(new Exception())

        maybeUser.isFailure()
    }

    def "insertUser: pass through to repo, on success returns user"(){
        when:
        Try<User> maybeUser = userService.insertUser(dummyUser)

        then:
        1 * userRepository.insertUser(dummyUser) >> new Try.Success<Void>(null)
        maybeUser.isSuccess()
        maybeUser.get() == dummyUser
    }

    def "insertUser: pass through to repo, if failure, i fail"(){
        when:
        Try<User> maybeUser = userService.insertUser(dummyUser)

        then:
        1 * userRepository.insertUser(_) >> new Try.Failure<Void>(new Exception())
        maybeUser.isFailure()
    }

    def "createUser: pass through to repo, on success returns user"(){
        when:
        Try<User> maybeUser = userService.createUser(createUserRequest)

        then:
        1 * userRepository.insertUser(_) >> new Try.Success<Void>(null)
        maybeUser.isSuccess()
        maybeUser.get().firstName == "first"
        maybeUser.get().lastName == "last"
        maybeUser.get().password == "9d4e1e23bd5b727046a9e3b4b7db57bd8d6ee684"
        maybeUser.get().roles.get(0) == "ROLE_STUDENT"
        !maybeUser.get().hasDataUsageConsent
    }

    def "createUser: pass through to repo, if failure, i fail"(){
        when:
        Try<User> maybeUser = userService.createUser(createUserRequest)

        then:
        1 * userRepository.insertUser(_) >> new Try.Failure<Void>(new Exception())
        maybeUser.isFailure()
    }

    @Unroll
    def "loadUserBySaml: builds SessionedUser successfully"(UserFieldConfig userFieldConfig, List<Attribute> attributes){
        setup:
        String userName = "user123"
        SAMLCredential credential = buildSamlCredentials(userName, attributes)
        userService = new UserServiceImpl(userRepository: userRepository, userFieldConfig: userFieldConfig, orikaMapper: orikaMapper);

        when:
        SessionedUser sessionedUser = userService.loadUserBySAML(credential);

        then:
        1 * userRepository.getUserByUsername(userName) >> new Try.Success<User>(null)
        1 * userRepository.insertUser(_) >> { args ->
            User user = args[0]
            assert user.firstName == "curt"
            assert user.lastName == "h"
            assert user.id == "userid123"
            assert user.roles.contains("ROLE_STUDENT")
            assert user.roles.contains("ROLE_ADMIN")
            assert user.roles.contains("ROLE_ADVISOR")

            return new Try.Success<Void>(null)
        }

        then:
        sessionedUser.getUsername() == userName
        sessionedUser.getId() == "userid123"
        sessionedUser.getFirstName() == "curt"
        sessionedUser.getLastName() == "h"
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_STUDENT" } != null
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_ADMIN" } != null
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_ADVISOR" } != null

        where:
        userFieldConfig                                                                                    | attributes
        new UserFieldConfig("role", "uuid", "firstName", "lastName", "admin", "advisor", "student", false, "null", null) | [buildAttribute("role", ["student", "admin", "advisor"]), buildAttribute("uuid", ["userid123"]), buildAttribute("firstName", ["curt"]), buildAttribute("lastName", ["h"]) ]
        new UserFieldConfig("groups", "userid", "first", "last", "poweruser", "teacher", "peon", false, null, null)      | [buildAttribute("groups", ["peon", "poweruser", "teacher"]), buildAttribute("userid", ["userid123"]), buildAttribute("first", ["curt"]), buildAttribute("last", ["h"]) ]
    }

    def "loadUserBySaml: useUniqueIdAttributeForLogin on new user success"(){
        setup:
        String userName = "user123"
        UserFieldConfig userFieldConfig = new UserFieldConfig("role", "uuid", "firstName", "lastName", "admin", "advisor", "student", true, null, null);
        List<Attribute> attributes = [buildAttribute("role", ["student", "admin", "advisor"]), buildAttribute("uuid", ["1234567890"]), buildAttribute("firstName", ["curt"]), buildAttribute("lastName", ["h"])]
        SAMLCredential credential = buildSamlCredentials(userName, attributes)
        userService = new UserServiceImpl(userRepository: userRepository, userFieldConfig: userFieldConfig, orikaMapper: orikaMapper);

        when:
        SessionedUser sessionedUser = userService.loadUserBySAML(credential);

        then:
        1 * userRepository.getUser("1234567890") >> new Try.Success<User>(null)
        1 * userRepository.insertUser(_) >> { args ->
            User user = args[0]
            assert user.firstName == "curt"
            assert user.lastName == "h"
            assert user.id == "1234567890"
            assert user.roles.contains("ROLE_STUDENT")
            assert user.roles.contains("ROLE_ADMIN")
            assert user.roles.contains("ROLE_ADVISOR")

            return new Try.Success<Void>(null)
        }

        then:
        sessionedUser.getUsername() == userName
        sessionedUser.getId() == "1234567890"
        sessionedUser.getFirstName() == "curt"
        sessionedUser.getLastName() == "h"
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_STUDENT" } != null
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_ADMIN" } != null
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_ADVISOR" } != null
    }

    def "loadUserBySaml: useUniqueIdAttributeForLogin on existing user success"(){
        setup:
        String userName = "user123"
        UserFieldConfig userFieldConfig = new UserFieldConfig("role", "uuid", "firstName", "lastName", "admin", "advisor", "student", true, null, null);
        List<Attribute> attributes = [buildAttribute("role", ["student", "admin", "advisor"]), buildAttribute("uuid", [dummyUser.getId()]), buildAttribute("firstName", ["curt"]), buildAttribute("lastName", ["h"])]
        SAMLCredential credential = buildSamlCredentials(userName, attributes)
        userService = new UserServiceImpl(userRepository: userRepository, userFieldConfig: userFieldConfig, orikaMapper: orikaMapper);

        when:
        SessionedUser sessionedUser = userService.loadUserBySAML(credential);

        then:
        1 * userRepository.getUser(dummyUser.getId()) >> new Try.Success<User>(dummyUser)
        1 * userRepository.saveUser(_) >> { args ->
            User user = args[0]
            assert user.firstName == "curt"
            assert user.lastName == "h"
            assert user.id == dummyUser.getId()
            assert user.roles.contains("ROLE_STUDENT")
            assert user.roles.contains("ROLE_ADMIN")
            assert user.roles.contains("ROLE_ADVISOR")

            return new Try.Success<Void>(null)
        }

        then:
        sessionedUser.getUsername() == userName
        sessionedUser.getId() == dummyUser.getId()
        sessionedUser.getFirstName() == "curt"
        sessionedUser.getLastName() == "h"
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_STUDENT" } != null
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_ADMIN" } != null
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_ADVISOR" } != null
    }

    def "loadUserBySaml: if user has rolesAreWritable = false, then do not update roles"(){
        setup:
        String userName = "user123"
        List<Attribute> attributes = [buildAttribute("role", ["student", "admin", "advisor"]), buildAttribute("uuid", [dummyUser.getId()]), buildAttribute("firstName", ["curt"]), buildAttribute("lastName", ["h"])]
        SAMLCredential credential = buildSamlCredentials(userName, attributes)
        dummyUser.setRoles(["ROLE_STUDENT"])
        dummyUser.setRolesAreWritable(false)

        when:
        SessionedUser sessionedUser = userService.loadUserBySAML(credential);

        then:
        1 * userRepository.getUserByUsername(userName) >> new Try.Success<User>(dummyUser)
        1 * userRepository.saveUser(_) >> new Try.Success<Void>(null)

        then:
        sessionedUser.getAuthorities().size() == 1
        sessionedUser.getAuthorities().find{ it.authority == "ROLE_STUDENT" } != null
    }

    def "loadUserBySaml: getUserByUsername fails, error thrown"(){
        setup:
        UserFieldConfig userFieldConfig = new UserFieldConfig("role", "uuid", "firstName", "lastName", "admin", "advisor", "student", false, null, null)
        SAMLCredential credential = buildSamlCredentials("user123", [])
        userService = new UserServiceImpl(userRepository: userRepository, userFieldConfig: userFieldConfig, orikaMapper: orikaMapper);

        when:
        userService.loadUserBySAML(credential);

        then:
        1 * userRepository.getUserByUsername("user123") >> new Try.Failure<User>(failureTypeException)
        0 * userRepository.insertUser(_)
        0 * userRepository.saveUser(_)

        then:
        thrown(FailureTypeException)
    }

    def "loadUserBySaml: insertUser fails, error thrown"(){
        setup:
        UserFieldConfig userFieldConfig = new UserFieldConfig("role", "uuid", "firstName", "lastName", "admin", "advisor", "student", false, null, null)
        SAMLCredential credential = buildSamlCredentials("user123", [])
        userService = new UserServiceImpl(userRepository: userRepository, userFieldConfig: userFieldConfig, orikaMapper: orikaMapper);

        when:
        userService.loadUserBySAML(credential);

        then:
        1 * userRepository.getUserByUsername("user123") >> new Try.Success<User>(null)
        1 * userRepository.insertUser(_) >> new Try.Failure<User>(failureTypeException)

        then:
        thrown(FailureTypeException)
    }

    def "loadUserBySaml: saveUser fails, error thrown"(){
        setup:
        UserFieldConfig userFieldConfig = new UserFieldConfig("role", "uuid", "firstName", "lastName", "admin", "advisor", "student", false, null, null)
        SAMLCredential credential = buildSamlCredentials("user123", [])
        userService = new UserServiceImpl(userRepository: userRepository, userFieldConfig: userFieldConfig, orikaMapper: orikaMapper);

        when:
        userService.loadUserBySAML(credential);

        then:
        1 * userRepository.getUserByUsername("user123") >> new Try.Success<User>(dummyUser)
        1 * userRepository.saveUser(_) >> new Try.Failure<User>(failureTypeException)

        then:
        thrown(FailureTypeException)
    }

    def "loadUserBySaml: no role attribute set"(){
        setup:
        userFieldConfig = new UserFieldConfig(null, "uuid", "firstName", "lastName", "admin", "advisor", "student", false, null, null);
        userService = new UserServiceImpl(userRepository: userRepository, userFieldConfig: userFieldConfig, orikaMapper: orikaMapper);

        String userName = "user123"
        SAMLCredential credential = buildSamlCredentials(userName, [
                buildAttribute("role", ["student"]),
                buildAttribute("uuid", ["userid123"]),
                buildAttribute("firstName", ["curt"]),
                buildAttribute("lastName", ["h"]) ])

        when:
        SessionedUser sessionedUser = userService.loadUserBySAML(credential);

        then:
        1 * userRepository.getUserByUsername(userName) >> new Try.Success<User>(null)
        1 * userRepository.insertUser(_) >> { args ->
            User user = args[0]
            assert user.firstName == "curt"
            assert user.lastName == "h"
            assert user.id == "userid123"
            assert user.roles.size() == 0

            return new Try.Success<Void>(null)
        }

        then:
        sessionedUser.getAuthorities().size() == 0
    }

    def "loadUserBySaml: no uuid attribute set, defaults to username"(){
        setup:
        userFieldConfig = new UserFieldConfig("role", null, "firstName", "lastName", "admin", "advisor", "student", false, null, null);
        userService = new UserServiceImpl(userRepository: userRepository, userFieldConfig: userFieldConfig, orikaMapper: orikaMapper);

        String userName = "user123"
        SAMLCredential credential = buildSamlCredentials(userName, [
                buildAttribute("role", ["student"]),
                buildAttribute("uuid", ["userid123"]),
                buildAttribute("firstName", ["curt"]),
                buildAttribute("lastName", ["h"]) ])

        when:
        SessionedUser sessionedUser = userService.loadUserBySAML(credential);

        then:
        1 * userRepository.getUserByUsername(userName) >> new Try.Success<User>(null)
        1 * userRepository.insertUser(_) >> { args ->
            User user = args[0]
            assert user.firstName == "curt"
            assert user.lastName == "h"
            assert user.id == "user123"
            assert user.username == "user123"
            assert user.roles.contains("ROLE_STUDENT")

            return new Try.Success<Void>(null)
        }

        then:
        sessionedUser.getId() == userName
    }

    def "recordEvent: success"(){
        setup:
        UserEvent userEvent = new UserEvent()

        when:
        Try<Void> maybeResults = userService.recordEvent(dummyUser.getId(), userEvent)

        then:
        1 * eventContainerRepository.recordUserEvent(dummyUser.getId(), userEvent) >> new Try.Success<Void>(null)

        then:
        maybeResults.isSuccess()
    }

    def "recordEvent: recordUserEvent fails, i fail"(){
        setup:
        UserEvent userEvent = new UserEvent()

        when:
        Try<Void> maybeResults = userService.recordEvent(dummyUser.getId(), userEvent)

        then:
        1 * eventContainerRepository.recordUserEvent(dummyUser.getId(), userEvent) >> new Try.Failure<Void>(failureTypeException)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() == failureTypeException
    }

    def "searchUser: success"(){
        when:
        Try<List<UserSearchResult>> maybeUserSearchResults = userService.searchUsers(["curt", "hostetter"], 10)

        then:
        1 * userRepository.searchUsers(["curt", "hostetter"], 10) >> new Try.Success<List<UserSearchResult>>([]);

        then:
        maybeUserSearchResults.isSuccess()
    }

    def "searchUser: searchUsers failed, i fail"(){
        when:
        Try<List<UserSearchResult>> maybeUserSearchResults = userService.searchUsers(["curt", "hostetter"], 10)

        then:
        1 * userRepository.searchUsers(*_) >> new Try.Failure<List<UserSearchResult>>(new Exception());

        then:
        maybeUserSearchResults.isFailure()
    }

    private static Attribute buildAttribute(String name, List<String> values){

        Attribute attribute = new AttributeBuilder().buildObject()
        attribute.setName(name)

        values.each{ value ->
            XSString xsString = new AttributedStringImpl("namespaceURI", "elementLocalName", "namespacePrefix")
            xsString.setValue(value)

            attribute.getAttributeValues().add(xsString)
        }

        return attribute;
    }

    private static SAMLCredential buildSamlCredentials(String username, List<Attribute> attributes){
        NameID nameID = new NameIDBuilder().buildObject();
        nameID.setValue(username);
        return new SAMLCredential(
                nameID,
                new AssertionBuilder().buildObject(),
                null,
                null,
                attributes,
                null);
    }

    def "getUserByUsername: success"(){
        when:
        Try<User> maybeResults = userService.getUserByUsername(dummyUser.getUsername())

        then:
        1 * userRepository.getUserByUsername(dummyUser.getUsername()) >> new Try.Success<User>(dummyUser)

        then:
        maybeResults.isSuccess()
        maybeResults.get().getUsername() == dummyUser.getUsername()
    }

    def "getUserBySecondaryId: success"(){
        when:
        Try<User> maybeResults = userService.getUserBySecondaryId(dummyUser.getSecondaryId())

        then:
        1 * userRepository.getUserBySecondaryId(dummyUser.getSecondaryId()) >> new Try.Success<User>(dummyUser)

        then:
        maybeResults.isSuccess()
        maybeResults.get().getSecondaryId() == dummyUser.getSecondaryId()
    }

    def "resetPassword: success"(){
        setup:
        dummyUser.setResetPasswordCode("12345")

        when:
        Try<Void> maybeResults = userService.resetPassword(dummyUser.getId(), "abc123", "12345")

        then:
        1 * userRepository.getUser(dummyUser.getId()) >> new Try.Success<User>(dummyUser)
        1 * userRepository.saveUser(_) >> { args ->
            User user = args[0]
            assert user.password == User.encodePassword("abc123")
            assert user.resetPasswordCode == null
            return new Try.Success<User>(user)
        }

        then:
        maybeResults.isSuccess()
    }

    def "resetPassword: bad userId"(){
        setup:
        dummyUser.setResetPasswordCode("12345")

        when:
        Try<Void> maybeResults = userService.resetPassword(dummyUser.getId(), "abc123", "12345")

        then:
        1 * userRepository.getUser(dummyUser.getId()) >> new Try.Success<User>(null)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() instanceof RepoNotFoundException
    }

    def "resetPassword: wrong reset code"(){
        setup:
        dummyUser.setResetPasswordCode("12345")

        when:
        Try<Void> maybeResults = userService.resetPassword(dummyUser.getId(), "abc123", "asdf")

        then:
        1 * userRepository.getUser(dummyUser.getId()) >> new Try.Success<User>(dummyUser)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() instanceof RepoNotFoundException
    }

    def "resetPassword: saveUser fails, i fail"(){
        setup:
        dummyUser.setResetPasswordCode("12345")

        when:
        Try<Void> maybeResults = userService.resetPassword(dummyUser.getId(), "abc123", "12345")

        then:
        1 * userRepository.getUser(dummyUser.getId()) >> new Try.Success<User>(dummyUser)
        1 * userRepository.saveUser(_) >> new Try.Failure<User>(failureTypeException)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() == failureTypeException
    }

    def "resetPassword: getUser fails, i fail"(){
        setup:
        dummyUser.setResetPasswordCode("12345")

        when:
        Try<Void> maybeResults = userService.resetPassword(dummyUser.getId(), "abc123", "12345")

        then:
        1 * userRepository.getUser(dummyUser.getId()) >> new Try.Failure<User>(failureTypeException)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() == failureTypeException
    }

    def "updateUser: success"(){
        setup:
        dummyUser.setHasDataUsageConsent(true)
        UpdateUserRequest updateUserRequest = new UpdateUserRequest(id: dummyUser.getId(), hasDataUsageConsent: false)

        when:
        Try<User> maybeResults = userService.updateUser(updateUserRequest)

        then:
        1 * userRepository.getUser(dummyUser.getId()) >> new Try.Success<User>(dummyUser)
        1 * userRepository.saveUser(_) >> { args ->
            User user = args[0]
            assert user.id == dummyUser.id
            assert user.hasDataUsageConsent == dummyUser.hasDataUsageConsent

            dummyUser = user

            return new Try.Success<Void>(null)
        }

        then:
        maybeResults.isSuccess()
        maybeResults.get() == dummyUser
        maybeResults.get().hasDataUsageConsent == updateUserRequest.hasDataUsageConsent
    }

    def "updateUser: saveUser fails, i fail"(){
        when:
        Try<User> maybeResults = userService.updateUser(new UpdateUserRequest(id: dummyUser.getId(), hasDataUsageConsent: false))

        then:
        1 * userRepository.getUser(dummyUser.getId()) >> new Try.Success<User>(dummyUser)
        1 * userRepository.saveUser(_) >> new Try.Failure<Void>(new Exception())

        then:
        maybeResults.isFailure()
    }

    def "updateUser: getUser fails, i fail"(){
        when:
        Try<User> maybeResults = userService.updateUser(new UpdateUserRequest(id: dummyUser.getId(), hasDataUsageConsent: false))

        then:
        1 * userRepository.getUser(dummyUser.getId()) >> new Try.Failure<User>(new Exception())

        then:
        maybeResults.isFailure()
    }
}
