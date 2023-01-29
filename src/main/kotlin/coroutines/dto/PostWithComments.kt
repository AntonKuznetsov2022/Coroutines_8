package ru.netology.coroutines.dto

data class PostWithComments(
    val post: Post,
    val authorPost: Author,
    val comment: List<CommentsWithAuthor>,
)

data class CommentsWithAuthor(
    val comment: Comment,
    val authorComment: Author,
)
