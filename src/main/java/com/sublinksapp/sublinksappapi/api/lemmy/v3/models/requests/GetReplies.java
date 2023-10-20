package com.sublinksapp.sublinksappapi.api.lemmy.v3.models.requests;

import com.sublinksapp.sublinksappapi.api.lemmy.v3.enums.CommentSortType;
import lombok.Builder;

@Builder
public record GetReplies(
        CommentSortType sort,
        Integer page,
        Integer limit,
        Boolean unread_only
) {
}