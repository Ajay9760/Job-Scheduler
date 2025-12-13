package com.example.chronos.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response wrapper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    // Helper properties
    private boolean first;
    private boolean last;
    private boolean empty;

    // Compute helper properties after building
    public void computeHelpers() {
        this.first = page == 0;
        this.last = page >= totalPages - 1;
        this.empty = content == null || content.isEmpty();
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        PageResponse<T> response = PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();

        response.computeHelpers();
        return response;
    }
}