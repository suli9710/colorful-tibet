package com.tibet.tourism.modules.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.common.security.AuthEntryPointJwt;
import com.tibet.tourism.common.security.CsrfCookieFilter;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.TokenRevocationService;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import com.tibet.tourism.common.security.UserDetailsServiceImpl;
import com.tibet.tourism.common.security.UserSessionVersionService;
import com.tibet.tourism.common.security.WebSecurityConfig;
import com.tibet.tourism.modules.auth.application.AdminMfaPolicy;
import com.tibet.tourism.modules.content.application.HeritageService;
import com.tibet.tourism.modules.content.domain.HeritageComment;
import com.tibet.tourism.modules.content.domain.HeritageEvent;
import com.tibet.tourism.modules.content.domain.HeritageInheritor;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import com.tibet.tourism.modules.content.infra.HeritageCommentRepository;
import com.tibet.tourism.modules.content.infra.HeritageItemRepository;
import com.tibet.tourism.modules.content.infra.HeritageLikeRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = HeritageController.class)
@Import({
        WebSecurityConfig.class,
        AuthEntryPointJwt.class,
        CsrfCookieFilter.class,
        TrustedProxyIpResolver.class
})
class HeritageControllerPublicReadAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HeritageService heritageService;

    @MockBean
    private HeritageItemRepository heritageItemRepository;

    @MockBean
    private HeritageLikeRepository heritageLikeRepository;

    @MockBean
    private HeritageCommentRepository heritageCommentRepository;

    @MockBean
    private JwtAuthSupport jwtAuthSupport;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AdminMfaPolicy adminMfaPolicy;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private CsrfTokenService csrfTokenService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserSessionVersionService userSessionVersionService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void heritageDetailAllowsAnonymousRead() throws Exception {
        when(heritageService.getItemByIdAndIncrementView(1L)).thenReturn(Optional.of(heritageItem()));

        mockMvc.perform(get("/api/heritage/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Thangka"));
    }

    @Test
    void heritageListReturnsStablePageEnvelope() throws Exception {
        when(heritageService.getAllItems(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(heritageItem()), PageRequest.of(2, 3), 7));

        ResultActions result = mockMvc.perform(get("/api/heritage?page=2&size=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Thangka"));

        expectStablePageEnvelope(result, 2, 3, 7, 3);
    }

    @Test
    void heritageSearchReturnsStablePageEnvelope() throws Exception {
        when(heritageService.searchItems(eq("paint"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(heritageItem()), PageRequest.of(0, 4), 1));

        ResultActions result = mockMvc.perform(get("/api/heritage?keyword=paint&size=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1));

        expectStablePageEnvelope(result, 0, 4, 1, 1);
    }

    @Test
    void heritageCommentsAllowAnonymousRead() throws Exception {
        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.empty());
        when(heritageCommentRepository.findByHeritageItemIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment()), PageRequest.of(1, 2), 5));

        ResultActions result = mockMvc.perform(get("/api/heritage/1/comments?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].content").value("Beautiful craft"))
                .andExpect(jsonPath("$.content[0].owner").value(false))
                .andExpect(jsonPath("$.content[0].nickname").doesNotExist())
                .andExpect(jsonPath("$.content[0].avatar").doesNotExist())
                .andExpect(jsonPath("$.content[0].username").doesNotExist())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].user_id").doesNotExist())
                .andExpect(content().string(not(containsString("Public Visitor"))))
                .andExpect(content().string(not(containsString("\"userId\""))))
                .andExpect(content().string(not(containsString("\"user_id\""))));

        expectStablePageEnvelope(result, 1, 2, 5, 3);
    }

    @Test
    void heritageCommentsExposeOnlyOwnerFlagForAuthenticatedAuthor() throws Exception {
        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.of(publicUser()));
        when(heritageCommentRepository.findByHeritageItemIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment())));

        mockMvc.perform(get("/api/heritage/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].owner").value(true))
                .andExpect(jsonPath("$.content[0].nickname").value("Public Visitor"))
                .andExpect(jsonPath("$.content[0].avatar").value("/uploads/avatars/public.jpg"))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].user_id").doesNotExist())
                .andExpect(content().string(not(containsString("\"userId\""))))
                .andExpect(content().string(not(containsString("\"user_id\""))));
    }

    @Test
    void heritageCommentsDoNotExposeOwnerNicknameWhenItMatchesUsername() throws Exception {
        User owner = publicUser();
        owner.setNickname(" VISITOR ");
        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.of(owner));
        when(heritageCommentRepository.findByHeritageItemIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment(owner))));

        mockMvc.perform(get("/api/heritage/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].owner").value(true))
                .andExpect(jsonPath("$.content[0].nickname").doesNotExist())
                .andExpect(jsonPath("$.content[0].avatar").value("/uploads/avatars/public.jpg"))
                .andExpect(content().string(not(containsString("VISITOR"))))
                .andExpect(content().string(not(containsString("\"username\""))));
    }

    @Test
    void heritageCommentsDoNotExposeOtherUserIdentityToAuthenticatedReaders() throws Exception {
        User otherUser = new User();
        otherUser.setId(8L);
        otherUser.setUsername("other");
        otherUser.setNickname("Other Reader");

        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.of(otherUser));
        when(heritageCommentRepository.findByHeritageItemIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment())));

        mockMvc.perform(get("/api/heritage/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].owner").value(false))
                .andExpect(jsonPath("$.content[0].nickname").doesNotExist())
                .andExpect(jsonPath("$.content[0].avatar").doesNotExist())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(content().string(not(containsString("Public Visitor"))));
    }

    @Test
    void heritageInheritorsAllowAnonymousRead() throws Exception {
        when(heritageService.getInheritorsByItemId(eq(1L), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(inheritor()), invocation.getArgument(1), 51));

        ResultActions result = mockMvc.perform(get("/api/heritage/1/inheritors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(20))
                .andExpect(jsonPath("$.content[0].name").value("Tashi"));

        expectStablePageEnvelope(result, 0, 20, 51, 3);
        Pageable pageable = captureInheritorPageable();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        Sort.Order idSort = pageable.getSort().getOrderFor("id");
        assertThat(idSort).isNotNull();
        assertThat(idSort.isAscending()).isTrue();
    }

    @Test
    void heritageInheritorsClampPageSizeToFifty() throws Exception {
        when(heritageService.getInheritorsByItemId(eq(1L), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(inheritor()), invocation.getArgument(1), 101));

        ResultActions result = mockMvc.perform(get("/api/heritage/1/inheritors?size=500&page=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(20));

        expectStablePageEnvelope(result, 2, 50, 101, 3);
        Pageable pageable = captureInheritorPageable();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    void heritageEventsAllowAnonymousRead() throws Exception {
        when(heritageService.getEventsByItemId(eq(1L), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(event()), invocation.getArgument(1), 41));

        ResultActions result = mockMvc.perform(get("/api/heritage/1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(30))
                .andExpect(jsonPath("$.content[0].title").value("Workshop"));

        expectStablePageEnvelope(result, 0, 20, 41, 3);
        Pageable pageable = captureEventPageable();
        Sort.Order eventDateSort = pageable.getSort().getOrderFor("eventDate");
        Sort.Order idSort = pageable.getSort().getOrderFor("id");
        assertThat(eventDateSort).isNotNull();
        assertThat(eventDateSort.isAscending()).isTrue();
        assertThat(idSort).isNotNull();
        assertThat(idSort.isAscending()).isTrue();
    }

    @Test
    void heritageEventsClampPageSizeToFifty() throws Exception {
        when(heritageService.getEventsByItemId(eq(1L), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(event()), invocation.getArgument(1), 101));

        ResultActions result = mockMvc.perform(get("/api/heritage/1/events?size=999&page=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(30));

        expectStablePageEnvelope(result, 1, 50, 101, 3);
        Pageable pageable = captureEventPageable();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    void upcomingHeritageEventsReturnStablePageEnvelope() throws Exception {
        when(heritageService.getUpcomingEvents(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event()), PageRequest.of(0, 1), 2));

        ResultActions result = mockMvc.perform(get("/api/heritage/events/upcoming?size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(30))
                .andExpect(jsonPath("$.content[0].title").value("Workshop"));

        expectStablePageEnvelope(result, 0, 1, 2, 2);
    }

    @Test
    void heritageWriteOperationStillRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/heritage/1/like"))
                .andExpect(status().isUnauthorized());
    }

    private static void expectStablePageEnvelope(
            ResultActions result, int page, int size, long totalElements, int totalPages) throws Exception {
        result.andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(totalElements))
                .andExpect(jsonPath("$.totalPages").value(totalPages))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(jsonPath("$.numberOfElements").doesNotExist())
                .andExpect(jsonPath("$.first").doesNotExist())
                .andExpect(jsonPath("$.last").doesNotExist())
                .andExpect(jsonPath("$.empty").doesNotExist());
    }

    private Pageable captureInheritorPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(heritageService).getInheritorsByItemId(eq(1L), captor.capture());
        return captor.getValue();
    }

    private Pageable captureEventPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(heritageService).getEventsByItemId(eq(1L), captor.capture());
        return captor.getValue();
    }

    private static HeritageItem heritageItem() {
        HeritageItem item = new HeritageItem();
        item.setId(1L);
        item.setName("Thangka");
        item.setDescription("Painted scroll art");
        item.setCategory("craft");
        item.setRegion("Lhasa");
        item.setProtectionLevel("national");
        item.setViewCount(12);
        item.setLikeCount(3);
        item.setCommentCount(1);
        return item;
    }

    private static HeritageComment comment() {
        return comment(publicUser());
    }

    private static HeritageComment comment(User user) {
        HeritageComment comment = new HeritageComment();
        comment.setId(10L);
        comment.setContent("Beautiful craft");
        comment.setRating(5);
        comment.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        comment.setUser(user);
        comment.setHeritageItem(heritageItem());
        return comment;
    }

    private static HeritageInheritor inheritor() {
        HeritageInheritor inheritor = new HeritageInheritor();
        inheritor.setId(20L);
        inheritor.setName("Tashi");
        inheritor.setLevel("national");
        inheritor.setBio("Painter");
        inheritor.setRegion("Lhasa");
        inheritor.setHeritageItem(heritageItem());
        return inheritor;
    }

    private static HeritageEvent event() {
        HeritageEvent event = new HeritageEvent();
        event.setId(30L);
        event.setTitle("Workshop");
        event.setDescription("Public workshop");
        event.setEventDate(LocalDate.parse("2026-07-01"));
        event.setLocation("Lhasa");
        event.setHeritageItem(heritageItem());
        return event;
    }

    private static User publicUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("visitor");
        user.setNickname("Public Visitor");
        user.setAvatar("/uploads/avatars/public.jpg");
        return user;
    }
}
