package com.sublinks.sublinksapi.person.entities;

import com.sublinks.sublinksapi.authorization.entities.Role;
import com.sublinks.sublinksapi.authorization.services.RoleAuthorizingService;
import com.sublinks.sublinksapi.comment.entities.Comment;
import com.sublinks.sublinksapi.comment.entities.CommentLike;
import com.sublinks.sublinksapi.instance.entities.Instance;
import com.sublinks.sublinksapi.language.entities.Language;
import com.sublinks.sublinksapi.person.enums.ListingType;
import com.sublinks.sublinksapi.person.enums.PostListingMode;
import com.sublinks.sublinksapi.person.enums.SortType;
import com.sublinks.sublinksapi.post.entities.PostLike;
import com.sublinks.sublinksapi.post.entities.PostRead;
import com.sublinks.sublinksapi.post.entities.PostSave;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "people")
public class Person implements UserDetails, Principal {

  /**
   * Relationships.
   */
  @OneToMany(mappedBy = "person", fetch = FetchType.LAZY)
  @Fetch(FetchMode.SUBSELECT)
  Set<LinkPersonCommunity> linkPersonCommunity;

  @OneToMany(mappedBy = "person", fetch = FetchType.LAZY)
  @Fetch(FetchMode.SUBSELECT)
  Set<LinkPersonPost> linkPersonPost;

  @ManyToOne
  @JoinTable(name = "link_person_instances", joinColumns = @JoinColumn(name = "person_id"), inverseJoinColumns = @JoinColumn(name = "instance_id"))
  private Instance instance;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @OneToOne(mappedBy = "person", cascade = CascadeType.ALL)
  private LinkPersonInstance linkPersonInstance;

