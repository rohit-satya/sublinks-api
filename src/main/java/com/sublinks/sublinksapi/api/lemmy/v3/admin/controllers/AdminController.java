package com.sublinks.sublinksapi.api.lemmy.v3.admin.controllers;

import com.sublinks.sublinksapi.api.lemmy.v3.admin.models.AddAdmin;
import com.sublinks.sublinksapi.api.lemmy.v3.admin.models.AddAdminResponse;
import com.sublinks.sublinksapi.api.lemmy.v3.admin.models.ApproveRegistrationApplication;
import com.sublinks.sublinksapi.api.lemmy.v3.admin.models.GetUnreadRegistrationApplicationCount;
import com.sublinks.sublinksapi.api.lemmy.v3.admin.models.GetUnreadRegistrationApplicationCountResponse;
import com.sublinks.sublinksapi.api.lemmy.v3.admin.models.ListRegistrationApplications;
import com.sublinks.sublinksapi.api.lemmy.v3.admin.models.ListRegistrationApplicationsResponse;
import com.sublinks.sublinksapi.api.lemmy.v3.admin.models.PurgeItemResponse;
import com.sublinks.sublinksapi.api.lemmy.v3.admin.models.RegistrationApplicationResponse;
import com.sublinks.sublinksapi.api.lemmy.v3.authentication.JwtPerson;
import com.sublinks.sublinksapi.api.lemmy.v3.comment.models.PurgeComment;
import com.sublinks.sublinksapi.api.lemmy.v3.common.controllers.AbstractLemmyApiController;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.PurgeCommunity;
import com.sublinks.sublinksapi.api.lemmy.v3.enums.ModlogActionType;
import com.sublinks.sublinksapi.api.lemmy.v3.modlog.services.ModerationLogService;
import com.sublinks.sublinksapi.api.lemmy.v3.post.models.PurgePost;
import com.sublinks.sublinksapi.api.lemmy.v3.user.models.PurgePerson;
import com.sublinks.sublinksapi.api.lemmy.v3.user.services.LemmyPersonRegistrationApplicationService;
import com.sublinks.sublinksapi.api.lemmy.v3.user.services.LemmyPersonService;
import com.sublinks.sublinksapi.authorization.enums.RolePermission;
import com.sublinks.sublinksapi.authorization.services.RoleAuthorizingService;
import com.sublinks.sublinksapi.comment.config.CommentHistoryConfig;
import com.sublinks.sublinksapi.comment.entities.Comment;
import com.sublinks.sublinksapi.comment.repositories.CommentRepository;
import com.sublinks.sublinksapi.comment.services.CommentHistoryService;
import com.sublinks.sublinksapi.comment.services.CommentService;
import com.sublinks.sublinksapi.instance.models.LocalInstanceContext;
import com.sublinks.sublinksapi.moderation.entities.ModerationLog;
import com.sublinks.sublinksapi.person.entities.Person;
import com.sublinks.sublinksapi.person.entities.PersonRegistrationApplication;
import com.sublinks.sublinksapi.person.enums.PersonRegistrationApplicationStatus;
import com.sublinks.sublinksapi.person.repositories.PersonRegistrationApplicationRepository;
import com.sublinks.sublinksapi.person.repositories.PersonRepository;
import com.sublinks.sublinksapi.person.services.PersonRegistrationApplicationService;
import com.sublinks.sublinksapi.post.entities.Post;
import com.sublinks.sublinksapi.post.repositories.PostRepository;
import com.sublinks.sublinksapi.post.services.PostHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/v3/admin")
@Tag(name = "Admin")
public class AdminController extends AbstractLemmyApiController {

  private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
  private final LocalInstanceContext localInstanceContext;
  private final PersonRepository personRepository;
  private final PersonRegistrationApplicationRepository personRegistrationApplicationRepository;
  private final PersonRegistrationApplicationService personRegistrationApplicationService;
  private final LemmyPersonRegistrationApplicationService lemmyPersonRegistrationApplicationService;
  private final ModerationLogService moderationLogService;
  private final RoleAuthorizingService roleAuthorizingService;
  private final PostHistoryService postHistoryService;
  private final CommentHistoryService commentHistoryService;
  private final LemmyPersonService lemmyPersonService;
  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final CommentService commentService;
  private final CommentHistoryConfig commentHistoryConfig;

  @Operation(summary = "Add an admin to your site.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AddAdminResponse.class))})})
  @PostMapping("add")
  AddAdminResponse create(@Valid @RequestBody final AddAdmin addAdminForm, JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person, RolePermission.INSTANCE_ADD_ADMIN,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not_an_admin"));

    final Person personToAdd = personRepository.findById((long) addAdminForm.person_id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "person_not_found"));

    if (RoleAuthorizingService.isAdmin(personToAdd)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already_admin");
    }

    // Add Admin
    personToAdd.setRole(roleAuthorizingService.getAdminRole());
    personRepository.save(personToAdd);

    // Create Moderation Log
    ModerationLog moderationLog = ModerationLog.builder()
        .actionType(ModlogActionType.ModAdd)
        .entityId(personToAdd.getId())
        .removed(!addAdminForm.added())
        .instance(localInstanceContext.instance())
        .moderationPersonId(person.getId())
        .otherPersonId(personToAdd.getId())
        .build();
    moderationLogService.createModerationLog(moderationLog);

    return AddAdminResponse.builder()
        .admins(roleAuthorizingService.getAdmins()
            .stream()
            .map(lemmyPersonService::getPersonView)
            .toList())
        .build();
  }

