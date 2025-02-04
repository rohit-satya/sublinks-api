package com.sublinks.sublinksapi.api.lemmy.v3.community.controllers;

import com.sublinks.sublinksapi.api.lemmy.v3.authentication.JwtPerson;
import com.sublinks.sublinksapi.api.lemmy.v3.common.controllers.AbstractLemmyApiController;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.AddModToCommunity;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.AddModToCommunityResponse;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.BanFromCommunity;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.BanFromCommunityResponse;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.CommunityModeratorView;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.CommunityResponse;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.DeleteCommunity;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.GetCommunityResponse;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.HideCommunity;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.RemoveCommunity;
import com.sublinks.sublinksapi.api.lemmy.v3.community.models.TransferCommunity;
import com.sublinks.sublinksapi.api.lemmy.v3.community.services.LemmyCommunityService;
import com.sublinks.sublinksapi.api.lemmy.v3.enums.ModlogActionType;
import com.sublinks.sublinksapi.api.lemmy.v3.modlog.services.ModerationLogService;
import com.sublinks.sublinksapi.api.lemmy.v3.user.services.LemmyPersonService;
import com.sublinks.sublinksapi.authorization.enums.RolePermission;
import com.sublinks.sublinksapi.authorization.services.RoleAuthorizingService;
import com.sublinks.sublinksapi.comment.services.CommentReportService;
import com.sublinks.sublinksapi.comment.services.CommentService;
import com.sublinks.sublinksapi.community.entities.Community;
import com.sublinks.sublinksapi.community.repositories.CommunityRepository;
import com.sublinks.sublinksapi.community.services.CommunityService;
import com.sublinks.sublinksapi.moderation.entities.ModerationLog;
import com.sublinks.sublinksapi.person.entities.Person;
import com.sublinks.sublinksapi.person.enums.LinkPersonCommunityType;
import com.sublinks.sublinksapi.person.repositories.PersonRepository;
import com.sublinks.sublinksapi.person.services.LinkPersonCommunityService;
import com.sublinks.sublinksapi.post.services.PostReportService;
import com.sublinks.sublinksapi.post.services.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping(path = "/api/v3/community")
@Tag(name = "Community")
@RequiredArgsConstructor
public class CommunityModActionsController extends AbstractLemmyApiController {

  private final CommunityService communityService;
  private final CommunityRepository communityRepository;
  private final RoleAuthorizingService roleAuthorizingService;
  private final LemmyCommunityService lemmyCommunityService;
  private final LinkPersonCommunityService linkPersonCommunityService;
  private final PersonRepository personRepository;
  private final LemmyPersonService lemmyPersonService;
  private final ConversionService conversionService;
  private final CommentService commentService;
  private final PostService postService;
  private final ModerationLogService moderationLogService;
  private final PostReportService postReportService;
  private final CommentReportService commentReportService;