  @OneToMany(mappedBy = "person")
  @Fetch(FetchMode.SUBSELECT)
  private List<UserData> userData;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "person")
  @Fetch(FetchMode.SUBSELECT)
  @PrimaryKeyJoinColumn
  private List<Comment> comments;

  @OneToMany(mappedBy = "person")
  @Fetch(FetchMode.SUBSELECT)
  @PrimaryKeyJoinColumn
  private List<CommentLike> commentLikes;

  @OneToMany(mappedBy = "person")
  @Fetch(FetchMode.SUBSELECT)
  @PrimaryKeyJoinColumn
  private List<CommentLike> commentReads;

  @OneToMany(mappedBy = "person")
  @Fetch(FetchMode.SUBSELECT)
  @PrimaryKeyJoinColumn
  private List<PostSave> postSaves;

  @OneToMany(mappedBy = "person")
  @Fetch(FetchMode.SUBSELECT)
  @PrimaryKeyJoinColumn
  private List<PostLike> postLikes;

  @OneToMany(mappedBy = "person")
  @Fetch(FetchMode.SUBSELECT)
  @PrimaryKeyJoinColumn
  private List<PostRead> postReads;

  @OneToOne(mappedBy = "person", cascade = CascadeType.ALL)
  @PrimaryKeyJoinColumn
  private PersonAggregate personAggregate;

  @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
  @Fetch(FetchMode.SUBSELECT)
  @JoinTable(name = "person_languages", joinColumns = @JoinColumn(name = "person_id"), inverseJoinColumns = @JoinColumn(name = "language_id"))
  private List<Language> languages;

  @OneToOne(mappedBy = "person", cascade = CascadeType.ALL)
  private PersonRegistrationApplication registrationApplication;

  /**
   * Attributes.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, name = "is_local")
  private boolean isLocal;

  @Column(nullable = false, name = "is_bot_account")
  private boolean isBotAccount;

  @Column(nullable = false, name = "is_deleted")
  private boolean isDeleted;

  @Column(nullable = false, name = "activity_pub_id")
  private String activityPubId;

  @Column(nullable = false, name = "actor_id")
  private String actorId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, name = "display_name")
  private String displayName;

  @Column(nullable = true)
  private String email;

  @Column(nullable = false, name = "is_email_verified")
  private boolean isEmailVerified;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String avatarImageUrl;

  @Column(nullable = false, name = "banner_image_url")
  private String bannerImageUrl;

  @Column(nullable = false)
  private String biography;

  @Column(nullable = false, name = "interface_language")
  private String interfaceLanguage;

  @Column(nullable = false, name = "default_theme")
  private String defaultTheme;

  @Column(nullable = false, name = "default_listing_type")
  @Enumerated(EnumType.STRING)

  private ListingType defaultListingType;

  @Column(nullable = false, name = "default_sort_type")
  @Enumerated(EnumType.STRING)

  private SortType defaultSortType;

  @Column(nullable = false, name = "post_listing_type")
  @Enumerated(EnumType.STRING)

  private PostListingMode postListingType;

  @Column(nullable = false, name = "is_infinite_scroll")
  private boolean isInfiniteScroll;

  @Column(nullable = false, name = "is_keyboard_navigation")
  private boolean isKeyboardNavigation;

  @Column(nullable = false, name = "is_animated_images")
  private boolean isAnimatedImages;

  @Column(nullable = false, name = "is_collapse_bot_comments")
  private boolean isCollapseBotComments;

  @Column(nullable = false, name = "is_auto_expanding")
  private boolean isAutoExpanding;

  @Column(nullable = false, name = "is_show_scores")
  private boolean isShowScores;

  @Column(nullable = false, name = "is_show_read_posts")
  private boolean isShowReadPosts;

  @Column(nullable = false, name = "is_show_nsfw")
  private boolean isShowNsfw;

  @Column(nullable = false, name = "is_blur_nsfw")
  private boolean isBlurNsfw;

  @Column(nullable = false, name = "is_show_bot_accounts")
  private boolean isShowBotAccounts;

  @Column(nullable = false, name = "is_show_avatars")
  private boolean isShowAvatars;

  @Column(nullable = false, name = "is_send_notifications_to_email")
  private boolean isSendNotificationsToEmail;

  @Column(nullable = false, name = "is_open_links_in_new_tab")
  private boolean isOpenLinksInNewTab;

  @Column(nullable = true, name = "matrix_user_id")
  private String matrixUserId;

  @Column(nullable = false, name = "public_key")
  private String publicKey;

  @Column(nullable = true, name = "private_key")
  private String privateKey;

  @Column(nullable = true, name = "totp_secret")
  private String totpSecret;

  @Column(nullable = true, name = "totp_verified_secret")
  private String totpVerifiedSecret;

  @CreationTimestamp
  @Column(updatable = false, nullable = false, name = "created_at")
  private Date createdAt;

  @UpdateTimestamp
  @Column(updatable = false, nullable = false, name = "updated_at")
  private Date updatedAt;

  public boolean isBanned() {

    if (getRole() == null) {
      throw new RuntimeException("Role is null!");
    }

    return RoleAuthorizingService.isBanned(getRole());
  }

  public boolean isAdmin() {

    if (getRole() == null) {
      throw new RuntimeException("Role is null!");
    }

    return RoleAuthorizingService.isAdmin(getRole());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {

    return null; // @todo
  }

  @Override
  public String getPassword() {

    return this.password;
  }

  @Override
  public String getUsername() {

    return getName();
  }

  @Override
  public boolean isAccountNonExpired() {

    return !isBanned();
  }

  @Override
  public boolean isAccountNonLocked() {

    return !isBanned();
  }

  @Override
  public boolean isCredentialsNonExpired() {

    return !isBanned();
  }

  @Override
  public boolean isEnabled() {

    return !isBanned();
  }

  @Override
  public final boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    Class<?> objectEffectiveClass =
        o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer()
            .getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass() : this.getClass();
    if (thisEffectiveClass != objectEffectiveClass) {
      return false;
    }
    Person person = (Person) o;
    return getId() != null && Objects.equals(getId(), person.getId());
  }

  @Override
  public final int hashCode() {

    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
        .getPersistentClass()
        .hashCode() : getClass().hashCode();
  }
}
