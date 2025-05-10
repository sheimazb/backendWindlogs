package com.windlogs.tickets.mapper;

import com.windlogs.tickets.dto.CommentRequestDTO;
import com.windlogs.tickets.dto.CommentResponseDTO;
import com.windlogs.tickets.entity.Comment;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "mentionedUsers", ignore = true)
    CommentResponseDTO commentToCommentResponseDTO(Comment comment);

    List<CommentResponseDTO> commentsToCommentResponseDTOs(List<Comment> comments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "mentionedUserIds", source = "mentionedUserIds")
    Comment commentRequestDTOToComment(CommentRequestDTO commentRequestDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "authorUserId", ignore = true)
    @Mapping(target = "mentionedUserIds", source = "mentionedUserIds")
    void updateCommentFromDTO(CommentRequestDTO commentRequestDTO, @MappingTarget Comment comment);
} 