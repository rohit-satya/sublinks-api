package com.sublinksapp.sublinksappapi.api.lemmy.v3.mappers.site;

import com.sublinksapp.sublinksappapi.api.lemmy.v3.announcment.Announcement;
import com.sublinksapp.sublinksappapi.api.lemmy.v3.mappers.LemmyPersonMapper;
import com.sublinksapp.sublinksappapi.api.lemmy.v3.mappers.LocalInstanceContextMapper;
import com.sublinksapp.sublinksappapi.api.lemmy.v3.mappers.TaglineMapper;
import com.sublinksapp.sublinksappapi.api.lemmy.v3.models.Language;
import com.sublinksapp.sublinksappapi.api.lemmy.v3.models.responses.GetSiteResponse;
import com.sublinksapp.sublinksappapi.api.lemmy.v3.models.views.CustomEmojiView;
import com.sublinksapp.sublinksappapi.instance.LocalInstanceContext;
import com.sublinksapp.sublinksappapi.person.Person;
import com.sublinksapp.sublinksappapi.person.PersonContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Collection;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {LocalInstanceContextMapper.class, TaglineMapper.class, LemmyPersonMapper.class})
public interface GetSiteResponseMapper {
    @Mapping(target = "version", constant = "0.1.0")
    @Mapping(target = "taglines", source = "announcements")
    @Mapping(target = "site_view", source = "context")
    @Mapping(target = "my_user", source = "personContext", conditionExpression = "java(personContext.getPerson() != null)")
    @Mapping(target = "discussion_languages", source = "discussionLanguages")
    @Mapping(target = "custom_emojis", source = "customEmojis")
    @Mapping(target = "all_languages", source = "languages")
    @Mapping(target = "admins", source = "admins")
    public GetSiteResponse map(
            LocalInstanceContext context,
            PersonContext personContext,
            Collection<Announcement> announcements,
            Collection<Person> admins,// todo collection of PersonContext
            Collection<Language> languages,
            Collection<CustomEmojiView> customEmojis,
            Collection<Integer> discussionLanguages
    );
}