  @Operation(summary = "Get the unread registration applications count.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GetUnreadRegistrationApplicationCountResponse.class))})})
  @GetMapping("registration_application/count")
  GetUnreadRegistrationApplicationCountResponse registrationApplicationCount(
      @Valid GetUnreadRegistrationApplicationCount getUnreadRegistrationApplicationCountForm,
      JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person, RolePermission.INSTANCE_REMOVE_ADMIN,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not_an_admin"));

    return GetUnreadRegistrationApplicationCountResponse.builder()
        .registration_applications(
            (int) personRegistrationApplicationRepository.countByApplicationStatus(
                PersonRegistrationApplicationStatus.pending))
        .build();
  }

  @Operation(summary = "List the registration applications.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ListRegistrationApplicationsResponse.class))})})
  @GetMapping("registration_application/list")
  ListRegistrationApplicationsResponse registrationApplicationList(
      @Valid final ListRegistrationApplications listRegistrationApplicationsForm,
      JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person,
        RolePermission.REGISTRATION_APPLICATION_READ,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not_an_admin"));

    final List<PersonRegistrationApplication> personRegistrationApplications = personRegistrationApplicationRepository.findAllByApplicationStatus(
        PersonRegistrationApplicationStatus.pending);

    return ListRegistrationApplicationsResponse.builder()
        .registration_applications(personRegistrationApplications.stream()
            .map(lemmyPersonRegistrationApplicationService::getPersonRegistrationApplicationView)
            .toList())
        .build();
  }

  @Operation(summary = "Approve a registration application.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RegistrationApplicationResponse.class))})})
  @PutMapping("registration_application/approve")
  RegistrationApplicationResponse registrationApplicationApprove(
      @Valid final ApproveRegistrationApplication approveRegistrationApplicationForm,
      final JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person,
        RolePermission.REGISTRATION_APPLICATION_UPDATE,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not_an_admin"));

    final PersonRegistrationApplication personRegistrationApplication = personRegistrationApplicationRepository.findById(
            (long) approveRegistrationApplicationForm.id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "registration_application_not_found"));

    personRegistrationApplication.setApplicationStatus(
        approveRegistrationApplicationForm.approved() ? PersonRegistrationApplicationStatus.approved
            : PersonRegistrationApplicationStatus.rejected);

    personRegistrationApplication.setAdmin(person);

    personRegistrationApplicationService.updatePersonRegistrationApplication(
        personRegistrationApplication);

    return RegistrationApplicationResponse.builder()
        .registration_application(
            lemmyPersonRegistrationApplicationService.getPersonRegistrationApplicationView(
                personRegistrationApplication))
        .build();
  }

  @Operation(summary = "Purge / Delete a person from the database.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PurgeItemResponse.class))})})
  @PostMapping("purge/person")
  PurgeItemResponse purgePerson(@Valid @RequestBody final PurgePerson purgePersonForm,
      final JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person, RolePermission.PURGE_USER,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not_an_admin"));

    final Person personToPurge = personRepository.findById((long) purgePersonForm.person_id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "person_not_found"));

    final int removedPostHistory = postHistoryService.deleteAllByCreator(personToPurge);
    final int removedCommentHistory = commentHistoryService.deleteAllByCreator(personToPurge);
    // @todo: Log purged history amount?
    // @todo: Implement purging

    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
  }

  @Operation(summary = "Purge / Delete a community from the database.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PurgeItemResponse.class))})})
  @PostMapping("purge/community")
  PurgeItemResponse purgeCommunity(@Valid @RequestBody final PurgeCommunity purgeCommunityForm,
      final JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person, RolePermission.PURGE_COMMUNITY,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not_an_admin"));

    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
  }

  @Operation(summary = "Purge / Delete a post from the database.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PurgeItemResponse.class))})})
  @PostMapping("purge/post")
  PurgeItemResponse purgePost(@Valid @RequestBody final PurgePost purgePostForm,
      final JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person, RolePermission.PURGE_POST,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not_an_admin"));

    final Post postToPurge = postRepository.getReferenceById((long) purgePostForm.post_id());
    try {
      final int removedPostHistory = postHistoryService.deleteAllByPost(postToPurge);
    } catch (Exception e) {
      e.printStackTrace();
    }
    // @todo: Log purged history amount?
    // @todo: Implement purging

    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
  }

  @Operation(summary = "Purge / Delete a comment from the database.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PurgeItemResponse.class))})})
  @PostMapping("purge/comment")
  PurgeItemResponse purgeComment(@Valid @RequestBody final PurgeComment purgeCommentForm,
      final JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person, RolePermission.PURGE_COMMENT,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not_an_admin"));

    final Comment commentToPurge = commentRepository.getReferenceById(
        (long) purgeCommentForm.comment_id());

    logger.info("Purging comment: {}", commentToPurge.getId());
    logger.info("Purge reason: {}", purgeCommentForm.reason());

    try {
      final int commentHistoryDeleted = commentHistoryService.deleteAllByComment(commentToPurge);
      logger.info("Successfully deleted {} comments from commentHistory", commentHistoryDeleted);

      final Comment deletedComment = commentService.deleteComment(commentToPurge);
      logger.info("Successfully deleted comment: {}", deletedComment.getId());
    } catch (Exception e) {
      logger.error("Error occurred while purging comments: {}", e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    return PurgeItemResponse.builder().build();
  }
}
