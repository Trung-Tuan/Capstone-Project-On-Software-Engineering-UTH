package com.uth.capstone.controller;

import com.uth.capstone.config.ImageUploadValidationInterceptor;
import com.uth.capstone.config.SecurityConfig;
import com.uth.capstone.dto.response.PageResponse;
import com.uth.capstone.dto.response.PostResponse;
import com.uth.capstone.enums.ItemType;
import com.uth.capstone.enums.PostStatus;
import com.uth.capstone.exception.AppException;
import com.uth.capstone.exception.ErrorCode;
import com.uth.capstone.exception.GlobalExceptionHandler;
import com.uth.capstone.repository.UserRepository;
import com.uth.capstone.security.*;
import com.uth.capstone.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostController.class)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        SchoolEmailValidator.class,
        JwtAuthenticationFilter.class,
        SchoolEmailEnforcementFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        CustomUserDetailsService.class,
        GlobalExceptionHandler.class,
        ImageUploadValidationInterceptor.class
})
class PostSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /posts và GET /api/posts công khai hỗ trợ tìm kiếm & lọc kết hợp không cần đăng nhập")
    void shouldAllowPublicSearchPostsWithCombinedFilters() throws Exception {
        PostResponse item = PostResponse.builder()
                .postId(100)
                .title("Mất laptop Dell")
                .itemType(ItemType.LOST)
                .status(PostStatus.APPROVED)
                .relevanceScore(2.35)
                .build();

        PageResponse<PostResponse> pageResponse = PageResponse.<PostResponse>builder()
                .items(List.of(item))
                .page(0)
                .size(12)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .sortBy("RELEVANCE")
                .queryTimeMs(15L)
                .build();

        when(postService.searchPosts(
                "laptop dell", "LOST", null, 2, 5, null,
                "2026-11-01", "2026-11-14", "RELEVANCE", 0, 12
        )).thenReturn(pageResponse);

        mockMvc.perform(get("/posts")
                        .param("keyword", "laptop dell")
                        .param("status", "LOST")
                        .param("categoryId", "2")
                        .param("locationId", "5")
                        .param("fromDate", "2026-11-01")
                        .param("toDate", "2026-11-14")
                        .param("sortBy", "RELEVANCE")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.sortBy").value("RELEVANCE"))
                .andExpect(jsonPath("$.result.items[0].postId").value(100))
                .andExpect(jsonPath("$.result.items[0].relevanceScore").value(2.35));
    }

    @Test
    @DisplayName("GET /posts trả về 400 Bad Request khi sai khoảng thời gian lọc")
    void shouldReturn400WhenDateRangeIsInvalid() throws Exception {
        when(postService.searchPosts(
                null, null, null, null, null, null,
                "2026-11-20", "2026-11-01", null, 0, 12
        )).thenThrow(new AppException(ErrorCode.INVALID_DATE_RANGE));

        mockMvc.perform(get("/api/posts")
                        .param("fromDate", "2026-11-20")
                        .param("toDate", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_DATE_RANGE.getCode()));
    }

    @Test
    @DisplayName("GET /posts trả về 400 Bad Request khi categoryId không phải số")
    void shouldReturn400WhenCategoryIdTypeMismatch() throws Exception {
        mockMvc.perform(get("/posts")
                        .param("categoryId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FILTER_PARAMETER.getCode()));
    }
}
