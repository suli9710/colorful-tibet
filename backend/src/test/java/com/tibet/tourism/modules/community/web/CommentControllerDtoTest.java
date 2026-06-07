package com.tibet.tourism.modules.community.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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
    void spotCommentsReturnPublicUserWithoutLoginUsername() throws Exception {
        Comment comment = comment(publicUser());

        when(commentRepository.findBySpotIdOrderByCreatedAtDesc(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));

        mockMvc.perform(get("/api/comments/spot/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].userId").value(7))
                .andExpect(jsonPath("$.content[0].nickname").value("Public Nickname"))
                .andExpect(jsonPath("$.content[0].avatar").value("/avatars/u7.png"))
                .andExpect(jsonPath("$.content[0].user.id").value(7))
                .andExpect(jsonPath("$.content[0].user.nickname").value("Public Nickname"))
                .andExpect(jsonPath("$.content[0].user.avatar").value("/avatars/u7.png"))
                .andExpect(jsonPath("$.content[0].username").doesNotExist())
                .andExpect(jsonPath("$.content[0].user.username").doesNotExist())
                .andExpect(jsonPath("$.content[0].user.phone").doesNotExist())
                .andExpect(jsonPath("$.content[0].user.ipAddress").doesNotExist())
                .andExpect(jsonPath("$.content[0].user.allowedLoginFingerprintHash").doesNotExist());
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
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.nickname").value("Public Nickname"))
                .andExpect(jsonPath("$.avatar").value("/avatars/u7.png"))
                .andExpect(jsonPath("$.user.id").value(7))
                .andExpect(jsonPath("$.user.nickname").value("Public Nickname"))
                .andExpect(jsonPath("$.user.avatar").value("/avatars/u7.png"))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.user.username").doesNotExist())
                .andExpect(jsonPath("$.user.phone").doesNotExist())
                .andExpect(jsonPath("$.user.ipAddress").doesNotExist())
                .andExpect(jsonPath("$.user.allowedLoginFingerprintHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.ipAddress").doesNotExist())
                .andExpect(jsonPath("$.allowedLoginFingerprintHash").doesNotExist());
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
