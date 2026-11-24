package albertmnz07.familyapp.infrastructure.web;

import albertmnz07.familyapp.application.query.PageQuery;
import albertmnz07.familyapp.application.query.PagedResult;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PageResponses {

    private PageResponses() {}

    public static PageQuery queryOf(Pageable pageable) {
        return PageQuery.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    public static <T, R> Page<R> of(PagedResult<T> result, Function<T, R> mapper) {
        return new PageImpl<>(
                result.content().stream().map(mapper).toList(),
                PageRequest.of(result.page(), result.size()),
                result.totalElements());
    }
}
