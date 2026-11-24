package albertmnz07.familyapp.application.query;

import java.util.List;
import java.util.Objects;

/** Framework-free slice of a server-ordered query result. */
public record PagedResult<T>(List<T> content, int page, int size, long totalElements) {

    public PagedResult {
        content = List.copyOf(Objects.requireNonNull(content, "content"));
    }

    public int totalPages() {
        return size <= 0 ? 1 : (int) Math.ceil((double) totalElements / (double) size);
    }

    public static <T> PagedResult<T> paginate(List<T> sorted, PageQuery pageQuery) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(pageQuery, "pageQuery");
        long offset = (long) pageQuery.page() * pageQuery.size();
        int start = (int) Math.min(offset, sorted.size());
        int end = Math.min(start + pageQuery.size(), sorted.size());
        return new PagedResult<>(
                sorted.subList(start, end), pageQuery.page(), pageQuery.size(), sorted.size());
    }
}
