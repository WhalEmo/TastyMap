package com.beem.TastyMap.userRelated.post.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequestDTO {
    @NotBlank(message = "{validation.comment.contents.notblank}")
    @Size(
            min = 1,
            max = 500,
            message = "{validation.comment.contents.size}"
    )
    private String contents;

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }
}