  @Operation(summary = "Hide a community from public / \"All\" view. Admins only.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommunityResponse.class))})})
  @PutMapping("hide")
  CommunityResponse hide(@Valid @RequestBody final HideCommunity hideCommunityForm,
      JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person,
        RolePermission.ADMIN_REMOVE_COMMUNITY,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));
    final Community community = communityRepository.findById(hideCommunityForm.community_id())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "community_not_found"));

    community.setLocal(hideCommunityForm.hidden());
    communityRepository.save(community);

    // Create Moderation Log
    ModerationLog moderationLog = ModerationLog.builder()
        .actionType(ModlogActionType.ModHideCommunity)
        .hidden(hideCommunityForm.hidden())
        .entityId(community.getId())
        .communityId(community.getId())
        .instance(community.getInstance())
        .adminPersonId(person.getId())
        .reason(hideCommunityForm.reason())
        .build();
    moderationLogService.createModerationLog(moderationLog);

    return CommunityResponse.builder()
        .community_view(lemmyCommunityService.communityViewFromCommunity(community))
        .build();
  }

  @Operation(summary = "Delete a community.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommunityResponse.class))})})
  @PostMapping("delete")
  CommunityResponse delete(@Valid final DeleteCommunity deleteCommunityForm, JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person, RolePermission.DELETE_COMMUNITY,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));

    RoleAuthorizingService.isAdminElseThrow(person,
        () -> new ResponseStatusException(HttpStatus.FORBIDDEN));

    final Community community = communityRepository.findById(
            (long) deleteCommunityForm.community_id())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "community_not_found"));

    community.setDeleted(deleteCommunityForm.deleted());
    communityRepository.save(community);

    // Create Moderation Log
    ModerationLog moderationLog = ModerationLog.builder()
        .actionType(ModlogActionType.ModRemoveCommunity)
        .removed(deleteCommunityForm.deleted())
        .entityId(community.getId())
        .communityId(community.getId())
        .instance(community.getInstance())
        .adminPersonId(person.getId())
        .build();
    moderationLogService.createModerationLog(moderationLog);

    return CommunityResponse.builder()
        .community_view(lemmyCommunityService.communityViewFromCommunity(community))
        .build();
  }

  @Operation(summary = "A moderator remove for a community.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommunityResponse.class))})})
  @PostMapping("remove")
  CommunityResponse remove(@Valid @RequestBody final RemoveCommunity removeCommunityForm,
      JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person,
        RolePermission.MODERATOR_REMOVE_COMMUNITY,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));

    final Community community = communityRepository.findById(
            (long) removeCommunityForm.community_id())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "community_not_found"));

    final boolean isAllowed =
        linkPersonCommunityService.hasLink(person, community, LinkPersonCommunityType.moderator)
            || linkPersonCommunityService.hasLink(person, community, LinkPersonCommunityType.owner);

    if (!isAllowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not_allowed");
    }

    community.setRemoved(removeCommunityForm.removed());
    communityRepository.save(community);

    // Create Moderation Log
    ModerationLog moderationLog = ModerationLog.builder()
        .actionType(ModlogActionType.ModRemoveCommunity)
        .removed(removeCommunityForm.removed())
        .entityId(community.getId())
        .communityId(community.getId())
        .instance(community.getInstance())
        .moderationPersonId(person.getId())
        .reason(removeCommunityForm.reason())
        .build();
    moderationLogService.createModerationLog(moderationLog);

    return CommunityResponse.builder()
        .community_view(lemmyCommunityService.communityViewFromCommunity(community))
        .build();
  }

  @Operation(summary = "Transfer your community to an existing moderator.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GetCommunityResponse.class))})})
  @PostMapping("transfer")
  GetCommunityResponse transfer(@Valid @RequestBody final TransferCommunity transferCommunityForm,
      JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person,
        RolePermission.MODERATOR_TRANSFER_COMMUNITY,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));

    final Community community = communityRepository.findById(
            (long) transferCommunityForm.community_id())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "community_not_found"));

    final boolean isAllowed =
        RoleAuthorizingService.isAdmin(person) || linkPersonCommunityService.hasLink(person,
            community, LinkPersonCommunityType.owner);

    if (!isAllowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not_allowed");
    }

    final Person newOwner = personRepository.findById((long) transferCommunityForm.person_id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "person_not_found"));

    if (!linkPersonCommunityService.hasLink(newOwner, community,
        LinkPersonCommunityType.moderator)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "person_not_moderator");
    }

    final Person oldOwner = linkPersonCommunityService.getPersonsFromCommunityAndListTypes(
            community, List.of(LinkPersonCommunityType.owner))
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner_not_found"));
    linkPersonCommunityService.addLink(oldOwner, community, LinkPersonCommunityType.moderator);
    linkPersonCommunityService.removeLink(oldOwner, community, LinkPersonCommunityType.owner);

    linkPersonCommunityService.addLink(newOwner, community, LinkPersonCommunityType.owner);
    linkPersonCommunityService.removeLink(newOwner, community, LinkPersonCommunityType.moderator);

    // Create Moderation Log
    ModerationLog moderationLog = ModerationLog.builder()
        .actionType(ModlogActionType.ModTransferCommunity)
        .entityId(community.getId())
        .communityId(community.getId())
        .instance(community.getInstance())
        .moderationPersonId(person.getId())
        .otherPersonId(newOwner.getId())
        .build();
    moderationLogService.createModerationLog(moderationLog);

    return GetCommunityResponse.builder()
        .community_view(lemmyCommunityService.communityViewFromCommunity(community))
        .build();
  }

  @Operation(summary = "Ban a user from a community.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BanFromCommunityResponse.class))})})
  @PostMapping("ban_user")
  BanFromCommunityResponse banUser(@Valid @RequestBody final BanFromCommunity banPersonForm,
      JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrPermissionOrThrow(person, RolePermission.MODERATOR_BAN_USER,
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not_allowed"));

    final Community community = communityRepository.findById((long) banPersonForm.community_id())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "community_not_found"));

    final boolean isAllowed =
        linkPersonCommunityService.hasLink(person, community, LinkPersonCommunityType.moderator)
            || linkPersonCommunityService.hasLink(person, community, LinkPersonCommunityType.owner);

    if (!isAllowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not_allowed");
    }

    final Person personToBan = personRepository.findById((long) banPersonForm.person_id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "person_not_found"));

    if (banPersonForm.ban()) {
      if (banPersonForm.remove_data()) {

        postReportService.resolveAllReportsByPersonAndCommunity(personToBan, community, person);
        commentReportService.resolveAllReportsByCommentCreatorAndCommunity(personToBan, community,
            person);

        commentService.removeAllCommentsFromCommunityAndUser(community, personToBan, true);
        postService.removeAllPostsFromCommunityAndUser(community, personToBan, true);
      }
      if (!linkPersonCommunityService.hasLink(personToBan, community,
          LinkPersonCommunityType.banned)) {

        linkPersonCommunityService.addLink(personToBan, community, LinkPersonCommunityType.banned);
      }
    } else {
      commentService.removeAllCommentsFromCommunityAndUser(community, personToBan, false);
      postService.removeAllPostsFromCommunityAndUser(community, personToBan, false);
      if (linkPersonCommunityService.hasLink(personToBan, community,
          LinkPersonCommunityType.banned)) {
        linkPersonCommunityService.removeLink(personToBan, community,
            LinkPersonCommunityType.banned);
      }
    }

    // Create Moderation Log
    ModerationLog moderationLog = ModerationLog.builder()
        .actionType(ModlogActionType.ModBanFromCommunity)
        .banned(banPersonForm.ban())
        .entityId(community.getId())
        .communityId(community.getId())
        .instance(community.getInstance())
        .moderationPersonId(person.getId())
        .otherPersonId(personToBan.getId())
        .reason(banPersonForm.reason())
        .expires(banPersonForm.expires() == null ? null : new Date(banPersonForm.expires() * 1000L))
        .build();
    moderationLogService.createModerationLog(moderationLog);

    return BanFromCommunityResponse.builder()
        .banned(banPersonForm.ban())
        .person_view(lemmyPersonService.getPersonView(personToBan))
        .build();
  }

  @Operation(summary = "Add a moderator to your community.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AddModToCommunityResponse.class))})})
  @PostMapping("mod")
  AddModToCommunityResponse addMod(@Valid @RequestBody AddModToCommunity addModToCommunityForm,
      JwtPerson principal) {

    final Person person = getPersonOrThrowUnauthorized(principal);

    roleAuthorizingService.hasAdminOrAnyPermissionOrThrow(person,
        Set.of(RolePermission.MODERATOR_ADD_MODERATOR,
            RolePermission.ADMIN_ADD_COMMUNITY_MODERATOR),
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"));

    final Community community = communityRepository.findById(
            (long) addModToCommunityForm.community_id())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "community_not_found"));

    final boolean isAllowed =
        linkPersonCommunityService.hasLink(person, community, LinkPersonCommunityType.moderator)
            || linkPersonCommunityService.hasLink(person, community, LinkPersonCommunityType.owner);

    if (!isAllowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not_allowed");
    }

    final Person personToAdd = personRepository.findById((long) addModToCommunityForm.person_id())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "person_not_found"));

    if (addModToCommunityForm.added()) {
      if (!linkPersonCommunityService.hasLink(personToAdd, community,
          LinkPersonCommunityType.moderator)) {
        linkPersonCommunityService.addLink(personToAdd, community,
            LinkPersonCommunityType.moderator);
      }
    } else {
      if (linkPersonCommunityService.hasLink(personToAdd, community,
          LinkPersonCommunityType.moderator)) {
        linkPersonCommunityService.removeLink(personToAdd, community,
            LinkPersonCommunityType.moderator);
      }
    }

    Collection<Person> moderators = linkPersonCommunityService.getPersonsFromCommunityAndListTypes(
        community, List.of(LinkPersonCommunityType.moderator));

    List<CommunityModeratorView> moderatorsView = moderators.stream()
        .map(moderator -> CommunityModeratorView.builder()
            .moderator(conversionService.convert(moderator,
                com.sublinks.sublinksapi.api.lemmy.v3.user.models.Person.class))
            .community(conversionService.convert(community,
                com.sublinks.sublinksapi.api.lemmy.v3.community.models.Community.class))
            .build())
        .toList();

    // Create Moderation Log
    ModerationLog moderationLog = ModerationLog.builder()
        .actionType(ModlogActionType.ModAddCommunity)
        .entityId(community.getId())
        .communityId(community.getId())
        .removed(!addModToCommunityForm.added())
        .instance(community.getInstance())
        .moderationPersonId(person.getId())
        .otherPersonId(personToAdd.getId())
        .build();
    moderationLogService.createModerationLog(moderationLog);

    return AddModToCommunityResponse.builder().moderators(moderatorsView).build();
  }
}
