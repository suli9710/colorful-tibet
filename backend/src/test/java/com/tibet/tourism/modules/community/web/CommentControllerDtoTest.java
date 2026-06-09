package com.tibet.tourism.modules.community.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.community.infra.CommentLikeRepository;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.upload.application.FileStorageService;
import com.tibet.tourism.modules.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerDtoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private ScenicSpotRepository spotRepository;

    @MockBean
    private CommentLikeRepository commentLikeRepository;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private JwtAuthSupport jwtAuthSupport;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private CsrfTokenService csrfTokenService;

    @MockBean
    private TrustedProxyIpResolver trustedProxyIpResolver;

    @Test
    void spotCommentsReturnPublicAuthorWithoutInternalUserId() throws Exception {
        Comment comment = comment(publicUser());

        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.empty());
        when(commentRepository.findBySpotIdOrderByCreatedAtDesc(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));

        mockMvc.perform(get("/api/comments/spot/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].owner").value(false))
                .andExpect(jsonPath("$.content[0].liked").value(false))
                .andExpect(jsonPath("$.content[0].nickname").value("Public Nickname"))
                .andExpect(jsonPath("$.content[0].avatar").value("/avatars/u7.png"))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].user_id").doesNotExist())
                .andExpect(jsonPath("$.content[0].user").doesNotExist())
                .andExpect(jsonPath("$.content[0].username").doesNotExist())
                .andExpect(jsonPath("$.content[0].phone").doesNotExist())
                .andExpect(jsonPath("$.content[0].ipAddress").doesNotExist())
                .andExpect(jsonPath("$.content[0].allowedLoginFingerprintHash").doesNotExist())
                .andExpect(content().string(not(containsString("\"userId\""))))
                .andExpect(content().string(not(containsString("\"user_id\""))));

        verify(commentLikeRepository, never()).findLikedCommentIds(any(), any());
    }

    @Test
    void spotCommentsExposeOnlyOwnerFlagForAuthenticatedAuthor() throws Exception {
        User user = publicUser();
        Comment comment = comment(user);

        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.of(user));
        when(commentRepository.findBySpotIdOrderByCreatedAtDesc(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));
        when(commentLikeRepository.findLikedCommentIds(7L, List.of(10L))).thenReturn(List.of(10L));

        mockMvc.perform(get("/api/comments/spot/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].owner").value(true))
                .andExpect(jsonPath("$.content[0].liked").value(true))
                .andExpect(jsonPath("$.content[0].nickname").value("Public Nickname"))
                .andExpect(jsonPath("$.content[0].avatar").value("/avatars/u7.png"))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].user_id").doesNotExist())
                .andExpect(jsonPath("$.content[0].user").doesNotExist())
                .andExpect(content().string(not(containsString("\"userId\""))))
                .andExpect(content().string(not(containsString("\"user_id\""))));

        verify(commentLikeRepository).findLikedCommentIds(7L, List.of(10L));
    }

    @Test
    void spotCommentsBatchLikedStateForAuthenticatedUser() throws Exception {
        User user = publicUser();
        Comment likedComment = comment(user);
        Comment unlikedComment = comment(user);
        unlikedComment.setId(11L);
        unlikedComment.setContent("Other view");

        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.of(user));
        when(commentRepository.findBySpotIdOrderByCreatedAtDesc(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(likedComment, unlikedComment)));
        when(commentLikeRepository.findLikedCommentIds(7L, List.of(10L, 11L))).thenReturn(List.of(10L));

        mockMvc.perform(get("/api/comments/spot/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].liked").value(true))
                .andExpect(jsonPath("$.content[1].liked").value(false));

        verify(commentLikeRepository).findLikedCommentIds(7L, List.of(10L, 11L));
    }

    @Test
    void spotCommentsReturnStablePageEnvelopeWithoutSpringDataInternals() throws Exception {
        User user = publicUser();
        Comment comment = comment(user);

        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.of(user));
        when(commentRepository.findBySpotIdOrderByCreatedAtDesc(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment), PageRequest.of(1, 2), 5));
        when(commentLikeRepository.findLikedCommentIds(7L, List.of(10L))).thenReturn(List.of());

        mockMvc.perform(get("/api/comments/spot/5?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].owner").value(true))
                .andExpect(jsonPath("$.content[0].liked").value(false))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(jsonPath("$.numberOfElements").doesNotExist())
                .andExpect(jsonPath("$.first").doesNotExist())
                .andExpect(jsonPath("$.last").doesNotExist())
                .andExpect(jsonPath("$.empty").doesNotExist())
                .andExpect(content().string(not(containsString("\"pageable\""))))
                .andExpect(content().string(not(containsString("\"sort\""))))
                .andExpect(content().string(not(containsString("\"number\""))))
                .andExpect(content().string(not(containsString("\"numberOfElements\""))))
                .andExpect(content().string(not(containsString("\"first\""))))
                .andExpect(content().string(not(containsString("\"last\""))))
                .andExpect(content().string(not(containsString("\"empty\""))));
    }

    @Test
    void addCommentReturnsDtoWithPublicUserOnly() throws Exception {
        User user = publicUser();

        ScenicSpot spot = new ScenicSpot();
        spot.setId(5L);

        when(jwtAuthSupport.resolveCurrentUser(any())).thenReturn(user);
        when(spotRepository.findById(5L)).thenReturn(Optional.of(spot));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(10L);
            comment.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
            return comment;
        });

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "spotId": 5,
                                  "content": "Nice view",
                                  "rating": 5,
                                  "imageUrl": "/uploads/comments/photo.jpg"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.owner").value(true))
                .andExpect(jsonPath("$.nickname").value("Public Nickname"))
                .andExpect(jsonPath("$.avatar").value("/avatars/u7.png"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.user_id").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.ipAddress").doesNotExist())
                .andExpect(jsonPath("$.allowedLoginFingerprintHash").doesNotExist())
                .andExpect(content().string(not(containsString("\"userId\""))))
                .andExpect(content().string(not(containsString("\"user_id\""))));
    }

    @Test
    void commentImageUploadDoesNotExposeStorageValidationDetails() throws Exception {
        when(fileStorageService.storeCommentImage(any()))
                .thenThrow(new IllegalArgumentException("Unsupported content type: application/x-msdownload"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.exe",
                "application/x-msdownload",
                "not-an-image".getBytes());

        mockMvc.perform(multipart("/api/comments/upload-image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Image upload failed"))
                .andExpect(content().string(not(containsString("application/x-msdownload"))))
                .andExpect(content().string(not(containsString("Unsupported content type"))));
    }

    @Test
    void commentImageUploadDoesNotExposeUnexpectedExceptionDetails() throws Exception {
        when(fileStorageService.storeCommentImage(any()))
                .thenThrow(new RuntimeException("s3://private-bucket/token=secret"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "not-an-image".getBytes());

        mockMvc.perform(multipart("/api/comments/upload-image").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Image upload failed"))
                .andExpect(content().string(not(containsString("private-bucket"))))
                .andExpect(content().string(not(containsString("token=secret"))));
    }

    private static User publicUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("login-name");
        user.setPassword("hashed-password");
        user.setNickname("Public Nickname");
        user.setAvatar("/avatars/u7.png");
        user.setPhone("13800138000");
        user.setIpAddress("203.0.113.99");
        user.setAllowedLoginFingerprintHash("fingerprint-hash");
        return user;
    }

    private static Comment comment(User user) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(5L);

        Comment comment = new Comment();
        comment.setId(10L);
        comment.setUser(user);
        comment.setSpot(spot);
        comment.setContent("Nice view");
        comment.setRating(5);
        comment.setImageUrl("/uploads/comments/photo.jpg");
        comment.setLikeCount(3);
        comment.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        return comment;
    }
}
