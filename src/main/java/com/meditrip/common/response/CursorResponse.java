package com.meditrip.common.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CursorResponse<T> {
    private List<T> items;
    private String nextCursor;
    private boolean hasNext;

    public static <T> CursorResponse<T> of(List<T> items, String nextCursor, boolean hasNext) {
        return CursorResponse.<T>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
