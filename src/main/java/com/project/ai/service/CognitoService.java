package com.project.ai.service;

import com.project.ai.config.CognitoProperties;
import com.project.ai.dto.user.AdminResetPasswordDto;
import com.project.ai.dto.user.ConfirmForceChangePasswordDto;
import com.project.ai.dto.user.ConfirmForgotPasswordRequest;
import com.project.ai.dto.user.CreateUserRequest;
import com.project.ai.dto.user.ForgotPasswordRequest;
import com.project.ai.dto.user.LoginRequest;
import com.project.ai.dto.user.LoginResponse;
import com.project.ai.dto.user.RefreshTokenRequest;
import com.project.ai.dto.user.ResetPasswordRequest;
import com.project.ai.dto.user.UpdateUserGroupRequest;
import com.project.ai.dto.user.UpdateUserRequest;
import com.project.ai.dto.user.UserDto;
import com.project.ai.dto.user.UserPageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChangePasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 8:57 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class CognitoService {


    private final CognitoIdentityProviderClient cognitoClient;
    private final CognitoProperties props;

    // ── Auth ──────────────────────────────────────────────────────────────────

    public LoginResponse login(LoginRequest request) {
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", request.getEmail());
            authParams.put("PASSWORD", request.getPassword());
            authParams.put("SECRET_HASH", computeSecretHash(request.getEmail()));

            InitiateAuthResponse response = cognitoClient.initiateAuth(
                    InitiateAuthRequest.builder()
                            .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                            .clientId(props.getClientId())
                            .authParameters(authParams)
                            .build()
            );

            // ── Handle NEW_PASSWORD_REQUIRED challenge ────────────────────────────
            if (response.challengeName() == ChallengeNameType.NEW_PASSWORD_REQUIRED) {
                log.info("[CognitoService] NEW_PASSWORD_REQUIRED challenge for user='{}'",
                        request.getEmail());
                return LoginResponse.builder()
                        .challengeName("NEW_PASSWORD_REQUIRED")
                        .session(response.session())
                        .challengeEmail(request.getEmail())
                        .build();
            }

            AuthenticationResultType result = response.authenticationResult();
            return LoginResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    .refreshToken(result.refreshToken())
                    .expiresIn(result.expiresIn())
                    .tokenType(result.tokenType())
                    .build();

        } catch (NotAuthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        } catch (Exception e) {
            log.error("[CognitoService] login error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public LoginResponse confirmForceChangePassword(ConfirmForceChangePasswordDto request) {
        try {
            Map<String, String> challengeResponses = new HashMap<>();
            challengeResponses.put("USERNAME",     request.getEmail());
            challengeResponses.put("NEW_PASSWORD", request.getNewPassword());
            challengeResponses.put("SECRET_HASH",  computeSecretHash(request.getEmail()));

            RespondToAuthChallengeResponse response = cognitoClient.respondToAuthChallenge(
                    RespondToAuthChallengeRequest.builder()
                            .clientId(props.getClientId())
                            .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                            .session(request.getSession())
                            .challengeResponses(challengeResponses)
                            .build()
            );

            AuthenticationResultType result = response.authenticationResult();
            return LoginResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    .refreshToken(result.refreshToken())
                    .expiresIn(result.expiresIn())
                    .tokenType(result.tokenType())
                    .build();

        } catch (Exception e) {
            log.error("[CognitoService] confirmForceChangePassword error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("REFRESH_TOKEN", request.getRefreshToken());
            authParams.put("SECRET_HASH", computeSecretHash(""));

            InitiateAuthResponse response = cognitoClient.initiateAuth(
                    InitiateAuthRequest.builder()
                            .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                            .clientId(props.getClientId())
                            .authParameters(authParams)
                            .build()
            );

            AuthenticationResultType result = response.authenticationResult();
            return LoginResponse.builder()
                    .accessToken(result.accessToken())
                    .idToken(result.idToken())
                    .expiresIn(result.expiresIn())
                    .tokenType(result.tokenType())
                    .build();

        } catch (Exception e) {
            log.error("[CognitoService] refresh error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        try {
            cognitoClient.forgotPassword(
                    software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest.builder()
                            .clientId(props.getClientId())
                            .secretHash(computeSecretHash(request.getEmail()))
                            .username(request.getEmail())
                            .build()
            );
        } catch (Exception e) {
            log.error("[CognitoService] forgotPassword error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public void confirmForgotPassword(ConfirmForgotPasswordRequest request) {
        try {
            cognitoClient.confirmForgotPassword(
                    software.amazon.awssdk.services.cognitoidentityprovider.model
                            .ConfirmForgotPasswordRequest.builder()
                            .clientId(props.getClientId())
                            .secretHash(computeSecretHash(request.getEmail()))
                            .username(request.getEmail())
                            .confirmationCode(request.getConfirmationCode())
                            .password(request.getNewPassword())
                            .build()
            );
        } catch (Exception e) {
            log.error("[CognitoService] confirmForgotPassword error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public void resetPassword(String accessToken, ResetPasswordRequest request) {
        try {
            cognitoClient.changePassword(
                    ChangePasswordRequest.builder()
                            .accessToken(accessToken)
                            .previousPassword(request.getOldPassword())
                            .proposedPassword(request.getNewPassword())
                            .build()
            );
        } catch (NotAuthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid current password");
        } catch (Exception e) {
            log.error("[CognitoService] resetPassword error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ── User Management ───────────────────────────────────────────────────────

    public UserDto createUser(CreateUserRequest request, Jwt jwt) {
        try {
            List<String> callerGroups = jwt.getClaimAsStringList("cognito:groups");
            if (callerGroups == null) callerGroups = List.of();

            boolean isMigforaAdmin = callerGroups.contains("MIGFORA_ADMIN");
            boolean isSuperAdmin   = callerGroups.contains("SUPER_ADMIN");
            boolean isBasic        = callerGroups.contains("BASIC");

            String requestedGroup = request.getGroup() != null
                    && !request.getGroup().isBlank()
                    ? request.getGroup() : "BASIC";

            // ── BASIC cannot create any user ──────────────────────────────────────
            if (isBasic && !isMigforaAdmin && !isSuperAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "BASIC users cannot create users");
            }

            // ── SUPER_ADMIN cannot create MIGFORA_ADMIN ───────────────────────────
            if (isSuperAdmin && !isMigforaAdmin
                    && "MIGFORA_ADMIN".equalsIgnoreCase(requestedGroup)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "SUPER_ADMIN cannot create users with MIGFORA_ADMIN role");
            }

            // ── Create user ───────────────────────────────────────────────────────
            List<AttributeType> attributes = List.of(
                    attr("email",                 request.getEmail()),
                    attr("name",                  request.getName()),
                    attr("family_name",           request.getFamilyName()),
                    attr("phone_number",          request.getPhoneNumber()),
                    attr("email_verified",        "true"),
                    attr("phone_number_verified", "true")
            );

            AdminCreateUserResponse response = cognitoClient.adminCreateUser(
                    AdminCreateUserRequest.builder()
                            .userPoolId(props.getUserPoolId())
                            .username(request.getEmail())
                            .temporaryPassword(request.getPassword())
                            .userAttributes(attributes)
                            .messageAction(MessageActionType.SUPPRESS)
                            .build()
            );

            // ── Set permanent password ────────────────────────────────────────────
            cognitoClient.adminSetUserPassword(
                    AdminSetUserPasswordRequest.builder()
                            .userPoolId(props.getUserPoolId())
                            .username(request.getEmail())
                            .password(request.getPassword())
                            .permanent(true)
                            .build()
            );

            // ── Assign group ──────────────────────────────────────────────────────
            cognitoClient.adminAddUserToGroup(
                    AdminAddUserToGroupRequest.builder()
                            .userPoolId(props.getUserPoolId())
                            .username(request.getEmail())
                            .groupName(requestedGroup)
                            .build()
            );

            log.info("[CognitoService] user created='{}' group='{}' by='{}'",
                    request.getEmail(), requestedGroup,
                    jwt.getClaimAsString("email"));

            return toUserDto(response.user(), List.of(requestedGroup));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (UsernameExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        } catch (Exception e) {
            log.error("[CognitoService] createUser error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public Map<String, Object> getUsers(String paginationToken, int limit) {
        try {
            ListUsersRequest.Builder requestBuilder = ListUsersRequest.builder()
                    .userPoolId(props.getUserPoolId())
                    .limit(limit);

            if (paginationToken != null && !paginationToken.isBlank()) {
                requestBuilder.paginationToken(paginationToken);
            }

            ListUsersResponse response = cognitoClient.listUsers(requestBuilder.build());

            List<UserPageDto> users = response.users().stream()
                    .map(u -> {
                        List<String> groups = getUserGroups(u.username()); // ← username not email
                        return toUserPageDto(u, groups);
                    })
                    .toList();

            Map<String, Object> result = new HashMap<>();
            result.put("users", users);
            result.put("nextToken", response.paginationToken());
            result.put("count", users.size());
            return result;

        } catch (Exception e) {
            log.error("[CognitoService] getUsers error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public UserDto getUserDetails(String username) {
        try {
            AdminGetUserResponse response = cognitoClient.adminGetUser(
                    AdminGetUserRequest.builder()
                            .userPoolId(props.getUserPoolId())
                            .username(username)
                            .build()
            );

            List<String> groups = getUserGroups(username);
            return toUserDtoFromAdmin(response, groups);

        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        } catch (Exception e) {
            log.error("[CognitoService] getUserDetails error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public UserDto updateUser(String username, UpdateUserRequest request) {
        try {
            List<AttributeType> attributes = new ArrayList<>();
            if (request.getName() != null)
                attributes.add(attr("name", request.getName()));
            if (request.getFamilyName() != null)
                attributes.add(attr("family_name", request.getFamilyName()));
            if (request.getPhoneNumber() != null) {
                attributes.add(attr("phone_number", request.getPhoneNumber()));
                attributes.add(attr("phone_number_verified", "true")); // ← add
            }

            cognitoClient.adminUpdateUserAttributes(
                    AdminUpdateUserAttributesRequest.builder()
                            .userPoolId(props.getUserPoolId())
                            .username(username)
                            .userAttributes(attributes)
                            .build()
            );

            return getUserDetails(username);

        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        } catch (Exception e) {
            log.error("[CognitoService] updateUser error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public void deleteUser(String username) {
        try {
            cognitoClient.adminDeleteUser(
                    AdminDeleteUserRequest.builder()
                            .userPoolId(props.getUserPoolId())
                            .username(username)
                            .build()
            );
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        } catch (Exception e) {
            log.error("[CognitoService] deleteUser error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public UserDto changeUserGroup(String username, UpdateUserGroupRequest request) {
        try {
            // SUPER_ADMIN only can assign MIGFORA_ADMIN
            if ("MIGFORA_ADMIN".equalsIgnoreCase(request.getGroup())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Cannot assign MIGFORA_ADMIN role — contact SUPER_ADMIN");
            }

            // validate group exists
            List<String> allowedGroups = List.of("BASIC", "SUPER_ADMIN");
            if (!allowedGroups.contains(request.getGroup())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid group. Allowed values: " + allowedGroups);
            }

            // get current groups
            List<String> currentGroups = getUserGroups(username);

            // remove from all current groups
            for (String group : currentGroups) {
                cognitoClient.adminRemoveUserFromGroup(
                        AdminRemoveUserFromGroupRequest.builder()
                                .userPoolId(props.getUserPoolId())
                                .username(username)
                                .groupName(group)
                                .build()
                );
            }

            // add to new group
            cognitoClient.adminAddUserToGroup(
                    AdminAddUserToGroupRequest.builder()
                            .userPoolId(props.getUserPoolId())
                            .username(username)
                            .groupName(request.getGroup())
                            .build()
            );

            log.info("[CognitoService] changed group for user='{}' from={} to='{}'",
                    username, currentGroups, request.getGroup());

            return getUserDetails(username);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        } catch (Exception e) {
            log.error("[CognitoService] changeUserGroup error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    public void adminResetPassword(String username, AdminResetPasswordDto request) {
        try {
            cognitoClient.adminSetUserPassword(
                    AdminSetUserPasswordRequest.builder()
                            .userPoolId(props.getUserPoolId())
                            .username(username)
                            .password(request.getNewPassword())
                            .permanent(true)
                            .build()
            );

            log.info("[CognitoService] admin reset password for user='{}'", username);

        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        } catch (Exception e) {
            log.error("[CognitoService] adminResetPassword error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public UserDto getCurrentUser(String accessToken) {
        try {
            GetUserResponse response = cognitoClient.getUser(
                    GetUserRequest.builder()
                            .accessToken(accessToken)
                            .build()
            );

            List<String> groups = getUserGroups(response.username());

            return UserDto.builder()
                    .sub(response.username())
                    .username(getAttr(response.userAttributes(), "email"))
                    .email(getAttr(response.userAttributes(), "email"))
                    .name(getAttr(response.userAttributes(), "name"))
                    .familyName(getAttr(response.userAttributes(), "family_name"))
                    .phoneNumber(getAttr(response.userAttributes(), "phone_number"))
                    .status("CONFIRMED")
                    .groups(groups)
                    .build();

        } catch (NotAuthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        } catch (Exception e) {
            log.error("[CognitoService] getCurrentUser error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<String> getUserGroups(String username) {
        try {
            return cognitoClient.adminListGroupsForUser(
                            AdminListGroupsForUserRequest.builder()
                                    .userPoolId(props.getUserPoolId())
                                    .username(username)
                                    .build()
                    ).groups().stream()
                    .map(GroupType::groupName)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private AttributeType attr(String name, String value) {
        return AttributeType.builder().name(name).value(value).build();
    }

    private String getAttr(List<AttributeType> attrs, String name) {
        return attrs.stream()
                .filter(a -> a.name().equals(name))
                .map(AttributeType::value)
                .findFirst()
                .orElse(null);
    }

    private String computeSecretHash(String username) {
        try {
            String message = username + props.getClientId();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                    props.getClientSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error computing secret hash", e);
        }
    }

    private UserDto toUserDto(UserType user, List<String> groups) {
        List<AttributeType> attrs = user.attributes();
        String email = getAttr(attrs, "email");
        return UserDto.builder()
                .sub(user.username())                     // ← UUID
                .username(email)                          // ← email
                .email(email)
                .name(getAttr(attrs, "name"))
                .familyName(getAttr(attrs, "family_name"))
                .phoneNumber(getAttr(attrs, "phone_number"))
                .status(user.userStatusAsString())
                .groups(groups)
                .createdAt(user.userCreateDate() != null
                        ? user.userCreateDate().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .build();
    }

    private UserDto toUserDtoFromAdmin(AdminGetUserResponse user, List<String> groups) {
        List<AttributeType> attrs = user.userAttributes();
        String email = getAttr(attrs, "email");
        return UserDto.builder()
                .sub(user.username())                     // ← UUID
                .username(email)                          // ← email
                .email(email)
                .name(getAttr(attrs, "name"))
                .familyName(getAttr(attrs, "family_name"))
                .phoneNumber(getAttr(attrs, "phone_number"))
                .status(user.userStatusAsString())
                .groups(groups)
                .createdAt(user.userCreateDate() != null
                        ? user.userCreateDate().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .build();
    }

    private UserPageDto toUserPageDto(UserType user, List<String> groups) {
        List<AttributeType> attrs = user.attributes();
        String email = getAttr(attrs, "email");
        return UserPageDto.builder()
                .username(email)
                .sub(user.username())
                .email(email)
                .name(getAttr(attrs, "name"))
                .familyName(getAttr(attrs, "family_name"))
                .phoneNumber(getAttr(attrs, "phone_number"))
                .status(user.userStatusAsString())
                .groups(groups)
                .createdAt(user.userCreateDate() != null
                        ? user.userCreateDate().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .build();
    }
}
