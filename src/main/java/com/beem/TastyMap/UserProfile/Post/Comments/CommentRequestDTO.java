package com.beem.TastyMap.UserProfile.Post.Comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequestDTO {
    @NotBlank(message = "Yorum boş olamaz")
    @Size(
            min = 1,
            max = 500,
            message = "Yorum en fazla 500 karakter olabilir"
    )
    private String contents;

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }
}
