package com.example.chronos.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    private boolean first;
    private boolean last;
    private boolean empty;

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
    public boolean isEmpty() {
        computeHelpers();
        return content == null || content.isEmpty();
    }

    public static class PageResponseBuilder<T> {
        public PageResponse<T> build() {
            PageResponse<T> response = new PageResponse<>(
                    content, page, size, totalElements, totalPages, first, last, empty
            );
            response.computeHelpers();
            response.empty = (content == null || content.isEmpty());
            return response;
        }

        public PageResponseBuilder<T> pageNumber(int number) {
            this.page = number;
            return this;
        }
    }
}
